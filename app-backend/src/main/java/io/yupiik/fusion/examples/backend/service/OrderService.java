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
import io.yupiik.fusion.examples.backend.model.Product;
import io.yupiik.fusion.examples.backend.persistence.OrderDao;
import io.yupiik.fusion.examples.backend.persistence.OrderEntity;
import io.yupiik.fusion.examples.backend.persistence.OrderProductEntity;
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.util.Objects.isNull;

@ApplicationScoped
public class OrderService {
    private final Logger logger = Logger.getLogger(OrderService.class.getName());
    private final OrderDao orderDao;

    private final ProductService productService;

    public OrderService(final OrderDao orderDao, final ProductService productService) {
        this.orderDao = orderDao;
        this.productService = productService;
    }

    public Order findOrder(final String id) {
        final var orderEntity = orderDao.findOrder(id);
        if (isNull(orderEntity)) {
            logger.severe("Order not found");
            throw new IllegalArgumentException("Not found");
        }
        final var productOrderEntities = orderDao.getFindAllProductForOrderQuery(id);
        final var order = mapToOrder(orderEntity);
        order.products().addAll(productOrderEntities.stream().map(OrderProductEntity::productId).map(productService::findProduct).toList());
        return order;
    }

    public List<Order> findOrders() {
        final var orderEntities = orderDao.findAllOrder();
        if (Objects.isNull(orderEntities)) return List.of();

        return orderEntities.stream()
                .map(this::mapToOrder)
                .peek(order -> order.products().addAll(
                    orderDao.getFindAllProductForOrderQuery(
                        order.id()).stream().map(OrderProductEntity::productId).map(productService::findProduct).toList()))
                .toList();
    }

    public Order createOrder(final Order order) {
        logger.fine("Create new order");
        final var createdOrder = orderDao.createOrder(mapToOrderEntity(order),
                order.products().stream().map(Product::id).toList());
        if (isNull(createdOrder)) {
            logger.severe("Error on order creation");
            throw new IllegalArgumentException("Error on order creation");
        }
        final var newOrder = mapToOrder(createdOrder);
        newOrder.products().addAll(order.products());
        return newOrder;
    }

    public Order updateOrder(final String orderId, Order order) {
        logger.fine("Update existing order");
        final var orderEntity = orderDao.findOrder(orderId);
        if (isNull(orderEntity)) {
            logger.severe("Error on update order");
            throw new IllegalArgumentException("Not found");
        }
        return mapToOrder(orderDao.updateOrder(mapToOrderEntity(order, orderId)));
    }

    public Order deleteOrder(final String orderId) {
        logger.fine("Delete existing order");
        final var orderEntity = orderDao.findOrder(orderId);
        if (isNull(orderEntity)) {
            logger.severe("Order not found on delete");
            throw new IllegalArgumentException("Not found");
        }
        final var order = this.findOrder(orderId);
        orderDao.deleteOrder(orderEntity);
        return order;
    }

    private Order mapToOrder(final OrderEntity orderEntity) {
        return new Order(
                orderEntity.id(),
                orderEntity.description(),
                orderEntity.name(),
                orderEntity.creationDate(),
                orderEntity.lastUpdateDate(),
                new ArrayList<>(),
                orderEntity.status());
    }

    private OrderEntity mapToOrderEntity(final Order order) {
        return new OrderEntity(
                order.id(),
                order.description(),
                order.name(),
                order.creationDate(),
                order.lastUpdateDate(),
                order.status()
        );
    }

    private OrderEntity mapToOrderEntity(final Order order, final String id) {
        return new OrderEntity(
                id,
                order.description(),
                order.name(),
                order.creationDate(),
                order.lastUpdateDate(),
                order.status()
        );
    }
}
