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
package io.yupiik.fusion.examples.backend.service;

import io.yupiik.fusion.examples.backend.model.Order;
import io.yupiik.fusion.examples.backend.model.OrderStatus;
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.http.server.api.Request;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class OrderService {

    private final ConcurrentHashMap<String, Order> orderInventory = new ConcurrentHashMap<>();

    public Order findOrder(String id) {
        return orderInventory.get(id);
    }

    public List<Order> findOrders() {
        return orderInventory.values().stream().toList();
    }

    public Order createOrder(Request request, Order order) {
        String newId = UUID.randomUUID().toString();
        Order newOrder = new Order(
                newId,
                order.name(),
                order.description(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                order.products(),
                OrderStatus.created);
        orderInventory.putIfAbsent(newId, newOrder);
        return newOrder;
    }

    public Order updateOrder(String orderId, Order order) {
        Order updatedOrder = orderInventory.computeIfPresent(orderId, (id, orderData) ->
                new Order(
                        orderData.id(),
                        Optional.ofNullable(order.name()).orElse(orderData.name()),
                        Optional.ofNullable(order.description()).orElse(orderData.description()),
                        orderData.creationDate(),
                        LocalDateTime.now(),
                        Optional.ofNullable(order.products()).orElse(orderData.products()),
                        Optional.ofNullable(order.status()).orElse(orderData.status()))
                );
        orderInventory.replace(orderId, updatedOrder);
        return updatedOrder;
    }

    public Order deleteOrder(String orderId) {
        Order order = orderInventory.get(orderId);
        if (Objects.isNull(order)) throw new IllegalStateException("Not found");
        orderInventory.remove(orderId);
        return order;
    }
}
