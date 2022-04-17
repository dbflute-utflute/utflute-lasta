/*
 * Copyright 2014-2022 the original author or authors.
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
package org.dbflute.utflute.lastaflute.inject;

import javax.annotation.Resource;

import org.dbflute.utflute.lastadi.LastaDiTestCase;
import org.dbflute.utflute.lastaflute.bean.FooAction;

/**
 * @author jflute
 * @since 0.7.4 (2017/05/01 Monday at rainbow bird rendezvous)
 */
public class InjectTest extends LastaDiTestCase {

    // ===================================================================================
    //                                                                               Basic
    //                                                                               =====
    public void test_inject_basic() {
        // ## Arrange ##
        FooAction action = new FooAction();
        inject(action);

        // ## Act ##
        String result = action.index();

        // ## Assert ##
        assertEquals("maihama", result);
    }

    // ===================================================================================
    //                                                                           Not Found
    //                                                                           =========
    public void test_inject_notFound() {
        // ## Arrange ##
        MockBrokenLogic logic = new MockBrokenLogic();
        inject(logic);

        // ## Act ##
        // ## Assert ##
        assertException(NullPointerException.class, () -> logic.callLogic());
    }

    public static class MockBrokenLogic {

        @Resource
        private MockNotFoundLogic notFoundLogic;

        public void callLogic() {
            notFoundLogic.toString();
        }
    }

    public static class MockNotFoundLogic {
    }
}
