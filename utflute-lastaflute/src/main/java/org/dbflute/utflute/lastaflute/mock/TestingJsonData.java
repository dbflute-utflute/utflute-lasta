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
package org.dbflute.utflute.lastaflute.mock;

/**
 * @param <RESULT> The type of JSON result.
 * @author jflute
 */
public class TestingJsonData<RESULT> {

    protected final RESULT _jsonResult; // not null

    public TestingJsonData(RESULT jsonResult) {
        _jsonResult = jsonResult;
    }

    @Deprecated
    public RESULT getJsonBean() { // for compatible
        return _jsonResult;
    }

    /**
     * Get the result object of JSON.
     * @return The result object of JSON. (NotNull)
     */
    public RESULT getJsonResult() {
        return _jsonResult;
    }
}
