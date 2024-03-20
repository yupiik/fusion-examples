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
package io.yupiik.fusion.examples.backend.model;

import io.yupiik.fusion.framework.build.api.json.JsonModel;
import io.yupiik.fusion.http.server.api.WebServer;
import io.yupiik.fusion.json.JsonMapper;
import io.yupiik.fusion.testing.Fusion;
import io.yupiik.fusion.testing.FusionSupport;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FusionSupport
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JsonRpcMobileLineTests {

    private final HttpClient client = HttpClient.newHttpClient();
    private static final AtomicReference<String> id = new AtomicReference<>("xxx");

    @Test
    @org.junit.jupiter.api.Order(1)
    void createOrder(@Fusion final WebServer.Configuration configuration, @Fusion JsonMapper jsonMapper) throws IOException, InterruptedException {
        final var res = client.send(
                HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString("""
                                {
                                    "jsonrpc": "2.0",
                                    "id": "xxx1",
                                    "method": "fusion.examples.order.create",
                                    "params": {
                                        "order": {
                                                     "description": "Mobile Line",
                                                     "name": "Mobile Line",
                                                     "products": [
                                                        {
                                                            "id": "123456789",
                                                            "name": "Mobile Line",
                                                            "description": "Mobile Line with MSISDN"
                                                        },
                                                        {
                                                            "id": "987654321",
                                                            "name": "Device Phone",
                                                            "description": "Phone Device X Model Alpha GT"
                                                        }
                                                     ]
                                                 }
                                    }
                                }
                                """, StandardCharsets.UTF_8))
                        .uri(URI.create("http://localhost:" + configuration.port() + "/jsonrpc")).build(),
                ofString());
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertTrue(res.body().contains("\"status\":\"created\""), res::body),
                () -> assertTrue(res.body().contains("\"id\":\"xxx1\""), res::body)
        );
        final var response = jsonMapper.fromString(JsonRpcResponse.class, res.body());
        final var orderId = ((LinkedHashMap<?, ?>) response.result).get("id").toString();
        id.getAndSet(orderId);
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void getOrder(@Fusion final WebServer.Configuration configuration) throws IOException, InterruptedException {
        final var res = client.send(
                HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString("{\n" +
                                "    \"jsonrpc\": \"2.0\",\n" +
                                "    \"id\": \"xxx2\",\n" +
                                "    \"method\": \"fusion.examples.order.findById\",\n" +
                                "    \"params\": {\n" +
                                "        \"id\": \"" + id.get() + "\"\n" +
                                "    }\n" +
                                "}", StandardCharsets.UTF_8))
                        .uri(URI.create("http://localhost:" + configuration.port() + "/jsonrpc")).build(),
                ofString());
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertTrue(res.body().contains("\"id\":\"" + id.get() + "\""), res::body),
                () -> assertTrue(res.body().contains("\"id\":\"xxx2\""), res::body)
        );
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void findOrders(@Fusion final WebServer.Configuration configuration) throws IOException, InterruptedException {
        final var res = client.send(
                HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString("""
                                {
                                    "jsonrpc": "2.0",
                                    "id": "xxx3",
                                    "method": "fusion.examples.order.findAll"
                                }
                                """, StandardCharsets.UTF_8))
                        .uri(URI.create("http://localhost:" + configuration.port() + "/jsonrpc")).build(),
                ofString());
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertTrue(res.body().contains("\"status\":\"created\""), res::body),
                () -> assertTrue(res.body().contains("\"id\":\"xxx3\""), res::body)
        );
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void deleteOrder(@Fusion final WebServer.Configuration configuration) throws IOException, InterruptedException {
        final var res = client.send(
                HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString("{\n" +
                                "    \"jsonrpc\": \"2.0\",\n" +
                                "    \"id\": \"xxx4\",\n" +
                                "    \"method\": \"fusion.examples.order.delete\",\n" +
                                "    \"params\": {\n" +
                                "        \"id\": \"" + id.get() + "\"\n" +
                                "    }\n" +
                                "}", StandardCharsets.UTF_8))
                        .uri(URI.create("http://localhost:" + configuration.port() + "/jsonrpc")).build(),
                ofString());
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertTrue(res.body().contains("\"id\":\"" + id.get() + "\""), res::body),
                () -> assertTrue(res.body().contains("\"id\":\"xxx4\""), res::body)
        );
    }

    @JsonModel
    record JsonRpcResponse(String jsonrpc, String id, Object result, Error error){

        @JsonModel
        record Error(Integer code, String message){}
    }
}
