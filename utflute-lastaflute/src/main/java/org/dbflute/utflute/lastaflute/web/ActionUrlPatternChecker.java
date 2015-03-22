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
package org.dbflute.utflute.lastaflute.web;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.lastaflute.web.ActionForm;
import org.dbflute.lastaflute.web.Execute;
import org.dbflute.lastaflute.web.exception.IllegalUrlPatternRuntimeException;
import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassHandler;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public class ActionUrlPatternChecker implements PoliceStoryJavaClassHandler {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final String ELEMENT_PATTERN = "([^/]+)"; // from SAFlute

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String _actionSuffix;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionUrlPatternChecker(String actionSuffix) {
        _actionSuffix = actionSuffix;
    }

    // ===================================================================================
    //                                                                              Handle
    //                                                                              ======
    public void handle(File srcFile, Class<?> actionType) { // field injection only for now
        if (!actionType.getName().endsWith(_actionSuffix)) {
            return;
        }
        final Method[] methods = actionType.getDeclaredMethods();
        for (Method method : methods) {
            int methodModifiers = method.getModifiers();
            if (!isPublicInstanceMember(methodModifiers)) {
                continue;
            }
            final Execute executeAnno = method.getAnnotation(Execute.class);
            if (executeAnno == null) {
                continue;
            }
            final String urlPattern = executeAnno.urlPattern();
            if (urlPattern == null || urlPattern.trim().length() == 0) {
                continue;
            }
            // the @Execute method that has urlPattern here
            final List<String> urlParamNames = new ArrayList<String>();
            analyzeUrlPattern(urlPattern, urlParamNames, new ArrayList<String>()); // not use required list

            final Set<String> urlParamSet = new HashSet<String>(urlParamNames);
            final List<Field> targetFieldList = new ArrayList<Field>();
            final Class<?> formType = extractActionFormType(actionType);
            for (Field field : formType.getDeclaredFields()) { // contains protected and concrete only
                if (!isPublicInstanceMember(field.getModifiers())) {
                    continue;
                }
                // public instance field here
                targetFieldList.add(field);
                for (String paramName : urlParamNames) {
                    if (field.getName().equals(paramName)) {
                        urlParamSet.remove(paramName);
                    }
                }
            }
            if (!urlParamSet.isEmpty()) {
                throwUrlPatternPropertyNotFoundException(actionType, method, urlPattern, formType, targetFieldList, urlParamSet);
            }
        }
    }

    protected boolean isPublicInstanceMember(int modifiers) {
        return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
    }

    protected Class<?> extractActionFormType(Class<?> actionType) {
        Class<?> formType = null;
        for (Field field : actionType.getDeclaredFields()) { // contains protected and concrete only
            if (field.getAnnotation(ActionForm.class) != null) {
                formType = field.getType();
            }
        }
        if (formType == null) {
            formType = actionType; // SAStruts's specification: action is action form if no annotation
        }
        return formType;
    }

    protected boolean hasAnnotation(Annotation[] annotations, String annoName) {
        for (Annotation annotation : annotations) {
            if (annoName.equals(annotation.getClass().getName())) {
                return true;
            }
        }
        return false;
    }

    protected void throwUrlPatternPropertyNotFoundException(Class<?> actionType, Method method, String urlPattern, Class<?> formType,
            List<Field> targetFieldList, Set<String> notFoundPropertySet) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the urlPattern property in the action form.");
        br.addItem("Advice");
        br.addElement("The specified property in urlPattern should exist in ActionForm.");
        br.addElement("For example,");
        br.addElement("  (x):");
        br.addElement("    (Action)");
        br.addElement("    urlPattern = \"{memberId}\"");
        br.addElement("    (ActionForm)");
        br.addElement("    public String membirId; // *NG");
        br.addElement("  (o):");
        br.addElement("    (Action)");
        br.addElement("    urlPattern = \"{memberId}\"");
        br.addElement("    (ActionForm)");
        br.addElement("    public String memberId; // OK");
        br.addItem("Action");
        br.addElement(actionType.getName() + "#" + method.getName() + "()");
        br.addItem("urlPattern");
        br.addElement(urlPattern);
        br.addItem("ActionForm");
        br.addElement(formType.getName());
        br.addItem("Public Field");
        final StringBuilder fieldExpSb = new StringBuilder();
        int index = 0;
        for (Field targetField : targetFieldList) {
            if (index % 3 == 0) {
                fieldExpSb.append("\n");
            }
            if (index > 0) {
                fieldExpSb.append(", ");
            }
            fieldExpSb.append(targetField.getName());
            ++index;
        }
        br.addElement(fieldExpSb.toString().trim());
        br.addItem("NotFound Property");
        br.addElement(notFoundPropertySet);
        final String msg = br.buildExceptionMessage();
        throw new AssertionFailedError(msg);
    }

    // ===================================================================================
    //                                                                     Framework Logic
    //                                                                     ===============
    // from SAFlute, keep plain code to re-copy as possible
    protected String analyzeUrlPattern(String urlPattern, List<String> urlParamNames, List<String> urlParamRequiredSet) {
        final StringBuilder sb = new StringBuilder(50);
        final char[] chars = urlPattern.toCharArray();
        final int length = chars.length;
        Character previousChar = null;
        boolean requiredElement = false;
        int index = -1;
        for (int i = 0; i < length; i++) {
            final char currentChar = chars[i];
            if (currentChar == '{') {
                index = i;
            } else if (previousChar != null && previousChar == '{' && currentChar == '*') {
                // e.g. {*id}/{*name} means required parameter, 404 not found if no value
                index = i; // to skip required mark
                requiredElement = true;
            } else if (currentChar == '}') {
                if (index >= 0) {
                    sb.append(ELEMENT_PATTERN);
                    final String elementName = urlPattern.substring(index + 1, i);
                    urlParamNames.add(elementName);
                    if (requiredElement) {
                        urlParamRequiredSet.add(elementName);
                    }
                    requiredElement = false;
                    index = -1;
                } else {
                    throw new IllegalUrlPatternRuntimeException(urlPattern);
                }
            } else if (index < 0) {
                sb.append(currentChar);
                requiredElement = false;
            }
            previousChar = currentChar;
        }
        if (index >= 0) {
            throw new IllegalUrlPatternRuntimeException(urlPattern);
        }
        return sb.toString();
    }
}
