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
package io.yupiik.fusion.examples.backend.model.test;

import io.yupiik.fusion.framework.api.container.Types;
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.framework.build.api.json.JsonModel;
import io.yupiik.fusion.http.server.api.WebServer;
import io.yupiik.fusion.httpclient.core.ExtendedHttpClient;
import io.yupiik.fusion.httpclient.core.ExtendedHttpClientConfiguration;
import io.yupiik.fusion.httpclient.core.listener.impl.ExchangeLogger;
import io.yupiik.fusion.httpclient.core.response.StaticHttpResponse;
import io.yupiik.fusion.json.JsonMapper;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

import static java.net.http.HttpClient.newHttpClient;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.time.Clock.systemUTC;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// thin testing helper for demo purposes
@ApplicationScoped
public class TestClient implements AutoCloseable {
    private final HttpClient client;
    private final URI uri;
    private final JsonMapper json;

    protected TestClient() { // for subclass proxy
        this.client = null;
        this.uri = null;
        this.json = null;
    }

    public TestClient(final JsonMapper jsonMapper, final WebServer.Configuration configuration) {
        this.client = new ExtendedHttpClient(new ExtendedHttpClientConfiguration()
                .setDelegate(newHttpClient())
                .setRequestListeners(List.of(new ExchangeLogger(Logger.getLogger(getClass().getName()), systemUTC(), true))));
        this.uri = URI.create("http://localhost:" + configuration.port());
        this.json = jsonMapper;
    }

    public <T> HttpResponse<T> send(final Function<URI, HttpRequest> requestBuilder,
                                    final Class<T> result) {
        return send(requestBuilder, (Type) result);
    }

    @SuppressWarnings("unchecked")
    public <T> HttpResponse<T> send(final Function<URI, HttpRequest> requestBuilder,
                                    final Type result) {
        try {
            final var res = client.send(requestBuilder.apply(uri), ofString());
            if (result == String.class) {
                return (HttpResponse<T>) res;
            }
            return new StaticHttpResponse<>(
                    res.request(), res.uri(), res.version(),
                    res.statusCode(), res.headers(),
                    json.fromString(result, res.body()));
        } catch (final IOException e) {
            return fail(e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return fail(e);
        }
    }

    public <T> HttpResponse<T> send(final HttpRequest request, final HttpResponse.BodyHandler<T> responseBodyHandler) {
        try {
            return client.send(request, responseBodyHandler);
        } catch (final IOException e) {
            return fail(e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return fail(e);
        }
    }

    public EnrichedResponse jsonRpc(final String method, final Object payload) {
        return new EnrichedResponse(
                send(
                        HttpRequest.newBuilder()
                                .POST(HttpRequest.BodyPublishers.ofString(json.toString(Map.of(
                                        "jsonrpc", "2.0",
                                        "method", method,
                                        "params", payload))))
                                .uri(uri.resolve("/jsonrpc"))
                                .build(),
                        ofString()),
                json);
    }

    @Override
    public void close() {
        client.close();
    }

    public static class JsonRpc {
        private final JsonRpcResponse response;
        private final JsonMapper jsonMapper;

        public <A> JsonRpc(final JsonRpcResponse response, final JsonMapper jsonMapper) {
            this.response = response;
            this.jsonMapper = jsonMapper;
        }

        public <T> T as(final Class<T> type) {
            success();
            try {
                return jsonMapper.fromString(type, jsonMapper.toString(response.result()));
            } catch (final IllegalStateException ise) {
                return fail(response.toString(), ise);
            }
        }

        public <T> List<T> asList(final Class<T> type) {
            success();
            return jsonMapper.fromString(new Types.ParameterizedTypeImpl(List.class, type), jsonMapper.toString(response.result()));
        }

        public JsonRpc success() {
            assertTrue(response.error() == null, response::toString);
            return this;
        }
    }

    public static class EnrichedResponse implements HttpResponse<String> {
        private final HttpResponse<String> original;
        private final JsonMapper jsonMapper;

        private EnrichedResponse(final HttpResponse<String> original,
                                 final JsonMapper jsonMapper) {
            this.original = original;
            this.jsonMapper = jsonMapper;
        }

        public JsonRpc jsonRpc() {
            return new JsonRpc(jsonMapper.fromString(JsonRpcResponse.class, body()), jsonMapper);
        }

        @Override
        public String body() {
            return original.body();
        }

        @Override
        public int statusCode() {
            return original.statusCode();
        }

        @Override
        public HttpRequest request() {
            return original.request();
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return original.previousResponse();
        }

        @Override
        public HttpHeaders headers() {
            return original.headers();
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return original.sslSession();
        }

        @Override
        public URI uri() {
            return original.uri();
        }

        @Override
        public HttpClient.Version version() {
            return original.version();
        }
    }

    @JsonModel
    public record JsonRpcResponse(String jsonrpc, String id, Object result, Error error) {
        @JsonModel
        public record Error(Integer code, String message) {
        }
    }
}
