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
package org.dbflute.utflute.lasta.s2container;

import java.io.File;
import java.lang.reflect.Field;

import javax.annotation.Resource;

import junit.framework.AssertionFailedError;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.lasta.di.core.annotation.Binding;
import org.dbflute.lasta.di.core.annotation.BindingType;
import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassHandler;
import org.dbflute.util.Srl;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public class SmartDeployDependencyChecker implements PoliceStoryJavaClassHandler {

    protected final String _title;
    protected final String _suffix;

    public SmartDeployDependencyChecker(String title, String suffix) {
        _title = title;
        _suffix = suffix;
    }

    public void handle(File srcFile, Class<?> clazz) { // field injection only for now
        final Field[] declaredFields = extractDeclaredFields(clazz);
        for (Field field : declaredFields) {
            if (!hasInjectionAnnotation(clazz, field)) {
                continue;
            }
            // DI component here
            final Class<?> injectedType = field.getType();
            final String injectedClassName = extractInjectedClassName(injectedType);
            if (!injectedClassName.endsWith(_suffix)) {
                continue;
            }
            // target class here
            processTargetClass(clazz, field, injectedType);
            if (injectedType.isInterface()) { // target but interface
                continue; // by-type is valid so OK
            }
            if (checkInjectionField(clazz, field, injectedType)) {
                continue;
            }
            throwInjectionPropertyNameDifferentException(clazz, field, injectedType);
        }
    }

    protected Field[] extractDeclaredFields(Class<?> clazz) { // customize point
        return clazz.getDeclaredFields(); // contains private
    }

    protected boolean hasInjectionAnnotation(Class<?> clazz, Field field) {
        if (field.getAnnotation(Resource.class) != null) {
            return true;
        }
        final Binding binding = field.getAnnotation(Binding.class);
        return binding != null && !BindingType.NONE.equals(binding.bindingType());
    }

    protected void processTargetClass(Class<?> clazz, Field field, Class<?> injectedType) {
        // you can override as you like it
    }

    protected boolean checkInjectionField(Class<?> clazz, Field field, Class<?> injectedType) { // customize point
        final String expectedFieldName = extractExpectedPropertyName(injectedType);
        return expectedFieldName.equals(field.getName());
    }

    protected void throwInjectionPropertyNameDifferentException(Class<?> clazz, Field field, Class<?> injectedType) {
        final String expectedPropertyName = extractExpectedPropertyName(injectedType);
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        final String uncapTitle = Srl.initUncap(_title);
        br.addNotice("The injection property was different from the " + uncapTitle + " name.");
        br.addItem("Advice");
        br.addElement("The property name (e.g. field name) should be same as");
        br.addElement(uncapTitle + " class name (initial character is uncapitalised) like this:");
        br.addElement("  (x):");
        br.addElement("    @Resource");
        br.addElement("    protected Foo" + _suffix + " fooFlute;");
        br.addElement("  (o):");
        br.addElement("    @Resource");
        br.addElement("    protected Foo" + _suffix + " foo" + _suffix + ";");
        br.addItem("Injection Property");
        br.addElement(clazz.getName());
        br.addElement(field.getName());
        br.addElement("(expected: " + expectedPropertyName + ")");
        br.addItem(_title + " Class");
        br.addElement(injectedType.getName());
        final String msg = br.buildExceptionMessage();
        throw new AssertionFailedError(msg);
    }

    protected String extractInjectedClassName(Class<?> injectedType) {
        return injectedType.getSimpleName();
    }

    protected String extractExpectedPropertyName(Class<?> injectedType) {
        return Srl.initUncap(extractInjectedClassName(injectedType));
    }
}
