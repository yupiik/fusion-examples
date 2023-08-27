/*
 * Copyright (c) 2022-2023 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.fusion.examples.restapi.endpoint;

import io.yupiik.fusion.examples.restapi.model.ProductCreate;
import io.yupiik.fusion.examples.restapi.model.ProductUpdate;
import io.yupiik.fusion.examples.restapi.service.ProductService;
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.framework.build.api.http.HttpMatcher;
import io.yupiik.fusion.http.server.api.Request;
import io.yupiik.fusion.http.server.api.Response;
import io.yupiik.fusion.json.JsonMapper;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedStage;

@ApplicationScoped
public class Endpoint {

    private final ProductService productService;
    private final JsonMapper jsonMapper;

    public Endpoint(ProductService productService, JsonMapper jsonMapper) {
        this.productService = productService;
        this.jsonMapper = jsonMapper;
    }

    @HttpMatcher(methods = "POST", path = "/product", pathMatching = HttpMatcher.PathMatching.EXACT)
    public CompletionStage<Response> createProduct(final Request request, final ProductCreate productCreate) {
        return completedStage(Response.of()
            .status(201)
            .header("content-type", "application/json")
            .body(jsonMapper.toString(productService.createProduct(request, productCreate)))
            .build());
    }

    @HttpMatcher(methods = "GET", path = "/product/", pathMatching = HttpMatcher.PathMatching.STARTS_WITH)
    public CompletionStage<Response> findProduct(final Request request) {
        final var id = request.path().split("/")[2];
        return completedStage(Response.of()
                .status(200)
                .header("content-type", "application/json")
                .body(jsonMapper.toString(productService.findProduct(id)))
                .build());
    }

    @HttpMatcher(methods = "DELETE", path = "/product/", pathMatching = HttpMatcher.PathMatching.STARTS_WITH)
    public CompletionStage<Response> deleteProduct(final Request request) {
        final var id = request.path().split("/")[2];
        return completedStage(productService.deleteProduct(id))
                .thenApply(product -> Response.of()
                    .status(204)
                    .header("content-type", "application/json")
                    .body(jsonMapper.toString(product))
                    .build())
                .exceptionally(throwable -> Response.of()
                    .status(404)
                    .header("content-type", "application/json")
                    .body(jsonMapper.toString(throwable.getMessage()))
                    .build());
    }

    @HttpMatcher(methods = "PATCH", path = "/product/", pathMatching = HttpMatcher.PathMatching.STARTS_WITH)
    public CompletionStage<Response> updateProduct(final Request request, final ProductUpdate productUpdate) {
        final var id = request.path().split("/")[2];
        return completedStage(Response.of()
                .status(200)
                .header("content-type", "application/json")
                .body(jsonMapper.toString(productService.patchProduct(id, productUpdate)))
                .build());
    }
}
