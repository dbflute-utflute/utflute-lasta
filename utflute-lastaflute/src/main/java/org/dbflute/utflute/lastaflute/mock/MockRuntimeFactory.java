/*
 * Copyright 2014-2017 the original author or authors.
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

import java.util.Collections;

import org.lastaflute.di.core.meta.impl.ComponentDefImpl;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionMapping;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.ruts.process.pathparam.RequestPathParam;

/**
 * @author jflute
 */
public class MockRuntimeFactory {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionMapping _mapping;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MockRuntimeFactory() {
        final MockRomanticActionCustomizer customizer = createMockRomanticActionCustomizer();
        _mapping = customizer.createActionMapping(createComponentDefImpl(MockAction.class, "mockAction"));
    }

    protected MockRomanticActionCustomizer createMockRomanticActionCustomizer() {
        return new MockRomanticActionCustomizer();
    }

    protected ComponentDefImpl createComponentDefImpl(Class<?> componentClass, String componentName) {
        return new ComponentDefImpl(componentClass, componentName);
    }

    // ===================================================================================
    //                                                                             Creator
    //                                                                             =======
    public ActionRuntime createHtmlRuntime() {
        final ActionExecute execute = _mapping.getExecuteMap().get("sea");
        return new ActionRuntime("/mock/sea/", execute, createRequestPathParam());
    }

    public ActionRuntime createJsonRuntime() {
        final ActionExecute execute = _mapping.getExecuteMap().get("land");
        return new ActionRuntime("/mock/land/", execute, createRequestPathParam());
    }

    protected RequestPathParam createRequestPathParam() {
        return new RequestPathParam(Collections.emptyList(), Collections.emptyMap());
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public ActionMapping getMapping() {
        return _mapping;
    }
}
