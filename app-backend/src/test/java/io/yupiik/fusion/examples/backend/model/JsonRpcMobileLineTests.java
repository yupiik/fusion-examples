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
import io.yupiik.fusion.testing.Fusion;
import io.yupiik.fusion.testing.MonoFusionSupport;
import io.yupiik.fusion.testing.http.TestClient;
import io.yupiik.fusion.testing.task.Task;
import io.yupiik.fusion.testing.task.TaskResult;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.yupiik.fusion.examples.backend.model.OrderStatus.created;
import static io.yupiik.fusion.testing.task.Task.Phase.AFTER;
import static io.yupiik.fusion.testing.task.Task.Phase.BEFORE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MonoFusionSupport
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JsonRpcMobileLineTests {
    @Test
    @Task(phase = AFTER, value = DeleteOrders.class)
    void createOrder(@Fusion final TestClient client) {
        final var res = client.jsonRpcRequest(
                "fusion.examples.order.create",
                Map.of("order", Map.of(
                        "description", "Mobile Line",
                        "name", "Mobile Line",
                        "products", List.of(
                                Map.of(
                                        "id", "123456789",
                                        "name", "Mobile Line",
                                        "description", "Mobile Line with MSISDN"
                                ),
                                Map.of(
                                        "id", "987654321",
                                        "name", "Device Phone",
                                        "description", "Phone Device X Model Alpha GT"
                                )))));

        final var order = res.asJsonRpc().success().as(Order.class);
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertEquals(created, order.status(), res::body),
                () -> assertNotNull(order.id(), res::body));
    }

    @Test
    @Task(phase = BEFORE, value = CreateOrder.class)
    @Task(phase = AFTER, value = DeleteOrders.class)
    void getOrder(@Fusion final TestClient client, @TaskResult(CreateOrder.class) final String id) {
        final var res = client.jsonRpcRequest(
                "fusion.examples.order.findById",
                Map.of("id", id));

        final var order = res.asJsonRpc().success().as(Order.class);
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertInsertedOrder(order, id, res::body));
    }

    @Test
    @Task(phase = BEFORE, value = CreateOrder.class)
    @Task(phase = AFTER, value = DeleteOrders.class)
    void findOrders(@Fusion final TestClient client, @TaskResult(CreateOrder.class) final String id) {
        final var res = client.jsonRpcRequest("fusion.examples.order.findAll", Map.of());

        final var orders = res.asJsonRpc().success().asList(Order.class);
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertEquals(1, orders.size()),
                () -> assertInsertedOrder(orders.getFirst(), id, res::body));
    }

    @Test
    @Task(phase = BEFORE, value = CreateOrder.class)
    @Task(phase = AFTER, value = DeleteOrders.class)
    void deleteOrder(@Fusion final TestClient client, @TaskResult(CreateOrder.class) final String id, @Fusion final OrderService service) {
        final var res = client.jsonRpcRequest(
                "fusion.examples.order.delete",
                Map.of("id", id));

        final var deletedId = res.asJsonRpc().success().as(String.class);
        assertAll(
                () -> assertEquals(id, deletedId),
                () -> assertTrue(service.findOrders().isEmpty(), () -> service.findOrders().toString()));
    }

    private void assertInsertedOrder(final Order order, @TaskResult(CreateOrder.class) final String id, final Supplier<String> debug) {
        assertAll(
                () -> assertEquals(id, order.id(), debug),
                () -> assertEquals("Mobile Line", order.name(), debug),
                () -> assertEquals("Mobile Line", order.description(), debug),
                () -> assertEquals(2, order.products().size(), debug));
    }
}
