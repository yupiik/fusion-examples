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

import io.yupiik.fusion.examples.backend.model.test.CreateOrder;
import io.yupiik.fusion.examples.backend.model.test.DeleteOrders;
import io.yupiik.fusion.examples.backend.service.OrderService;
import io.yupiik.fusion.framework.api.container.Types;
import io.yupiik.fusion.testing.Fusion;
import io.yupiik.fusion.testing.MonoFusionSupport;
import io.yupiik.fusion.testing.http.TestClient;
import io.yupiik.fusion.testing.task.Task;
import io.yupiik.fusion.testing.task.TaskResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.yupiik.fusion.examples.backend.model.OrderStatus.pendingActive;
import static io.yupiik.fusion.testing.task.Task.Phase.AFTER;
import static io.yupiik.fusion.testing.task.Task.Phase.BEFORE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MonoFusionSupport
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RestMobileLineTests {
    @Test
    @Task(phase = AFTER, value = DeleteOrders.class)
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
    @Task(phase = BEFORE, value = CreateOrder.class)
    @Task(phase = AFTER, value = DeleteOrders.class)
    void getOrder(@Fusion final TestClient client, @TaskResult(CreateOrder.class) final String id) {
        final var get = client.send(
                uri -> HttpRequest.newBuilder()
                        .GET()
                        .uri(uri.resolve("/order/" + id))
                        .build(),
                Order.class);
        assertAll(
                () -> assertEquals(200, get.statusCode()),
                () -> assertEquals(id, get.body().id(), () -> get.body().toString()));
    }

    @Test
    @Task(phase = BEFORE, value = CreateOrder.class)
    @Task(phase = AFTER, value = DeleteOrders.class)
    void findOrders(@Fusion final TestClient client, @TaskResult(CreateOrder.class) final String id) {
        final HttpResponse<List<Order>> res = client.send(
                uri -> HttpRequest.newBuilder()
                        .GET()
                        .uri(uri.resolve("/order"))
                        .build(),
                new Types.ParameterizedTypeImpl(List.class, Order.class));
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertEquals(1, res.body().size(), res.statusCode()),
                () -> assertEquals(id, res.body().getFirst().id(), () -> res.body().toString()));
    }

    @Test
    @Task(phase = BEFORE, value = CreateOrder.class)
    @Task(phase = AFTER, value = DeleteOrders.class)
    void updateOrder(@Fusion final TestClient client, @TaskResult(CreateOrder.class) final String id) {
        final var res = client.send(
                uri -> HttpRequest.newBuilder()
                        .method("PUT",
                                HttpRequest.BodyPublishers.ofString("""
                                        {
                                             "description": "Mobile Line",
                                             "name": "Mobile Line",
                                             "status": "pendingActive",
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
                                        """, StandardCharsets.UTF_8)
                        )
                        .uri(uri.resolve("/order/" + id))
                        .build(),
                Order.class);
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertEquals(id, res.body().id(), () -> res.body().toString()),
                () -> assertEquals(pendingActive, res.body().status(), () -> res.body().toString()));
    }

    @Test
    @Task(phase = BEFORE, value = CreateOrder.class)
    @Task(phase = AFTER, value = DeleteOrders.class)
    void deleteOrder(@Fusion final TestClient client, @TaskResult(CreateOrder.class) final String id, @Fusion final OrderService service) {
        final var res = client.send(
                uri -> HttpRequest.newBuilder()
                        .DELETE()
                        .uri(uri.resolve("/order/" + id))
                        .build(),
                String.class);
        assertAll(
                () -> assertEquals(204, res.statusCode()));
    }
}
