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
import io.yupiik.fusion.examples.backend.model.Product;
import io.yupiik.fusion.examples.backend.persistence.OrderEntity;
import io.yupiik.fusion.examples.backend.persistence.OrderProductEntity;
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.persistence.api.ContextLessDatabase;
import io.yupiik.fusion.persistence.api.TransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

@ApplicationScoped
public class OrderService {
    private final Logger logger = Logger.getLogger(OrderService.class.getName());
    private final ProductService productService;
    private final ContextLessDatabase database;
    private final TransactionManager txMgr;
    private final Queries queries;

    public OrderService(final ProductService productService, final ContextLessDatabase database, final TransactionManager txMgr) {
        this.database = database;
        this.txMgr = txMgr;
        this.productService = productService;
        // compute queries
        this.queries = ofNullable(database).map(Queries::new).orElse(null);
    }

    private record Queries(String findAllProductForOrderQuery, String deleteAllProductForOrderQuery) {
        private Queries(final ContextLessDatabase database) {
            this(
                    database.entity(OrderProductEntity.class).getFindAllQuery()
                            + " WHERE order_id = ?",
                    "delete from " + database.entity(OrderProductEntity.class).getTable() + " where order_id = ?"
            );
        }
    }

    public Order findOrder(final String id) {
        return txMgr.read(connection -> {
            final var orderEntity = database.findById(connection, OrderEntity.class, id);
            if (isNull(orderEntity)) {
                logger.severe("Order not found");
                throw new IllegalArgumentException("Not found");
            }
            final var productOrderEntities = database.query(connection, OrderProductEntity.class, this.queries.findAllProductForOrderQuery(), b -> b.bind(id));
            final var order = mapToOrder(orderEntity);
            order.products().addAll(productOrderEntities.stream().map(OrderProductEntity::productId).map(productService::findProduct).toList());
            return order;
        });
    }

    public List<Order> findOrders() {
//        final var orderEntities = orderDao.findAllOrder();
//
        return txMgr.read(connection -> {
            final var orders = database.findAll(connection, OrderEntity.class);
            return orders.stream()
                            .map(this::mapToOrder)
                            .peek(order -> order.products().addAll(
                                    database.query(connection, OrderProductEntity.class, this.queries.findAllProductForOrderQuery(), b -> b.bind(order.id()))
                                        .stream()
                                            .map(OrderProductEntity::productId)
                                            .map(productService::findProduct)
                                            .toList()))
                            .toList();
        });

//        return txMgr.read(connection -> {
//            final var orders = database.findAll(connection, OrderEntity.class);
//            final var orderIds = orders.stream().map(OrderEntity::id).distinct().toList();
//            final var productOrderIdProductId = orderDao.findProductsByOrderIds(conn, orderIds).stream()
//                    .collect(groupingBy(OrderProductEntity::orderId));
//            return orders.stream()
//                    .map(it -> mapToOrder(it, ofNullable(productOrderIdProductId.get(it.id())).orElse(List.of())))
//                    .toList();
//        });
    }

    public Order createOrder(final Order order) {
        logger.fine("Create new order");
        try {
            final var createdOrder =  txMgr.write(connection -> {
                final var temp = database.insert(connection, mapToOrderEntity(order));
                database.batchInsert(connection, OrderProductEntity.class, order.products().stream().map(Product::id).toList().stream()
                        .map(it -> new OrderProductEntity(temp.id(), it))
                        .iterator());
                return temp;
            });
            final var newOrder = mapToOrder(createdOrder);
            newOrder.products().addAll(order.products());
            return newOrder;

        } catch (Exception exception) {
            // error, rollback is managed by datasource, no need to manage it by hand
            logger.severe("Error on insert order");
            throw new IllegalStateException("Error on order creation");
        }

    }

    public Order updateOrder(final String orderId, Order order) {
        logger.fine("Update existing order");
        return txMgr.write(connection -> {
            final var existingOrderEntity = database.findById(connection, OrderEntity.class, orderId);
            if (isNull(existingOrderEntity)) {
                logger.severe("Error on update order");
                throw new IllegalArgumentException("Not found");
            }
            database.update(connection,mapToOrderEntity(order, orderId));
            return order;
        });
    }

    public void deleteOrder(final String orderId) {
        logger.fine("Delete existing order");
        txMgr.write(connection -> {
            final var existingOrderEntity = database.findById(connection, OrderEntity.class, orderId);
            if (isNull(existingOrderEntity)) {
                logger.severe("Error on delete order");
                throw new IllegalArgumentException("Not found");
            }
            database.execute(connection, this.queries.deleteAllProductForOrderQuery(), b -> b.bind(orderId));
            database.delete(connection, existingOrderEntity);
            return existingOrderEntity;
        });
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
