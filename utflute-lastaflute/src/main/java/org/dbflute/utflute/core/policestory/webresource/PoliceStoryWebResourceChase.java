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
package org.dbflute.utflute.core.policestory.webresource;

import java.io.File;

import org.dbflute.utflute.core.policestory.miscfile.PoliceStoryMiscFileChase;
import org.dbflute.utflute.core.policestory.miscfile.PoliceStoryMiscFileHandler;

/**
 * @author jflute
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class PoliceStoryWebResourceChase {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Object _testCase;
    protected final File _webappDir;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public PoliceStoryWebResourceChase(Object testCase, File webappDir) {
        _testCase = testCase;
        _webappDir = webappDir;
    }

    // ===================================================================================
    //                                                                               Chase
    //                                                                               =====
    public void chaseWebResource(final PoliceStoryWebResourceHandler handler) {
        new PoliceStoryMiscFileChase(_testCase, _webappDir) {
            protected String getChaseFileExt() {
                return null; // means all
            }
        }.chaseMiscFile(new PoliceStoryMiscFileHandler() {
            public void handle(File textFile) {
                handler.handle(textFile);
            }
        });
    }
}
