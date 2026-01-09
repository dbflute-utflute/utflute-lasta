/*
 * Copyright 2014-2024 the original author or authors.
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

/**
 * @author jflute
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class BindingAnnotationRule {

    protected BindingFindingType _findingType;
    protected NonBindingDeterminer _nonBindingDeterminer;

    public boolean isByNameOnly() {
        return BindingFindingType.BY_NAME_ONLY.equals(_findingType);
    }

    public BindingAnnotationRule byNameOnly() {
        _findingType = BindingFindingType.BY_NAME_ONLY;
        return this;
    }

    public boolean isByTypeOnly() {
        return BindingFindingType.BY_TYPE_ONLY.equals(_findingType);
    }

    public BindingAnnotationRule byTypeOnly() {
        _findingType = BindingFindingType.BY_TYPE_ONLY;
        return this;
    }

    public NonBindingDeterminer getNonBindingDeterminer() {
        return _nonBindingDeterminer;
    }

    public BindingAnnotationRule determineNonBinding(NonBindingDeterminer nonBindingDeterminer) {
        this._nonBindingDeterminer = nonBindingDeterminer;
        return this;
    }
}
