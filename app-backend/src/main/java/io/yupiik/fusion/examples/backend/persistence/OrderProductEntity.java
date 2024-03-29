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

import io.yupiik.fusion.examples.backend.model.OrderStatus;
import io.yupiik.fusion.framework.build.api.persistence.Column;
import io.yupiik.fusion.framework.build.api.persistence.Id;
import io.yupiik.fusion.framework.build.api.persistence.OnDelete;
import io.yupiik.fusion.framework.build.api.persistence.OnInsert;
import io.yupiik.fusion.framework.build.api.persistence.OnLoad;
import io.yupiik.fusion.framework.build.api.persistence.OnUpdate;
import io.yupiik.fusion.framework.build.api.persistence.Table;

import java.time.LocalDateTime;

@Table("FUSION_ORDER_PRODUCT")
public record OrderProductEntity(
        @Id(order = 0)
        @Column(name = "ORDER_ID")  String orderId,
        @Id(order = 1)
        @Column(name = "PRODUCT_ID")  String productId)
{}
