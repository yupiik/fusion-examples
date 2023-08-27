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
package io.yupiik.fusion.examples.restapi.service;

import io.yupiik.fusion.examples.restapi.model.Product;
import io.yupiik.fusion.examples.restapi.model.ProductCreate;
import io.yupiik.fusion.examples.restapi.model.ProductStatusType;
import io.yupiik.fusion.examples.restapi.model.ProductUpdate;
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.http.server.api.Request;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ProductService {

    private final ConcurrentHashMap<String, Product> productInventory = new ConcurrentHashMap<>();

    public Product findProduct(String id) {
        return productInventory.get(id);
    }

    public Product createProduct(Request request, ProductCreate productCreate) {
        String uri = request.scheme() + "://" + request.header("host") + request.path();
        String newId = UUID.randomUUID().toString();
        Product newProduct = new Product(
                newId,
                uri + "/" + newId,
                productCreate.description(),
                productCreate.name(),
                LocalDate.now(),
                LocalDate.now(),
                productCreate.productSerialNumber(),
                ProductStatusType.created
        );
        productInventory.putIfAbsent(newId, newProduct);
        return newProduct;
    }

    public Product patchProduct(String productId, ProductUpdate productUpdate) {
        Product updatedProduct = productInventory.computeIfPresent(productId, (id, product) ->
                new Product(
                        product.id(),
                        product.href(),
                        Optional.ofNullable(productUpdate.description()).orElse(product.description()),
                        Optional.ofNullable(productUpdate.name()).orElse(product.name()),
                        product.creationDate(),
                        LocalDate.now(),
                        Optional.ofNullable(productUpdate.productSerialNumber()).orElse(product.productSerialNumber()),
                        Optional.ofNullable(productUpdate.status()).orElse(product.status())
                )
        );
        productInventory.replace(productId, updatedProduct);
        return updatedProduct;
    }

    public Product deleteProduct(String productId) {
        Product product = productInventory.get(productId);
        if (Objects.isNull(product)) throw new IllegalStateException("Not found");
        productInventory.remove(productId);
        return product;
    }
}
