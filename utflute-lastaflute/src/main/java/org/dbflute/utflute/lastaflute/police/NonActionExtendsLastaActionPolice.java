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
package org.dbflute.utflute.lastaflute.police;

import java.io.File;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassHandler;
import org.dbflute.util.Srl;
import org.lastaflute.web.LastaAction;

/**
 * @author jflute
 */
public class NonActionExtendsLastaActionPolice implements PoliceStoryJavaClassHandler {

    public void handle(File srcFile, Class<?> clazz) {
        check(srcFile, clazz, getAssistSuffix());
        check(srcFile, clazz, getLogicSuffix());
        check(srcFile, clazz, getServiceSuffix());
    }

    protected void check(File srcFile, Class<?> clazz, String logicSuffix) {
        if (clazz.getName().endsWith(logicSuffix) && LastaAction.class.isAssignableFrom(clazz)) {
            throwLogicExtendsBaseActionException(srcFile, clazz, logicSuffix);
        }
    }

    protected String getAssistSuffix() {
        return "Assist";
    }

    protected String getLogicSuffix() {
        return "Logic";
    }

    protected String getServiceSuffix() {
        return "Service";
    }

    protected void throwLogicExtendsBaseActionException(File srcFile, Class<?> clazz, String suffix) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("No no no, the " + Srl.initUncap(suffix) + " extends action.");
        br.addItem("Advice");
        br.addElement(suffix + " cannot extend action.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public class Sea" + suffix + " extends LandBaseAction { // *Bad");
        br.addElement("  (o):");
        br.addElement("    public class Sea" + suffix + " { // Good");
        br.addElement("  (o):");
        br.addElement("    public class Sea" + suffix + " extends LandBase" + suffix + " { // Good");
        br.addItem("Logic Class");
        br.addElement(clazz.getName());
        br.addElement(srcFile);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }
}