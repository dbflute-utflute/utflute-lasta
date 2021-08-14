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
package org.dbflute.utflute.mocklet;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.WriteListener;

/**
 * @author modified by jflute (originated in Seasar)
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class MockletServletOutputStreamImpl extends MockletServletOutputStream {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final OutputStream outputStream;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MockletServletOutputStreamImpl(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    // ===================================================================================
    //                                                                               Write
    //                                                                               =====
    public void write(int b) throws IOException {
        outputStream.write(b);
    }

    // ===================================================================================
    //                                                                         since 3.1.0
    //                                                                         ===========
    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
    }
}
