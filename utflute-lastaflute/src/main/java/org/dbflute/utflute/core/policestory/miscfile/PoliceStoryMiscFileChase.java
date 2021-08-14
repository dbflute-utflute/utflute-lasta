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
package org.dbflute.utflute.core.policestory.miscfile;

import java.io.File;
import java.io.IOException;

import org.dbflute.helper.filesystem.FileHierarchyTracer;
import org.dbflute.helper.filesystem.FileHierarchyTracingHandler;
import org.dbflute.helper.message.ExceptionMessageBuilder;

/**
 * @author jflute
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class PoliceStoryMiscFileChase {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Object _testCase;
    protected final File _baseDir;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public PoliceStoryMiscFileChase(Object testCase, File baseDir) {
        _testCase = testCase;
        _baseDir = baseDir;
    }

    // ===================================================================================
    //                                                                               Chase
    //                                                                               =====
    public void chaseMiscFile(PoliceStoryMiscFileHandler handler) {
        try {
            doChase(_baseDir, handler);
        } catch (RuntimeException e) {
            throwPoliceStoryOfTextFileChaseFailureException(_baseDir, e);
        }
    }

    protected void throwPoliceStoryOfTextFileChaseFailureException(File baseDir, Exception cause) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to chase the file.");
        br.addItem("Test Case");
        br.addElement(_testCase);
        br.addItem("Base Directory");
        br.addElement(baseDir);
        br.addItem("Exception");
        br.addElement(cause.getClass());
        br.addElement(cause.getMessage());
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg, cause);
    }

    protected void doChase(final File baseDir, final PoliceStoryMiscFileHandler handler) {
        if (!baseDir.exists()) {
            String msg = "The base directory does not exist: " + baseDir;
            throw new IllegalStateException(msg);
        }
        final String chaseFileExt = getChaseFileExt();
        createFileHierarchyTracer().trace(baseDir, new FileHierarchyTracingHandler() {
            public boolean isTargetFileOrDir(File currentFile) {
                if (currentFile.isDirectory()) {
                    return true;
                }
                if (chaseFileExt != null) {
                    return currentFile.getName().endsWith("." + chaseFileExt);
                }
                return true;
            }

            public void handleFile(File currentFile) throws IOException {
                handler.handle(currentFile);
            }
        });
    }

    protected FileHierarchyTracer createFileHierarchyTracer() {
        return new FileHierarchyTracer();
    }

    protected String getChaseFileExt() { // customize point
        return null; // as default, and not contains dot '.'
    }
}
