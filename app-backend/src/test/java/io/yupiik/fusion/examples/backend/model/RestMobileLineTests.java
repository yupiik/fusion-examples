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

import io.yupiik.fusion.examples.backend.model.test.Data;
import io.yupiik.fusion.examples.backend.model.test.OrderId;
import io.yupiik.fusion.examples.backend.service.OrderService;
import io.yupiik.fusion.framework.api.container.Types;
import io.yupiik.fusion.testing.Fusion;
import io.yupiik.fusion.testing.MonoFusionSupport;
import io.yupiik.fusion.testing.http.TestClient;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.yupiik.fusion.examples.backend.model.OrderStatus.pendingActive;
import static io.yupiik.fusion.examples.backend.model.test.Data.Phase.AFTER;
import static io.yupiik.fusion.examples.backend.model.test.Data.Phase.BEFORE;
import static io.yupiik.fusion.examples.backend.model.test.Data.Type.INSERT_ORDER;
import static io.yupiik.fusion.examples.backend.model.test.Data.Type.RESTORE_STATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MonoFusionSupport
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RestMobileLineTests {
    @Test
    @Data(phase = AFTER, type = RESTORE_STATE)
    void createOrder(@Fusion final TestClient client) {
        final var res = client.send(
                uri -> HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString("""
                                {
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
                                """, StandardCharsets.UTF_8))
                        .uri(uri.resolve("/order"))
                        .build(),
                String.class);
        assertAll(
                () -> assertEquals(201, res.statusCode()),
                () -> assertTrue(res.body().contains("\"status\":\"created\""), res::body));
    }

    @Test
    @Data(phase = BEFORE, type = INSERT_ORDER)
    @Data(phase = AFTER, type = RESTORE_STATE)
    void getOrder(@Fusion final TestClient client, final OrderId id) {
        final var get = client.send(
                uri -> HttpRequest.newBuilder()
                        .GET()
                        .uri(uri.resolve("/order/" + id.value()))
                        .build(),
                Order.class);
        assertAll(
                () -> assertEquals(200, get.statusCode()),
                () -> assertEquals(id.value(), get.body().id(), () -> get.body().toString()));
    }

    @Test
    @Data(phase = BEFORE, type = INSERT_ORDER)
    @Data(phase = AFTER, type = RESTORE_STATE)
    void findOrders(@Fusion final TestClient client, final OrderId id) {
        final HttpResponse<List<Order>> res = client.send(
                uri -> HttpRequest.newBuilder()
                        .GET()
                        .uri(uri.resolve("/order"))
                        .build(),
                new Types.ParameterizedTypeImpl(List.class, Order.class));
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertEquals(1, res.body().size(), res.statusCode()),
                () -> assertEquals(id.value(), res.body().getFirst().id(), () -> res.body().toString()));
    }

    @Test
    @Data(phase = BEFORE, type = INSERT_ORDER)
    @Data(phase = AFTER, type = RESTORE_STATE)
    void updateOrder(@Fusion final TestClient client, final OrderId id) {
        final var res = client.send(
                uri -> HttpRequest.newBuilder()
                        .method("PATCH",
                                HttpRequest.BodyPublishers.ofString("""
                                        {
                                             "status": "pendingActive"
                                         }
                                        """, StandardCharsets.UTF_8)
                        )
                        .uri(uri.resolve("/order/" + id.value()))
                        .build(),
                Order.class);
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertEquals(id.value(), res.body().id(), () -> res.body().toString()),
                () -> assertEquals(pendingActive, res.body().status(), () -> res.body().toString()));
    }

    @Test
    @Data(phase = BEFORE, type = INSERT_ORDER)
    @Data(phase = AFTER, type = RESTORE_STATE)
    void deleteOrder(@Fusion final TestClient client, final OrderId id, @Fusion final OrderService service) {
        final var res = client.send(
                uri -> HttpRequest.newBuilder()
                        .DELETE()
                        .uri(uri.resolve("/order/" + id.value()))
                        .build(),
                String.class);
        assertAll(
                () -> assertEquals(204, res.statusCode()),
                () -> assertNull(service.findOrder(id.value())));
    }
}
