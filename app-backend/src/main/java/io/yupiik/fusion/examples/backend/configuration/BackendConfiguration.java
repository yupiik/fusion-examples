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
package io.yupiik.fusion.examples.backend.configuration;

import io.yupiik.fusion.framework.build.api.configuration.Property;
import io.yupiik.fusion.framework.build.api.configuration.RootConfiguration;

import java.util.Map;

@RootConfiguration("backend")
public record BackendConfiguration(
        @Property(defaultValue = "\"productInventory.json\"", documentation = "Resource to load as default product inventory.")
        String productInventoryResource,
        @Property(defaultValue = "\"-\"", documentation = "Zipkin URL if tracing is enabled.")
        String zipkin,
        @Property(documentation = "Header list.")
        Map<String, String> headers) {
}
