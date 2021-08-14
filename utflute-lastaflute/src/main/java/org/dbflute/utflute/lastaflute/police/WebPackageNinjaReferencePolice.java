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
import java.util.List;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.utflute.core.filesystem.FilesystemPlayer;
import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassHandler;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.Srl;

/**
 * @author jflute
 * @since 0.6.0B (2015/12/27 Sunday)
 */
public class WebPackageNinjaReferencePolice implements PoliceStoryJavaClassHandler {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final List<String> parentChildSharedPackageList; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public WebPackageNinjaReferencePolice() {
        final List<String> sharedPackageList = prepareParentChildSharedPackageList();
        if (sharedPackageList == null) {
            throw new IllegalStateException("The sharedPackageList should not be null.");
        }
        parentChildSharedPackageList = sharedPackageList;
    }

    protected List<String> prepareParentChildSharedPackageList() { // you can override
        return DfCollectionUtil.newArrayList("assist"); // as default
    }

    // ===================================================================================
    //                                                                              Handle
    //                                                                              ======
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

    // ===================================================================================
    //                                                                       Checking Base
    //                                                                       =============
    protected void check(File srcFile, Class<?> clazz, String webPackageKeyword) {
        final String myRearName = deriveMyRearName(clazz, webPackageKeyword); // sea.land.SeaLandAction, RootAction
        new FilesystemPlayer().readLine(srcFile, "UTF-8", line -> {
            if (line.startsWith("import ") && !line.startsWith("import static ")) { /* static has difficult pattern */
                final String imported = extractImported(line);
                if (imported.contains(webPackageKeyword)) { // importing app.web class
                    final String rearImported = Srl.substringFirstRear(imported, webPackageKeyword);
                    if (existsNinjaReference(clazz, myRearName, rearImported)) {
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

    // ===================================================================================
    //                                                                 NINJA Determination
    //                                                                 ===================
    protected boolean existsNinjaReference(Class<?> clazz, String myRearName, String rearImported) {
        // e.g. myRearName:
        //  o sea.land.SeaLandAction
        //  o RootAction
        //
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
            if (isFromRootClass(myRearName, rearImported)) { // myRearName is e.g. RootAction
                return false; // allowed (root classes can sub package classes)
            }
            if (isReferingToAction(myRearName, rearImported)) { // allowed (e.g. from redirect(), from loginAssist)
                return false;
            }
            if (isReferingToCommonPackage(myRearName, rearImported)) { // e.g. base.HarborBaseAction, common...
                return false;
            }
            // here e.g. sea.SeaLandAction, sea.land.SeaLandAction
            final String myRelativePackage = Srl.substringLastFront(myRearName, "."); // e.g. sea.land (if sea.land.SeaLandAction)
            final String yourRelativePackage = extractYourRelativePackage(rearImported);
            if (hasParentChildRelationship(myRelativePackage, yourRelativePackage)) {
                return false; // allowed (same or sub or parent package) e.g. a.b => a.b.c, a.b.c => a.b
            }
            return true;
        } else { // refers to e.g. RootAction, *
            return false; // however, reference to root action is also...
        }
    }

    protected boolean isFromRootClass(String myRearName, String rearImported) {
        return !myRearName.contains(".");
    }

    protected boolean isReferingToAction(String myRearName, String rearImported) {
        return rearImported.endsWith("Action");
    }

    protected boolean isReferingToCommonPackage(String myRearName, String rearImported) {
        final String firstPurePackage = Srl.substringFirstFront(rearImported, "."); // e.g. base, sea
        return firstPurePackage.equals("base") || firstPurePackage.equals("common"); // allowed (base is common package)
    }

    protected String extractYourRelativePackage(String rearImported) {
        String yourRelativePackage = Srl.substringLastFront(rearImported, "."); // e.g. sea, sea.land, sea.SeaBean
        if (yourRelativePackage.contains(".")) { // e.g. sea.land, sea.SeaBean
            if (Srl.isInitUpperCase(Srl.substringLastRear(yourRelativePackage, "."))) { // e.g. sea.SeaBean
                yourRelativePackage = Srl.substringLastFront(yourRelativePackage, "."); // for inner class reference
            }
        }
        return yourRelativePackage;
    }

    protected boolean hasParentChildRelationship(String myRelativePackage, String yourRelativePackage) {
        if (judgeParentChildPackage(myRelativePackage, yourRelativePackage)) {
            return true;
        }
        // different package line here, but allowed if shared package (basically for e.g. RESTful)
        if (!parentChildSharedPackageList.isEmpty()) { // e.g. assist
            for (String sharedPackage : parentChildSharedPackageList) {
                final String sharedSuffix = "." + sharedPackage;
                if (myRelativePackage.endsWith(sharedSuffix) && yourRelativePackage.endsWith(sharedSuffix)) {
                    final String myRemoved = Srl.substringLastFront(myRelativePackage, sharedSuffix);
                    final String yourRemoved = Srl.substringLastFront(yourRelativePackage, sharedSuffix);
                    if (judgeParentChildPackage(myRemoved, yourRemoved)) {
                        return true; // e.g. sea.land.assist.SeaLandAssist refers sea.assist.SeaAssist
                    }
                }
            }
        }
        return false;
    }

    protected boolean judgeParentChildPackage(String myRelativePackage, String yourRelativePackage) {
        return myRelativePackage.startsWith(yourRelativePackage) || yourRelativePackage.startsWith(myRelativePackage);
    }

    protected void throwWebPackageNinjaReferenceException(Class<?> targetType, Object reference) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Ninja reference in web package.");
        br.addItem("Advice");
        br.addElement("Web resources cannot refer other business web resources");
        br.addElement("For example: (under app.web package)");
        br.addElement("  (x):");
        br.addElement("    land.LandAction refers sea.SeaForm");
        br.addElement("  (o):");
        br.addElement("    land.LandAction refers land.LandForm");
        br.addElement("    land.LandAction refers base.sea.SeaForm");
        br.addElement("    land.LandAction refers common.sea.SeaForm");
        setupParentChildSharedMessage(br);
        br.addItem("Source Type");
        br.addElement(targetType.getName());
        br.addItem("Ninja Reference");
        br.addElement(reference);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }

    protected void setupParentChildSharedMessage(ExceptionMessageBuilder br) {
        br.addElement("");
        br.addElement("While, shared packages e.g. 'assist' on parent-child relationship");
        br.addElement("can be shared each other.");
        br.addElement("You can customize it by overridding the police class.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    sea.land.SeaLandAssist refers sea.assist.SeaAssist");
        br.addElement("    sea.assist.SeaAssist refers sea.land.SeaLandAssist");
        br.addElement("  (o):");
        br.addElement("    sea.land.assist.SeaLandAssist refers sea.assist.SeaAssist");
        br.addElement("    sea.assist.SeaAssist refers sea.land.assist.SeaLandAssist");
    }
}
