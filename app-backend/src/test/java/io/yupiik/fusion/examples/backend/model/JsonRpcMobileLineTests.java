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
import io.yupiik.fusion.examples.backend.model.test.TestClient;
import io.yupiik.fusion.examples.backend.service.OrderService;
import io.yupiik.fusion.testing.Fusion;
import io.yupiik.fusion.testing.MonoFusionSupport;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.yupiik.fusion.examples.backend.model.OrderStatus.created;
import static io.yupiik.fusion.examples.backend.model.test.Data.Phase.AFTER;
import static io.yupiik.fusion.examples.backend.model.test.Data.Phase.BEFORE;
import static io.yupiik.fusion.examples.backend.model.test.Data.Type.INSERT_ORDER;
import static io.yupiik.fusion.examples.backend.model.test.Data.Type.RESTORE_STATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MonoFusionSupport
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JsonRpcMobileLineTests {
    @Test
    @Data(phase = AFTER, type = RESTORE_STATE)
    void createOrder(@Fusion final TestClient client) {
        final var res = client.jsonRpc(
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

        final var order = res.jsonRpc().success().as(Order.class);
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertEquals(created, order.status(), res::body),
                () -> assertNotNull(order.id(), res::body));
    }

    @Test
    @Data(phase = BEFORE, type = INSERT_ORDER)
    @Data(phase = AFTER, type = RESTORE_STATE)
    void getOrder(@Fusion final TestClient client, final OrderId id) {
        final var res = client.jsonRpc(
                "fusion.examples.order.findById",
                Map.of("id", id.value()));

        final var order = res.jsonRpc().success().as(Order.class);
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertInsertedOrder(order, id, res::body));
    }

    @Test
    @Data(phase = BEFORE, type = INSERT_ORDER)
    @Data(phase = AFTER, type = RESTORE_STATE)
    void findOrders(@Fusion final TestClient client, final OrderId id) {
        final var res = client.jsonRpc("fusion.examples.order.findAll", Map.of());

        final var orders = res.jsonRpc().success().asList(Order.class);
        assertAll(
                () -> assertEquals(200, res.statusCode()),
                () -> assertEquals(1, orders.size()),
                () -> assertInsertedOrder(orders.getFirst(), id, res::body));
    }

    @Test
    @Data(phase = BEFORE, type = INSERT_ORDER)
    @Data(phase = AFTER, type = RESTORE_STATE)
    void deleteOrder(@Fusion final TestClient client, final OrderId id, @Fusion final OrderService service) {
        final var res = client.jsonRpc(
                "fusion.examples.order.delete",
                Map.of("id", id.value()));

        final var order = res.jsonRpc().success().as(Order.class);
        assertAll(
                () -> assertInsertedOrder(order, id, res::body),
                () -> assertTrue(service.findOrders().isEmpty(), () -> service.findOrders().toString()));
    }

    private void assertInsertedOrder(final Order order, final OrderId id, final Supplier<String> debug) {
        assertAll(
                () -> assertEquals(id.value(), order.id(), debug),
                () -> assertEquals("description", order.name(), debug),
                () -> assertEquals("Mobile Line", order.description(), debug),
                () -> assertEquals(2, order.products().size(), debug));
    }
}
