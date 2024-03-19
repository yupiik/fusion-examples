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

import io.yupiik.fusion.http.server.api.WebServer;
import io.yupiik.fusion.testing.Fusion;
import io.yupiik.fusion.testing.FusionSupport;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FusionSupport
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RestProductInventory {

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void findProduct(@Fusion final WebServer.Configuration configuration) throws IOException, InterruptedException {
        final var res = client.send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("http://localhost:" + configuration.port() + "/product/123456789")).build(),
                ofString());
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertTrue(res.body().contains("\"id\":\"123456789\""), res::body)
        );
    }

    @Test
    void findProducts(@Fusion final WebServer.Configuration configuration) throws IOException, InterruptedException {
        final var res = client.send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("http://localhost:" + configuration.port() + "/product")).build(),
                ofString());
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertTrue(res.body().contains("\"id\":\"123456789\""), res::body)
        );
    }
}
