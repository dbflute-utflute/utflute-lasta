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
package org.dbflute.utflute.lasta;

import org.dbflute.utflute.lasta.LastaDiTestCase;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public class LastaDiTestCaseTest extends LastaDiTestCase {

    public void test_xcanUseComponentNameByBindingNamingRule_basic() throws Exception {
        assertTrue(xcanUseComponentNameByBindingNamingRule("foo_bar", "bar"));
        assertTrue(xcanUseComponentNameByBindingNamingRule("foo_bar", "foo_bar"));
        assertTrue(xcanUseComponentNameByBindingNamingRule("foo_bar_qux", "qux"));
        assertTrue(xcanUseComponentNameByBindingNamingRule("foo_bar_qux", "bar_qux"));
        assertTrue(xcanUseComponentNameByBindingNamingRule("foo_bar_qux", "foo_bar_qux"));
        assertTrue(xcanUseComponentNameByBindingNamingRule("foo_bar_quxLogic", "quxLogic"));
        assertTrue(xcanUseComponentNameByBindingNamingRule("foo_bar_quxLogic", "bar_quxLogic"));
        assertTrue(xcanUseComponentNameByBindingNamingRule("foo_bar_quxLogic", "foo_bar_quxLogic"));

        assertFalse(xcanUseComponentNameByBindingNamingRule("bar", "bar")); // not smart deploy component
        assertFalse(xcanUseComponentNameByBindingNamingRule("foo_bar_quxLogic", "Logic"));
        assertFalse(xcanUseComponentNameByBindingNamingRule("foo_bar_quxLogic", "uxLogic"));
        assertFalse(xcanUseComponentNameByBindingNamingRule("foo_bar_quxLogic", "ar_quxLogic"));
        assertFalse(xcanUseComponentNameByBindingNamingRule("foo_bar_quxLogic", "oo_bar_quxLogic"));
        assertFalse(xcanUseComponentNameByBindingNamingRule("foo_bar_quxLogic", "_quxLogic"));
    }
}
