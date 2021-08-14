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
package org.dbflute.utflute.lastaflute.police;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.function.Function;

import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassHandler;

/**
 * @author jflute
 */
public class ActionComponentPolice implements PoliceStoryJavaClassHandler {

    protected final Function<Class<?>, Object> componentProvider;

    public ActionComponentPolice(Function<Class<?>, Object> componentProvider) {
        this.componentProvider = componentProvider;
    }

    public void handle(File srcFile, Class<?> clazz) {
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) { // e.g. BaseAction
            return;
        }
        final String className = clazz.getName();
        if (className.contains(getWebPackageKeyword()) && className.endsWith(getActionSuffix())) {
            final Object action = componentProvider.apply(clazz); // expect no exception
            if (action == null) { // basically no way, exception before here
                throw new IllegalStateException("Not found the action: " + clazz);
            }
        }
    }

    protected String getWebPackageKeyword() {
        return ".app.web.";
    }

    protected String getActionSuffix() {
        return "Action";
    }
}
