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
package org.dbflute.utflute.lastaflute.mock;

import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.web.aspect.RomanticActionCustomizer;
import org.lastaflute.web.ruts.config.ActionMapping;

/**
 * @author jflute
 */
public class MockRomanticActionCustomizer extends RomanticActionCustomizer {

    @Override
    public ActionMapping createActionMapping(ComponentDef actionDef) { // to be public
        return super.createActionMapping(actionDef);
    }
}
