package io.yupiik.fusion.examples.backend.server;

import io.yupiik.fusion.framework.api.scope.DefaultScoped;
import io.yupiik.fusion.framework.build.api.event.OnEvent;
import io.yupiik.fusion.framework.build.api.order.Order;
import io.yupiik.fusion.http.server.api.WebServer;
import io.yupiik.fusion.http.server.impl.tomcat.TomcatWebServerConfiguration;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.filters.CorsFilter;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import static jakarta.servlet.DispatcherType.REQUEST;
import static org.apache.catalina.filters.CorsFilter.PARAM_CORS_ALLOWED_ORIGINS;

// just for swagger tests/demo
@DefaultScoped
public class Cors {
    protected void onStart(@OnEvent @Order(20_000) final WebServer.Configuration configuration) {
        final var tomcatConf = configuration.unwrap(TomcatWebServerConfiguration.class);
        tomcatConf.setContextCustomizers(
                Stream.concat(
                                tomcatConf.getContextCustomizers().stream(),
                                Stream.of(this::addCors))
                        .toList());
    }

    private void addCors(final StandardContext context) {
        context.addServletContainerInitializer((set, servletContext) -> {
            final var filter = servletContext.addFilter("cors", CorsFilter.class);
            filter.setInitParameter(PARAM_CORS_ALLOWED_ORIGINS, "*");
            filter.setAsyncSupported(true);
            filter.addMappingForUrlPatterns(EnumSet.of(REQUEST), true, "/jsonrpc");
        }, Set.of());
    }
}
