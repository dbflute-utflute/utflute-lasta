/*
 * Copyright 2014-2022 the original author or authors.
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
package org.dbflute.utflute.core.policestory.javaclass;

import java.io.File;
import java.io.IOException;

import org.dbflute.helper.filesystem.FileHierarchyTracer;
import org.dbflute.helper.filesystem.FileHierarchyTracingHandler;
import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.Srl;

/**
 * @author jflute
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class PoliceStoryJavaClassChase {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Object _testCase;
    protected final File _javaSrcDir;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public PoliceStoryJavaClassChase(Object testCase, File javaSrcDir) {
        _testCase = testCase;
        _javaSrcDir = javaSrcDir;
    }

    // ===================================================================================
    //                                                                               Chase
    //                                                                               =====
    public void chaseJavaClass(PoliceStoryJavaClassHandler handler) {
        try {
            doChase(_javaSrcDir, handler);
        } catch (RuntimeException e) {
            throwPoliceStoryOfJavaClassChaseFailureException(_javaSrcDir, e);
        }
    }

    protected void throwPoliceStoryOfJavaClassChaseFailureException(File srcDir, Exception cause) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to chase Java class.");
        br.addItem("Test Case");
        br.addElement(_testCase);
        br.addItem("Source Directory");
        br.addElement(srcDir);
        br.addItem("Cause");
        br.addElement(cause.getClass());
        br.addElement("See the cause exception message at 'Caused by'.");
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg, cause);
    }

    protected void doChase(final File srcDir, final PoliceStoryJavaClassHandler handler) {
        if (!srcDir.exists()) {
            String msg = "The source directory does not exist: " + srcDir;
            throw new IllegalStateException(msg);
        }
        createFileHierarchyTracer().trace(srcDir, new FileHierarchyTracingHandler() {
            public boolean isTargetFileOrDir(File currentFile) {
                if (currentFile.isDirectory()) {
                    return true;
                }
                final String fileName = currentFile.getName();
                if ("package-info.java".equals(fileName)) {
                    return false; // except it
                }
                return fileName.endsWith(".java");
            }

            public void handleFile(File currentFile) throws IOException {
                handler.handle(currentFile, analyzeClass(srcDir, currentFile));
            }
        });
    }

    protected FileHierarchyTracer createFileHierarchyTracer() {
        return new FileHierarchyTracer();
    }

    protected Class<?> analyzeClass(File srcDir, File currentFile) throws IOException {
        final String srcCanoPath = Srl.replace(srcDir.getCanonicalPath(), "\\", "/");
        final String fileCanoPath = Srl.replace(currentFile.getCanonicalPath(), "\\", "/");
        final String packageFile = Srl.substringFirstRear(fileCanoPath, srcCanoPath);
        final String packageExp = Srl.trim(Srl.replace(Srl.substringLastFront(packageFile, "/"), "/", "."), ".");
        final String simpleName = Srl.substringLastFront(currentFile.getName(), ".java");
        final String className = packageExp + "." + simpleName;
        return DfReflectionUtil.forName(className);
    }
}
