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

import io.yupiik.fusion.framework.api.RuntimeContainer;
import io.yupiik.fusion.testing.impl.FusionParameterResolver;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Collection;

import static io.yupiik.fusion.examples.backend.model.test.Data.Phase.AFTER;
import static io.yupiik.fusion.examples.backend.model.test.Data.Phase.BEFORE;
import static java.util.Optional.ofNullable;
import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;

public class DataExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(DataExtension.class);

    @Override
    public void beforeEach(final ExtensionContext ctx) {
        final var items = findRepeatableAnnotations(ctx.getElement(), Data.class);
        if (items.isEmpty()) {
            return;
        }

        final var container = ctx.getStore(ExtensionContext.Namespace.create(FusionParameterResolver.class))
                .get(RuntimeContainer.class, RuntimeContainer.class);
        final var store = ctx.getStore(NAMESPACE);
        final var holder = new Holder(items, container, store);
        holder.run(BEFORE);
        store.put(Holder.class, holder);
    }

    @Override
    public void afterEach(final ExtensionContext ctx) {
        ofNullable(ctx.getStore(NAMESPACE).get(Holder.class, Holder.class))
                .ifPresent(holder -> holder.run(AFTER));
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext ctx) throws ParameterResolutionException {
        return resolveParameter(parameterContext, ctx) != null;
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext ctx) throws ParameterResolutionException {
        final var type = parameterContext.getParameter().getType();
        return ctx.getStore(NAMESPACE).get(type, type);
    }

    private record Holder(Collection<Data> data, RuntimeContainer container, ExtensionContext.Store store) {
        private void run(final Data.Phase phase) {
            data.stream()
                    .filter(it -> it.phase() == phase)
                    .map(Data::type)
                    .forEach(it -> it.accept(container, store));
        }
    }
}
