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
import io.yupiik.fusion.examples.backend.service.OrderService;
import io.yupiik.fusion.examples.backend.service.ProductService;
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.framework.build.api.http.HttpMatcher;
import io.yupiik.fusion.http.server.api.Request;
import io.yupiik.fusion.http.server.api.Response;
import io.yupiik.fusion.json.JsonMapper;

@ApplicationScoped
public class RestEndpoint {

    private final OrderService orderService;
    private final ProductService productService;
    private final JsonMapper jsonMapper;

    public RestEndpoint(final OrderService orderService, final ProductService productService, final JsonMapper jsonMapper) {
        this.orderService = orderService;
        this.productService = productService;
        this.jsonMapper = jsonMapper;
    }

    @HttpMatcher(methods = "GET", path = "/product", pathMatching = HttpMatcher.PathMatching.STARTS_WITH)
    public Response findAllProduct(final Request request) {
        return Response.of()
                .status(200)
                .header("content-type", "application/json")
                .body(jsonMapper.toString(productService.findProducts()))
                .build();
    }

    @HttpMatcher(methods = "GET", path = "/product/", pathMatching = HttpMatcher.PathMatching.STARTS_WITH)
    public Response findProduct(final Request request) {
        final var id = request.path().split("/")[2];
        return Response.of()
                .status(200)
                .header("content-type", "application/json")
                .body(jsonMapper.toString(productService.findProduct(id)))
                .build();
    }

    @HttpMatcher(methods = "POST", path = "/order", pathMatching = HttpMatcher.PathMatching.EXACT)
    public Response createOrder(final Request request, final Order order) {
        return Response.of()
            .status(201)
            .header("content-type", "application/json")
            .body(jsonMapper.toString(orderService.createOrder(request, order)))
            .build();
    }

    @HttpMatcher(methods = "GET", path = "/order", pathMatching = HttpMatcher.PathMatching.STARTS_WITH)
    public Response findAllOrder(final Request request) {
        return Response.of()
                .status(200)
                .header("content-type", "application/json")
                .body(jsonMapper.toString(orderService.findOrders()))
                .build();
    }

    @HttpMatcher(methods = "GET", path = "/order/", pathMatching = HttpMatcher.PathMatching.STARTS_WITH)
    public Response findOrder(final Request request) {
        final var id = request.path().split("/")[2];
        return Response.of()
                .status(200)
                .header("content-type", "application/json")
                .body(jsonMapper.toString(orderService.findOrder(id)))
                .build();
    }

    @HttpMatcher(methods = "DELETE", path = "/order/", pathMatching = HttpMatcher.PathMatching.STARTS_WITH)
    public Response deleteOrder(final Request request) {
        final var id = request.path().split("/")[2];
        try {
            return Response.of()
                    .status(204)
                    .header("content-type", "application/json")
                    .body(jsonMapper.toString(orderService.deleteOrder(id)))
                    .build();
        } catch (Exception ex) {
            return Response.of()
                    .status(404)
                    .header("content-type", "application/json")
                    .body(jsonMapper.toString(ex.getMessage()))
                    .build();
        }
    }

    @HttpMatcher(methods = "PATCH", path = "/order/", pathMatching = HttpMatcher.PathMatching.STARTS_WITH)
    public Response updateOrder(final Request request, final Order order) {
        final var id = request.path().split("/")[2];
        return Response.of()
                .status(200)
                .header("content-type", "application/json")
                .body(jsonMapper.toString(orderService.updateOrder(id, order)))
                .build();
    }
}
