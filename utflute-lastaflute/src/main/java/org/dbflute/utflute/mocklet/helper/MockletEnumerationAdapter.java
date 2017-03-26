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
package org.dbflute.utflute.mocklet.helper;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * @param <ELEMENT> The type of element.
 * @author modified by jflute (originated in Seasar)
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class MockletEnumerationAdapter<ELEMENT> implements Enumeration<ELEMENT> {

    protected final Iterator<ELEMENT> iterator;

    public MockletEnumerationAdapter(Iterator<ELEMENT> iterator) {
        this.iterator = iterator;
    }

    public boolean hasMoreElements() {
        return iterator.hasNext();
    }

    public ELEMENT nextElement() {
        return iterator.next();
    }
}
