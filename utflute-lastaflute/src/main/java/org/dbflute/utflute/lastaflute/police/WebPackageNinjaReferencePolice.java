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
import org.dbflute.utflute.core.filesystem.FilesystemPlayer;
import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassHandler;
import org.dbflute.util.Srl;

/**
 * @author jflute
 * @since 0.6.0B (2015/12/27 Sunday)
 */
public class WebPackageNinjaReferencePolice implements PoliceStoryJavaClassHandler {

    public void handle(File srcFile, Class<?> clazz) {
        final String webPackageKeyword = getWebPackageKeyword();
        if (!clazz.getName().contains(webPackageKeyword)) {
            return;
        }
        check(srcFile, clazz, webPackageKeyword);
    }

    protected String getWebPackageKeyword() {
        return ".app.web.";
    }

    protected void check(File srcFile, Class<?> clazz, String webPackageKeyword) {
        final String myRearName = deriveMyRearName(clazz, webPackageKeyword); // sea.land.SeaLandAction, RootAction
        final String myPackage = Srl.substringFirstFront(myRearName, "."); // e.g. sea (if sea.land.SeaLandAction), RootAction
        final String myRelativePackage = Srl.substringLastFront(myRearName, "."); // e.g. sea.land (if sea.land.SeaLandAction)
        new FilesystemPlayer().readLine(srcFile, "UTF-8", line -> {
            if (line.startsWith("import ") && !line.startsWith("import static ")) { /* static has difficult pattern */
                final String imported = extractImported(line);
                if (imported.contains(webPackageKeyword)) { // importing app.web class
                    final String rearImported = Srl.substringFirstRear(imported, webPackageKeyword);
                    if (existsNinjaReference(clazz, myRearName, myPackage, myRelativePackage, rearImported)) {
                        throwWebPackageNinjaReferenceException(clazz, imported);
                    }
                }
            }
        });
    }

    protected String deriveMyRearName(Class<?> clazz, String webPackageKeyword) {
        return Srl.substringFirstRear(clazz.getName(), webPackageKeyword);
    }

    protected String deriveMyPackage(Class<?> clazz, String webPackageKeyword) {
        return Srl.substringFirstFront(Srl.substringFirstRear(clazz.getName(), webPackageKeyword), ".");
    }

    protected String deriveMyRelativePackage(Class<?> clazz, String webPackageKeyword) {
        return Srl.substringLastFront(Srl.substringFirstRear(clazz.getName(), webPackageKeyword), ".");
    }

    protected String extractImported(String line) {
        return Srl.substringFirstFront(Srl.ltrim(Srl.substringFirstRear(line, "import "), "static "), ";");
    }

    protected boolean existsNinjaReference(Class<?> clazz, String myRearName, String myPackage, String myRelativePackage,
            String rearImported) {
        // e.g. rearImported:
        //  o *
        //  o RootAction
        //  o base.DocksideBaseAction
        //  o base.login.DocksideLoginAssist
        //  o sea.*
        //  o sea.SeaAction
        //  o sea.land.SeaLandAction
        //  o sea.land.SeaLandBean.ElementBean
        if (rearImported.contains(".")) {
            if (rearImported.endsWith("Action")) { // allowed (e.g. from redirect(), from loginAssist)
                return false;
            }
            final String purePackage = Srl.substringFirstFront(rearImported, "."); // e.g. base, sea
            if (isCommonPackage(purePackage)) {
                return false;
            }
            if (myRearName.contains(".")) { // e.g. sea.land.SeaLandAction
                String relativePackage = Srl.substringLastFront(rearImported, "."); // e.g. sea, sea.land, sea.SeaBean
                if (relativePackage.contains(".")) { // e.g. sea.land, sea.SeaBean
                    if (Srl.isInitUpperCase(Srl.substringLastRear(relativePackage, "."))) { // e.g. sea.SeaBean
                        relativePackage = Srl.substringLastFront(relativePackage, "."); // for inner class reference
                    }
                }
                if (relativePackage.startsWith(myRelativePackage) || myRelativePackage.startsWith(relativePackage)) {
                    // allowed (same or sub or parent package) e.g. a.b => a.b.c, a.b.c => a.b
                    return false;
                }
            } else { // myRearName is e.g. RootAction
                return false; // allowed (root classes can sub package classes)
            }
            return true;
        } else { // e.g. RootAction, *
            return false; // however, reference to root action is also...
        }
    }

    protected boolean isCommonPackage(String purePackage) {
        return purePackage.equals("common") || purePackage.equals("base"); // allowed (base is common package)
    }

    protected void throwWebPackageNinjaReferenceException(Class<?> targetType, Object reference) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Ninja reference in web package.");
        br.addItem("Advice");
        br.addElement("Web resources cannot refer other business web resources");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    app.web.land.LandAction refers app.web.sea.SeaForm");
        br.addElement("  (o):");
        br.addElement("    app.web.land.LandAction refers app.web.land.LandForm");
        br.addElement("    app.web.land.LandAction refers app.web.base.sea.SeaForm");
        br.addElement("    app.web.land.LandAction refers app.web.common.sea.SeaForm");
        br.addItem("Source Type");
        br.addElement(targetType.getName());
        br.addItem("Ninja Reference");
        br.addElement(reference);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }
}
