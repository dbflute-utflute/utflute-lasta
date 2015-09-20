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
package org.dbflute.utflute.core.document;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author p1us2er0
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
// TODO jflute class naming. Handler or Provider or ...  by p1us2er0 (2015/09/18)
public interface SourceParserHandler {

    // ===================================================================================
    //                                                                             Reflect
    //                                                                             =======
    void reflect(ActionDocMeta bean, Method method, List<String> srcDirList);

    void reflect(TypeDocMeta bean, Class<?> clazz, List<String> srcDirList);
}
