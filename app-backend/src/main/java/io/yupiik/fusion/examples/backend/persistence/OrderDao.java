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
package io.yupiik.fusion.examples.backend.persistence;

import io.yupiik.fusion.examples.backend.model.Order;
import io.yupiik.fusion.examples.backend.model.Product;
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.persistence.api.ContextLessDatabase;
import io.yupiik.fusion.persistence.api.TransactionManager;

import java.util.List;
import java.util.logging.Logger;

import static java.util.Optional.ofNullable;

@ApplicationScoped
public class OrderDao {

    private final Logger logger = Logger.getLogger(OrderDao.class.getName());
    private final ContextLessDatabase database;
    private final TransactionManager txMgr;
    private final Queries queries;

    public OrderDao(final ContextLessDatabase database, final TransactionManager txMgr) {
        this.database = database;
        this.txMgr = txMgr;

        // compute queries
        this.queries = ofNullable(database).map(Queries::new).orElse(null);
    }

    private record Queries(String findAllProductForOrderQuery, String deleteAllProductForOrderQuery) {
        private Queries(final ContextLessDatabase database) {
            this(
                    database.entity(OrderProductEntity.class).getFindAllQuery()
                            + " WHERE order_id = ?",
                    database.entity(OrderProductEntity.class).getDeleteQuery()
                            + " order_id = ?"
            );
        }
    }

    public OrderEntity findOrder(final String id) {
        return txMgr.read(connection -> database.findById(connection, OrderEntity.class, id));
    }

    public List<OrderEntity> findAllOrder() {
        return txMgr.read(connection -> database.findAll(connection, OrderEntity.class));
    }

    public OrderEntity createOrder(final OrderEntity orderEntity,
                             final List<String> products) {
        try {
            return txMgr.write(connection -> {
                final var createdOrder = database.insert(connection, orderEntity);
                products.forEach(product -> database.insert(connection, new OrderProductEntity(createdOrder.id(), product)));
                return createdOrder;
            });
        } catch (Error error) {
            // error, rollback is managed by datasource, no need to manage it by hand
            logger.severe("Error on insert order");
            throw new IllegalStateException("Error on order creation");
        }
    }

    public OrderEntity updateOrder(final OrderEntity entity) {
        try {
            return txMgr.write(connection -> database.update(connection, entity));
        } catch (Error error) {
            // error, rollback is managed by datasource, no need to manage it by hand
            logger.severe("Error on update for entity :: " + entity.id());
            throw new IllegalStateException("Error on order update");
        }
    }

    public void deleteOrder(final OrderEntity entity) {
        try {
            txMgr.write(connection -> database.execute(
                    connection, this.queries.deleteAllProductForOrderQuery(), b -> b.bind(entity.id())));
            txMgr.write(connection -> database.delete(connection, entity));
        } catch (Error error) {
            // error, rollback is managed by datasource, no need to manage it by hand
            logger.severe("Error on delete for entity :: " + entity.id());
        }
    }

    public List<OrderProductEntity> getFindAllProductForOrderQuery(final String orderId) {
        return txMgr.read(connection -> database.query(
                connection, OrderProductEntity.class, this.queries.findAllProductForOrderQuery(), b -> b.bind(orderId)));
    }
}
