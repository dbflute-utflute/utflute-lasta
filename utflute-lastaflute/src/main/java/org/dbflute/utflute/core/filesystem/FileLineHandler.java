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
package org.dbflute.utflute.core.filesystem;

/**
 * The handler of line string read from text file.
 * @author jflute
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public interface FileLineHandler {

    /**
     * Handle one line read from the text file.
     * @param line The string of line in the text file. (NotNull)
     */
    void handle(String line);
}
