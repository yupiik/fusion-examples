/*
 * Copyright (c) 2022-present - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.yupiik.fusion.examples.backend.observability;

import io.yupiik.fusion.examples.backend.configuration.BackendConfiguration;
import io.yupiik.fusion.framework.api.scope.DefaultScoped;
import io.yupiik.fusion.framework.build.api.event.OnEvent;
import io.yupiik.fusion.http.server.api.WebServer;
import io.yupiik.fusion.http.server.impl.tomcat.TomcatWebServerConfiguration;
import io.yupiik.fusion.json.JsonMapper;
import io.yupiik.fusion.observability.metrics.MetricsRegistry;
import io.yupiik.fusion.tracing.collector.AccumulatingSpanCollector;
import io.yupiik.fusion.tracing.id.IdGenerator;
import io.yupiik.fusion.tracing.server.ServerTracingConfiguration;
import io.yupiik.fusion.tracing.server.TracingValve;
import io.yupiik.fusion.tracing.zipkin.ZipkinFlusher;
import io.yupiik.fusion.tracing.zipkin.ZipkinFlusherConfiguration;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

import static io.yupiik.fusion.tracing.id.IdGenerator.Type.HEX;
import static jakarta.servlet.DispatcherType.REQUEST;
import static java.net.http.HttpClient.newHttpClient;
import static java.time.Clock.systemUTC;

/**
 * Binds using tomcat API a request counter (as a sample).
 */
@DefaultScoped
public class DemoMetricRegistrar {
    protected void onStart(@OnEvent final WebServer.Configuration configuration,
                           final MetricsRegistry registry,
                           final BackendConfiguration conf,
                           final JsonMapper json) {
        // get tomcat to just add a plain filter - generally done with a lib for ex
        final var tomcatConf = configuration.unwrap(TomcatWebServerConfiguration.class);
        // add a custom metric
        tomcatConf.setContextCustomizers(List.of(ctx -> customize(ctx, registry)));
        // enable tracing (zipkin/jaeger)
        tomcatConf.setTomcatCustomizers(List.of(t -> enableTracing(t, conf, json)));
    }

    private void enableTracing(final Tomcat tomcat, final BackendConfiguration conf, final JsonMapper json) {
        // just add the tracing valve to the tomcat host
        final var serverSpanConfiguration = new ServerTracingConfiguration()
                .setOperation("tomcat")
                .setServiceName("fusion-backend");
        final var spanCollector = new AccumulatingSpanCollector(new AccumulatingSpanCollector.Configuration()
                .setFlushInterval(5_000L))
                .setOnFlush(new ZipkinFlusher(json, newHttpClient(), new ZipkinFlusherConfiguration()
                        .setUrls(List.of(conf.zipkin()))));
        final var tracingValve = new TracingValve(serverSpanConfiguration, spanCollector, new IdGenerator(HEX), systemUTC(), true);
        tomcat.getHost().getPipeline().addValve(tracingValve);
    }

    private void customize(final Context app, final MetricsRegistry registry) {
        // add a servlet filter to count requests - for the demo
        final var counter = new LongAdder();
        app.addServletContainerInitializer((c, ctx) ->
                ctx.addFilter("request-counter", (request, response, chain) -> {
                            counter.increment();
                            chain.doFilter(request, response);
                        })
                        .addMappingForUrlPatterns(EnumSet.of(REQUEST), false, "/*"), Set.of());

        // register the counter as a gauge
        registry.registerReadOnlyGauge("backend_request_count", "none", counter::sum);
    }
}
