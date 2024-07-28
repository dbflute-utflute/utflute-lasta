/*
 * Copyright 2014-2024 the original author or authors.
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
import org.dbflute.utflute.core.filesystem.FilesystemPlayer;
import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassHandler;
import org.dbflute.util.Srl;
import org.lastaflute.web.servlet.cookie.CookieManager;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.servlet.session.SessionManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * @author jflute
 * @since 0.6.0B (2015/12/27 Sunday)
 */
public class NonWebHasWebReferencePolice implements PoliceStoryJavaClassHandler {

    public void handle(File srcFile, Class<?> clazz) {
        check(srcFile, clazz, getLogicKeyword());
        check(srcFile, clazz, getJobKeyword());
    }

    protected String getLogicKeyword() {
        return ".app.logic.";
    }

    protected String getJobKeyword() {
        return ".app.job.";
    }

    protected void check(File srcFile, Class<?> clazz, String packageKeyword) {
        if (!clazz.getName().contains(packageKeyword)) {
            return;
        }
        // checked when also creator process so only small check here
        doCheck(srcFile, clazz, getAppWebPackageKeyword());
        doCheck(srcFile, clazz, getBizfwWebPackageKeyword());
        doCheck(srcFile, clazz, getMylastaWebPackageKeyword());
        doCheck(srcFile, clazz, getWebClsPackageKeyword());
    }

    protected String getAppWebPackageKeyword() {
        return ".app.web.";
    }

    protected String getBizfwWebPackageKeyword() {
        return ".bizfw.web.";
    }

    protected String getMylastaWebPackageKeyword() {
        return ".mylasta.web.";
    }

    protected String getWebClsPackageKeyword() {
        return ".mylasta.webcls.";
    }

    protected void doCheck(File srcFile, Class<?> clazz, final String webPackageKeyword) {
        new FilesystemPlayer().readLine(srcFile, "UTF-8", line -> {
            if (line.startsWith("import ")) {
                final String imported = extractImported(line);
                if (imported.contains(webPackageKeyword)) {
                    throwNonWebHasWebReferenceException(clazz, imported);
                }
                if (isWebComponent(imported)) {
                    throwNonWebHasWebReferenceException(clazz, imported);
                }
            }
        });
    }

    protected String extractImported(String line) {
        return Srl.substringFirstFront(Srl.ltrim(Srl.substringFirstRear(line, "import "), "static "), ";");
    }

    protected boolean isWebComponent(String imported) {
        return Srl.equalsPlain(imported // is class name
                , RequestManager.class.getName() // lastaflute request
                , ResponseManager.class.getName() // lastaflute response
                , SessionManager.class.getName() // lastaflute session
                , CookieManager.class.getName() // lastaflute cookie
                , HttpServletRequest.class.getName() // servlet request
                , HttpServletResponse.class.getName() // servlet response
                , HttpSession.class.getName() // servlet session
                );
    }

    protected void throwNonWebHasWebReferenceException(Class<?> componentType, Object target) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Web reference from the non-web object.");
        br.addItem("Advice");
        br.addElement("Non-web object should not refer web resources,");
        br.addElement(" e.g. classes under 'app.web' package, RequestManager.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public class SeaLogic {");
        br.addElement("        @Resource");
        br.addElement("        private RequestManager requestManager; // *Bad");
        br.addElement("");
        br.addElement("        public void land(SeaForm form) { // *Bad");
        br.addElement("            ...");
        br.addElement("        }");
        br.addElement("    }");
        br.addItem("Non-Web Object");
        br.addElement(componentType);
        br.addItem("Web Reference");
        br.addElement(target);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }
}
