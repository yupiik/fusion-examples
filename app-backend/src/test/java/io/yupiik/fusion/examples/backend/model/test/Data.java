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
package io.yupiik.fusion.examples.backend.model.test;

import io.yupiik.fusion.examples.backend.model.Order;
import io.yupiik.fusion.examples.backend.service.OrderService;
import io.yupiik.fusion.examples.backend.service.ProductService;
import io.yupiik.fusion.framework.api.RuntimeContainer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.yupiik.fusion.examples.backend.model.OrderStatus.created;
import static io.yupiik.fusion.examples.backend.model.test.Data.Phase.AFTER;
import static io.yupiik.fusion.examples.backend.model.test.Data.Type.RESTORE_STATE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.time.LocalDate.EPOCH;

@Target(METHOD)
@Retention(RUNTIME)
@Repeatable(Data.List.class)
@ExtendWith(DataExtension.class)
public @interface Data {
    Phase phase() default AFTER;

    Type type() default RESTORE_STATE;

    enum Type implements BiConsumer<RuntimeContainer, ExtensionContext.Store> {
        INSERT_ORDER {
            @Override
            public void accept(final RuntimeContainer container, final ExtensionContext.Store store) {
                final var date = EPOCH.atStartOfDay();
                with(container, ProductService.class, products -> with(
                        container, OrderService.class,
                        service -> store.put(
                                OrderId.class,
                                new OrderId(service.createOrder(new Order(
                                                null /* generated */,
                                                "description", "Mobile Line",
                                                date, date, products.findProducts(), created))
                                        .id()))));
            }
        },

        /**
         * Products are immutable in this demo so we just drop all orders (starts empty).
         */
        RESTORE_STATE {
            @Override
            public void accept(final RuntimeContainer container, final ExtensionContext.Store ignored) {
                with(
                        container, OrderService.class,
                        service -> service.findOrders()
                                .stream()
                                .map(Order::id)
                                .forEach(service::deleteOrder));
            }
        };

        protected <T> void with(final RuntimeContainer container, final Class<T> type, final Consumer<T> consumer) {
            try (final var instance = container.lookup(type)) {
                consumer.accept(instance.instance());
            }
        }
    }

    enum Phase {
        BEFORE, AFTER
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @interface List {
        Data[] value();
    }
}
