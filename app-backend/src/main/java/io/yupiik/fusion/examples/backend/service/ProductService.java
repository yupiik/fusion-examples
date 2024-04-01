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

import io.yupiik.fusion.examples.backend.configuration.BackendConfiguration;
import io.yupiik.fusion.examples.backend.model.Product;
import io.yupiik.fusion.framework.api.container.Types;
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.framework.build.api.lifecycle.Init;
import io.yupiik.fusion.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@ApplicationScoped
public class ProductService {
    private final Logger logger = Logger.getLogger(ProductService.class.getName());

    private final BackendConfiguration configuration;
    private final Map<String, Product> productInventory = new LinkedHashMap<>();

    private final JsonMapper jsonMapper;

    private List<Product> products;

    public ProductService(final BackendConfiguration configuration, final JsonMapper jsonMapper) {
        this.configuration = configuration;
        this.jsonMapper = jsonMapper;
    }

    @Init
    public void loadDemoData() {
        try (final var initProductInventory = ProductService.class.getClassLoader()
                .getResourceAsStream(configuration.productInventoryResource())) {
            final List<Product> products = jsonMapper.fromBytes(new Types.ParameterizedTypeImpl(List.class, Product.class), initProductInventory.readAllBytes());
            productInventory.putAll(products.stream().collect(toMap(Product::id, identity())));
        } catch (final Exception exception) {
            logger.severe("Unable to load product inventory init list from resource file");
        }
        products = productInventory.values().stream().toList();
    }

    public Product findProduct(final String id) {
        return productInventory.get(id);
    }

    public List<Product> findProducts() {
        return products;
    }
}
