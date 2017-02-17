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
package org.dbflute.utflute.core.policestory;

import java.io.File;
import java.io.IOException;

import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassChase;
import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassHandler;
import org.dbflute.utflute.core.policestory.jspfile.PoliceStoryJspFileChase;
import org.dbflute.utflute.core.policestory.jspfile.PoliceStoryJspFileHandler;
import org.dbflute.utflute.core.policestory.miscfile.PoliceStoryMiscFileChase;
import org.dbflute.utflute.core.policestory.miscfile.PoliceStoryMiscFileHandler;
import org.dbflute.utflute.core.policestory.pjresource.PoliceStoryProjectResourceChase;
import org.dbflute.utflute.core.policestory.pjresource.PoliceStoryProjectResourceHandler;
import org.dbflute.utflute.core.policestory.webresource.PoliceStoryWebResourceChase;
import org.dbflute.utflute.core.policestory.webresource.PoliceStoryWebResourceHandler;

/**
 * @author jflute
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class PoliceStory {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Object _testCase;
    protected final File _projectDir;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public PoliceStory(Object testCase, File projectDir) {
        _testCase = testCase;
        _projectDir = projectDir;
    }

    // ===================================================================================
    //                                                                               Chase
    //                                                                               =====
    public void chaseJavaClass(PoliceStoryJavaClassHandler handler) {
        createJavaClassChase(getSrcMainJavaDir()).chaseJavaClass(handler);
        if (handler.containsTestClass()) { // simple way #for_now
            createJavaClassChase(getSrcTestJavaDir()).chaseJavaClass(handler);
        }
    }

    public void chaseJspFile(PoliceStoryJspFileHandler handler) {
        createJspFileChase(getWebappDir()).chaseJspFile(handler);
    }

    public void chaseMiscFile(PoliceStoryMiscFileHandler handler, File miscDir) {
        createMiscFileChase(miscDir).chaseMiscFile(handler);
    }

    public void chaseProjectResource(PoliceStoryProjectResourceHandler handler) {
        createProjectResourceChase(getProjectDir()).chaseProjectResource(handler);
    }

    public void chaseWebResource(PoliceStoryWebResourceHandler handler) {
        createWebResourceChase(getWebappDir()).chaseWebResource(handler);
    }

    // ===================================================================================
    //                                                                             Factory
    //                                                                             =======
    protected PoliceStoryJavaClassChase createJavaClassChase(File javaSourceDir) {
        return new PoliceStoryJavaClassChase(_testCase, javaSourceDir);
    }

    protected PoliceStoryJspFileChase createJspFileChase(File jspDir) {
        return new PoliceStoryJspFileChase(_testCase, jspDir);
    }

    protected PoliceStoryMiscFileChase createMiscFileChase(File baseDir) {
        return new PoliceStoryMiscFileChase(_testCase, baseDir);
    }

    protected PoliceStoryProjectResourceChase createProjectResourceChase(File projectDir) {
        return new PoliceStoryProjectResourceChase(_testCase, projectDir);
    }

    protected PoliceStoryWebResourceChase createWebResourceChase(File webappDir) {
        return new PoliceStoryWebResourceChase(_testCase, webappDir);
    }

    // ===================================================================================
    //                                                                         File System
    //                                                                         ===========

    protected File getSrcMainJavaDir() {
        return new File(getProjectPath() + "/src/main/java/");
    }

    protected File getSrcMainResourcesDir() {
        return new File(getProjectPath() + "/src/main/resources/");
    }

    protected File getSrcTestJavaDir() {
        return new File(getProjectPath() + "/src/test/java/");
    }

    protected File getSrcTestResourcesDir() {
        return new File(getProjectPath() + "/src/test/resources/");
    }

    protected File getWebappDir() {
        return new File(getProjectPath() + "/src/main/webapp/");
    }

    protected File getProjectDir() {
        return _projectDir;
    }

    protected String getProjectPath() {
        final File projectDir = getProjectDir();
        final String projectCanonicalPath;
        try {
            projectCanonicalPath = projectDir.getCanonicalPath();
        } catch (IOException e) {
            String msg = "Failed to get canonical path from: " + projectDir;
            throw new IllegalStateException(msg, e);
        }
        return projectCanonicalPath;
    }
}
