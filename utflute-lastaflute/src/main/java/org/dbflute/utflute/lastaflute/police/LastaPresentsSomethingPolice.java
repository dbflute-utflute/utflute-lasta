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
package org.dbflute.utflute.lastaflute.police;

import java.io.File;
import java.lang.reflect.Modifier;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.utflute.core.filesystem.FileLineHandler;
import org.dbflute.utflute.core.filesystem.FilesystemPlayer;
import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassHandler;
import org.dbflute.util.Srl;

/**
 * @author jflute
 * @since 0.6.1A (2016/08/17 Wednesday)
 */
public class LastaPresentsSomethingPolice implements PoliceStoryJavaClassHandler {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected boolean _formImmutable;

    // ===================================================================================
    //                                                                              Handle
    //                                                                              ======
    public void handle(File srcFile, Class<?> clazz) {
        handleFormImmutable(srcFile, clazz);
    }

    // ===================================================================================
    //                                                                      Form Immutable
    //                                                                      ==============
    protected void handleFormImmutable(File srcFile, Class<?> clazz) {
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) { // e.g. BaseAction
            return;
        }
        if (isFormImmutableTargetClass(clazz)) {
            new FilesystemPlayer().readLine(srcFile, "UTF-8", new FileLineHandler() {
                private boolean hasFormSetup;

                @Override
                public void handle(String line) {
                    if (_formImmutable) {
                        if (!hasFormSetup && hasFormSetupMethodCall(line)) {
                            hasFormSetup = true;
                        }
                        if (!hasFormSetup) {
                            checkFormImmutableLine(clazz, line, "Form", "form."); // option
                        }
                    }
                    checkFormImmutableLine(clazz, line, "Body", "body."); // as default
                }
            });
        }
    }

    protected boolean isFormImmutableTargetClass(Class<?> clazz) {
        final String name = clazz.getName();
        return name.contains(getWebPackageKeyword()) && (name.endsWith(getActionSuffix()) || name.endsWith(getAssistSuffix()));
    }

    protected boolean hasFormSetupMethodCall(String line) {
        return line.contains("setup(form ->");
    }

    protected void checkFormImmutableLine(Class<?> clazz, String line, String title, String formPrefix) {
        if (line.contains(formPrefix)) { // simple determination for performance
            final String ltrimmedLine = Srl.ltrim(line);
            if (ltrimmedLine.startsWith(formPrefix) && ltrimmedLine.length() != line.length()) {
                final String formRear = Srl.substringFirstRear(line, formPrefix);
                if (formRear.contains(" = ")) { // not complete for now
                    throwFormImmutableBreakException(clazz, title, ltrimmedLine);
                }
            }
        }
    }

    protected void throwFormImmutableBreakException(Class<?> clazz, String title, final String ltrimmedLine) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice(title + " should be immutable but re-assignment was found.");
        br.addItem("Advice");
        br.addElement("Form and Body should be read-only object.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    form.memberName = \"sea\"; // *Bad");
        br.addElement("    body.memberAccount = \"land\"; // *Bad");
        br.addElement("  (o):");
        br.addElement("    selectMember(form.memberName); // Good");
        br.addElement("    selectMember(body.memberAccount); // Good");
        br.addItem("Target Class");
        br.addElement(clazz);
        br.addItem("Wrong Statement");
        br.addElement(ltrimmedLine);
        final String msg = br.buildExceptionMessage();
        throw new FormImmutableBrokenException(msg);
    }

    public static class FormImmutableBrokenException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public FormImmutableBrokenException(String msg) {
            super(msg);
        }
    }

    // ===================================================================================
    //                                                                              Naming
    //                                                                              ======
    protected String getWebPackageKeyword() {
        return ".app.web.";
    }

    protected String getActionSuffix() {
        return "Action";
    }

    protected String getAssistSuffix() {
        return "Assist";
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    public LastaPresentsSomethingPolice formImmutable() {
        _formImmutable = true;
        return this;
    }
}
