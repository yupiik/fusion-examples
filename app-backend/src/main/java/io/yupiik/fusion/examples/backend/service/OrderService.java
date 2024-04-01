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

import java.util.List;
import java.util.logging.Logger;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

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

        // precompute runtime queries,
        // ofNullable() cause @ApplicationScoped will create a subclass with null for all params if no no-arg constructor is defined
        this.queries = ofNullable(database).map(Queries::new).orElse(null);
    }

    public Order findOrder(final String id) {
        final var db = txMgr.read(connection -> {
            final var orderEntity = database.findById(connection, OrderEntity.class, id);
            if (isNull(orderEntity)) {
                logger.severe(() -> "Order '" + id + "' not found");
                throw new IllegalArgumentException("Not found");
            }
            return new Pair<>(
                    orderEntity,
                    database.query(
                            connection, this.queries.findAllProductForOrder(),
                            b -> b.bind(id), r -> r.mapAll(s -> s.getString(1))));
        });
        return mapToOrder(db.first(), findProducts(db.second()));
    }

    public List<Order> findOrders() {
        final var db = txMgr.read(connection -> {
            final var orders = database.findAll(connection, OrderEntity.class);
            final var orderIds = orders.stream().map(OrderEntity::id).toList();
            final var products = orderIds.isEmpty() ?
                    List.<OrderProductEntity>of() :
                    database.query(
                            connection,
                            OrderProductEntity.class,
                            queries.findAllProductForOrdersBase() + orderIds.stream()
                                    .map(i -> "?")
                                    .collect(joining(", ", "(", ")")),
                            b -> orderIds.forEach(b::bind));
            return new Pair<>(orders, products);
        });

        final var productsPerOrder = db.second().stream()
                .collect(groupingBy(OrderProductEntity::orderId, mapping(OrderProductEntity::productId, toList())));
        return db.first().stream()
                .map(it -> mapToOrder(it, findProducts(productsPerOrder.getOrDefault(it.id(), List.of()))))
                .toList();
    }

    public Order createOrder(final Order order) {
        logger.fine("Create new order");
        final var createdOrder = txMgr.write(connection -> {
            final var temp = database.insert(connection, mapToOrderEntity(order));
            database.batchInsert(connection, OrderProductEntity.class, order.products().stream().map(Product::id).toList().stream()
                    .map(it -> new OrderProductEntity(temp.id(), it))
                    .iterator());
            return temp;
        });

        return mapToOrder(createdOrder, order.products());
    }

    public Order updateOrder(final Order order) {
        logger.fine("Update existing order");
        txMgr.write(connection -> database.update(connection, mapToOrderEntity(order))); // fails if not found
        return order;
    }

    public void deleteOrder(final String orderId) {
        logger.fine("Delete existing order");
        txMgr.write(connection -> {
            database.execute(connection, this.queries.deleteAllProductForOrder(), b -> b.bind(orderId));
            database.execute(connection, this.queries.deleteOrderEntityById(), b -> b.bind(orderId));
            return null;
        });
    }

    private Order mapToOrder(final OrderEntity orderEntity, final List<Product> products) {
        return new Order(
                orderEntity.id(),
                orderEntity.description(),
                orderEntity.name(),
                orderEntity.creationDate(),
                orderEntity.lastUpdateDate(),
                products,
                orderEntity.status());
    }

    private List<Product> findProducts(final List<String> products) {
        return products.stream().map(productService::findProduct).toList();
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

    private record Pair<A, B>(A first, B second) {
    }

    private record Queries(String findAllProductForOrder,
                           String findAllProductForOrdersBase,
                           String deleteOrderEntityById,
                           String deleteAllProductForOrder) {
        private Queries(final ContextLessDatabase database) {
            this(
                    "SELECT product_id as productId " +
                            "FROM " + database.entity(OrderProductEntity.class).getTable() + ' '
                            + "WHERE order_id = ?",
                    database.entity(OrderProductEntity.class).getFindAllQuery() + " WHERE order_id in ",
                    "DELETE FROM " + database.entity(OrderEntity.class).getTable() + " WHERE id = ?",
                    "DELETE FROM " + database.entity(OrderProductEntity.class).getTable() + " WHERE order_id = ?"
            );
        }
    }
}
