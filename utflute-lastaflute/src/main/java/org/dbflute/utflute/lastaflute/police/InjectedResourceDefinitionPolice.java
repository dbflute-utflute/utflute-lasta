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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Resource;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassHandler;
import org.dbflute.util.DfReflectionUtil;

/**
 * @author jflute
 * @since 0.6.1A (2016/08/17 Wednesday)
 */
public class InjectedResourceDefinitionPolice implements PoliceStoryJavaClassHandler {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected boolean alwaysPrivateOrProtectedField;
    protected Predicate<Field> privateFieldDeterminer;

    // ===================================================================================
    //                                                                              Handle
    //                                                                              ======
    public void handle(File srcFile, Class<?> clazz) {
        final List<Field> fieldList = DfReflectionUtil.getWholeFieldList(clazz);
        for (Field field : fieldList) {
            if (field.isAnnotationPresent(Resource.class)) {
                check(clazz, field);
            }
        }
    }

    // ===================================================================================
    //                                                                         Check Logic
    //                                                                         ===========
    protected void check(Class<?> targetType, Field targetField) {
        if (alwaysPrivateOrProtectedField) {
            if (!isPrivate(targetField) && !isProtected(targetField)) { // e.g. public, package scope
                throwInjectedResourceNonPrivateOrProtectedFieldException(targetType, targetField);
            }
        }
        if (privateFieldDeterminer != null && privateFieldDeterminer.test(targetField)) {
            if (!isPrivate(targetField)) { // e.g. public, protected
                throwInjectedResourceNonPrivateFieldException(targetType, targetField);
            }
        }
    }

    protected void throwInjectedResourceNonPrivateOrProtectedFieldException(Class<?> targetType, Field targetField) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Non private or protected field for injected resource.");
        br.addItem("Advice");
        br.addElement("You should define @Resource field as priavte or protected.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Resource");
        br.addElement("    public MemberBhv memberBhv; // *Bad");
        br.addElement("  (x):");
        br.addElement("    @Resource");
        br.addElement("    MemberBhv memberBhv; // *Bad");
        br.addElement("  (o):");
        br.addElement("    @Resource");
        br.addElement("    private MemberBhv memberBhv; // Good");
        br.addElement("  (o):");
        br.addElement("    @Resource");
        br.addElement("    protected MemberBhv memberBhv; // Good");
        br.addItem("Target Class");
        br.addElement(targetType);
        br.addItem("Target Field");
        br.addElement(targetField);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }

    protected void throwInjectedResourceNonPrivateFieldException(Class<?> targetType, Field targetField) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Non private field for injected resource.");
        br.addItem("Advice");
        br.addElement("You should define @Resource field as priavte.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Resource");
        br.addElement("    public MemberBhv memberBhv; // *Bad");
        br.addElement("  (x):");
        br.addElement("    @Resource");
        br.addElement("    protected MemberBhv memberBhv; // *Bad");
        br.addElement("  (o):");
        br.addElement("    @Resource");
        br.addElement("    private MemberBhv memberBhv; // Good");
        br.addItem("Target Class");
        br.addElement(targetType);
        br.addItem("Target Field");
        br.addElement(targetField);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }

    // ===================================================================================
    //                                                                          Check Rule
    //                                                                          ==========
    public InjectedResourceDefinitionPolice alwaysPrivateOrProtectedField() {
        this.alwaysPrivateOrProtectedField = true;
        return this;
    }

    public InjectedResourceDefinitionPolice shouldBePrivateField(Predicate<Field> oneArgLambda) {
        this.privateFieldDeterminer = oneArgLambda;
        return this;
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected boolean isPrivate(Field field) {
        return Modifier.isPrivate(field.getModifiers());
    }

    protected boolean isProtected(Field field) {
        return Modifier.isProtected(field.getModifiers());
    }
}
