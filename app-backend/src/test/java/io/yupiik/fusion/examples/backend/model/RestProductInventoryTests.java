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

import io.yupiik.fusion.framework.api.container.Types;
import io.yupiik.fusion.testing.Fusion;
import io.yupiik.fusion.testing.MonoFusionSupport;
import io.yupiik.fusion.testing.http.TestClient;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MonoFusionSupport
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RestProductInventoryTests {
    @Test
    void findProduct(@Fusion final TestClient client) {
        final var res = client.send(
                uri -> HttpRequest.newBuilder()
                        .GET()
                        .uri(uri.resolve("/product/123456789"))
                        .build(),
                Product.class);
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertEquals("123456789", res.body().id(), () -> res.body().toString()));
    }

    @Test
    void findProducts(@Fusion final TestClient client) {
        final HttpResponse<List<Product>> res = client.send(
                uri -> HttpRequest.newBuilder()
                        .GET()
                        .uri(uri.resolve("/product"))
                        .build(),
                new Types.ParameterizedTypeImpl(List.class, Product.class));
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertEquals(2, res.body().size()),
                () -> assertEquals("123456789", res.body().getFirst().id(), () -> res.body().toString()));
    }
}
