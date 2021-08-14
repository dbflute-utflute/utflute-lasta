/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.dbflute.utflute.core.binding;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * @author jflute
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public interface BindingRuleProvider {

    /**
     * Get the rule map of binding annotation for the DI container.
     * @return The map of annotation type (key) and rule object (value). (NotNull)
     */
    Map<Class<? extends Annotation>, BindingAnnotationRule> provideBindingAnnotationRuleMap();

    /**
     * Filter the property name by the binding naming rule for the DI container.
     * @param propertyName The property name defined for injected component. (NotNull)
     * @param propertyType The type of the property. (NotNull)
     * @return The filtered component name of the property. (NullAllowed: if null, no filtering)
     */
    String filterByBindingNamingRule(String propertyName, Class<?> propertyType);
}
