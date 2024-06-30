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
package org.dbflute.utflute.core.exception;

/**
 * @param <CAUSE> The type of cause exception.
 * @author jflute
 * @since 0.6.? (2017/02/11 Saturday)
 */
public class ExceptionExpectationAfter<CAUSE extends Throwable> {

    protected final CAUSE _cause; // not null

    /**
     * @param cause The expected cause. (NotNull)
     */
    public ExceptionExpectationAfter(CAUSE cause) {
        _cause = cause;
    }

    /**
     * Handle the expected cause to assert.
     * @param causeLambda The callback for handling of expected cause. (NotNull)
     */
    public void handle(ExceptionExpectationCall<CAUSE> causeLambda) {
        causeLambda.callback(_cause);
    }
}
