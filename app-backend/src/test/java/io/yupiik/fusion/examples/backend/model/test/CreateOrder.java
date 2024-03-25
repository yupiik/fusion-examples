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
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.framework.api.scope.DefaultScoped;
import io.yupiik.fusion.testing.task.Task;

import static io.yupiik.fusion.examples.backend.model.OrderStatus.created;
import static java.time.LocalDate.EPOCH;

@ApplicationScoped
public class CreateOrder implements Task.Supplier<String> {
    private final ProductService products;
    private final OrderService service;

    public CreateOrder(final ProductService products, final OrderService service) {
        this.products = products;
        this.service = service;
    }

    @Override
    public String get() {
        final var date = EPOCH.atStartOfDay();
        final var order = service.createOrder(new Order(
                null /* generated */,
                "Mobile Line", "Mobile Line",
                date, date, products.findProducts(), null /* set by service */));
        return order.id();
    }
}
