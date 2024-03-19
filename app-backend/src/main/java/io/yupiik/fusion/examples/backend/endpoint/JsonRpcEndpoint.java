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
package io.yupiik.fusion.examples.backend.endpoint;

import io.yupiik.fusion.examples.backend.model.Order;
import io.yupiik.fusion.examples.backend.model.Product;
import io.yupiik.fusion.examples.backend.service.OrderService;
import io.yupiik.fusion.examples.backend.service.ProductService;
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.framework.build.api.jsonrpc.JsonRpc;
import io.yupiik.fusion.http.server.api.Request;
import io.yupiik.fusion.json.JsonMapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class JsonRpcEndpoint {

    private final OrderService orderService;
    private final ProductService productService;
    private final JsonMapper jsonMapper;

    public JsonRpcEndpoint(final OrderService orderService, final ProductService productService, final JsonMapper jsonMapper) {
        this.orderService = orderService;
        this.productService = productService;
        this.jsonMapper = jsonMapper;
    }

    @JsonRpc(value = "fusion.examples.product.findAll", documentation = "Fetch all product available")
    public List<Product> findAllProduct(final Request request) {
        return productService.findProducts();
    }

    @JsonRpc(value = "fusion.examples.product.findById", documentation = "Find a product by id")
    public Product findProduct(final Request request, final String id) {
        return productService.findProduct(id);
    }

    @JsonRpc(value = "fusion.examples.order.create", documentation = "Create a new order")
    public CompletionStage<Order> createOrder(final Request request, final Order order) {
        return CompletableFuture.completedStage(orderService.createOrder(request, order));
    }

    @JsonRpc(value = "fusion.examples.order.findAll", documentation = "Find all created order")
    public List<Order> findAllOrder(final Request request) {
        return orderService.findOrders();
    }

    @JsonRpc(value = "fusion.examples.order.findById", documentation = "Find an order by id")
    public Order findOrder(final Request request, final String id) {
        return orderService.findOrder(id);
    }

    @JsonRpc(value = "fusion.examples.order.delete", documentation = "Delete an order by id")
    public CompletionStage<Order> deleteOrder(final Request request, final String id) {
        return CompletableFuture.completedStage(orderService.deleteOrder(id));
    }

    @JsonRpc(value = "fusion.examples.order.update", documentation = "Update an existing order")
    public CompletionStage<Order> updateOrder(final Request request, final Order order) {
        return CompletableFuture.completedStage(orderService.updateOrder(order.id(), order));
    }
}
