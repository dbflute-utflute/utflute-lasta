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
package org.dbflute.utflute.lastaflute.mock;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.mail.PostedMailCounter;
import org.lastaflute.core.mail.RequestedMailCount;

/**
 * @author jflute
 */
public class MockRequestedMailHandler {

    public OptionalThing<RequestedMailCount> findMailCount() {
        final PostedMailCounter counter = ThreadCacheContext.findMailCounter();
        return OptionalThing.ofNullable(counter != null ? new RequestedMailCount(counter) : null, () -> {
            throw new IllegalStateException("Not found the mail counter on the thread.");
        });
    }
}
