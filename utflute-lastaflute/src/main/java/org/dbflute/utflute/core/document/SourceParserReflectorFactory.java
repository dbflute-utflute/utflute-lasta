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

import java.util.List;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfReflectionUtil.ReflectionFailureException;

/**
 * @author p1us2er0
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class SourceParserReflectorFactory {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final String JAVA_PARSER_CLASS_NAME = "com.github.javaparser.JavaParser";

    // ===================================================================================
    //                                                                           Reflector
    //                                                                           =========
    public OptionalThing<SourceParserReflector> reflector(List<String> srcDirList) { // empty allowed if not found
        SourceParserReflector sourceParserReflector = null;
        try {
            DfReflectionUtil.forName(JAVA_PARSER_CLASS_NAME);
            sourceParserReflector = new JavaparserSourceParserReflector(srcDirList);
        } catch (ReflectionFailureException e) {
            sourceParserReflector = null;
        }
        return OptionalThing.ofNullable(sourceParserReflector, () -> {
            throw new IllegalStateException("Not found the java parser: " + JAVA_PARSER_CLASS_NAME);
        });
    }
}
