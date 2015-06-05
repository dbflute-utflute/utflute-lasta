/*
 * Copyright 2014-2015 the original author or authors.
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

/**
 * @author jflute
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public interface NonBindingDeterminer {

    /**
     * Should be the annotation treated as non-binding?
     * @param bindingAnno The annotation instance for binding to determine. (NotNull)
     * @return The determination, true or false.
     */
    boolean isNonBinding(Annotation bindingAnno);
}
