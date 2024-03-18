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

import io.yupiik.fusion.examples.backend.model.Product;
import io.yupiik.fusion.framework.api.container.Types;
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@ApplicationScoped
public class ProductService {

    private final Logger logger = Logger.getLogger(ProductService.class.getName());

    private final ConcurrentHashMap<String, Product> productInventory = new ConcurrentHashMap<>();

    private final JsonMapper jsonMapper;

    public ProductService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        // load productInventory from json file
        try (final var initProductInventory = ProductService.class.getClassLoader().getResourceAsStream("productInventory.json")) {
            List<Product> products = jsonMapper.fromBytes(new Types.ParameterizedTypeImpl(List.class, Product.class), initProductInventory.readAllBytes());
            products.forEach(product -> productInventory.put(product.id(), product));
        } catch (Exception exception) {
            logger.severe("Unable to load product inventory init list from resource file");
        }
    }

    public Product findProduct(String id) {
        return productInventory.get(id);
    }

    public List<Product> findProducts() {
        return productInventory.values().stream().toList();
    }
}
