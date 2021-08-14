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

/**
 * @author jflute
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public interface PoliceStoryMiscFileHandler {

    /**
     * @param miscFile The file object for the miscellaneous file. (NotNull)
     */
    void handle(File miscFile);
}
