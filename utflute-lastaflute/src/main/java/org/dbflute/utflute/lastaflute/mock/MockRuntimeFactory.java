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
package org.dbflute.utflute.lastaflute.mock;

import java.util.Collections;

import org.lastaflute.di.core.meta.impl.ComponentDefImpl;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionMapping;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.ruts.process.RequestUrlParam;

/**
 * @author jflute
 */
public class MockRuntimeFactory {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionMapping mapping;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MockRuntimeFactory() {
        final MockRomanticActionCustomizer customizer = createMockRomanticActionCustomizer();
        mapping = customizer.createActionMapping(createComponentDefImpl(MockAction.class, "mockAction"));
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
        final ActionExecute execute = mapping.getExecuteMap().get("sea");
        return new ActionRuntime("/mock/sea/", execute, createRequestUrlParam());
    }

    public ActionRuntime createJsonRuntime() {
        final ActionExecute execute = mapping.getExecuteMap().get("land");
        return new ActionRuntime("/mock/land/", execute, createRequestUrlParam());
    }

    protected RequestUrlParam createRequestUrlParam() {
        return new RequestUrlParam(Collections.emptyList(), Collections.emptyMap());
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public ActionMapping getMapping() {
        return mapping;
    }
}
