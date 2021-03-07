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
package org.dbflute.utflute.lastaflute.inject;

import org.dbflute.utflute.lastadi.LastaDiTestCase;
import org.dbflute.utflute.lastaflute.bean.FooAction;
import org.dbflute.utflute.lastaflute.bean.FooAssist;
import org.dbflute.utflute.lastaflute.bean.FooBhv;
import org.dbflute.utflute.lastaflute.bean.FooLogic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author jflute
 * @since 0.7.4 (2017/05/01 Monday at rainbow bird rendezvous)
 */
public class RegisterMockTest extends LastaDiTestCase {

    // ===================================================================================
    //                                                                        Nested Level
    //                                                                        ============
    @Test
    public void test_registerMock_firstLevel() {
        // ## Arrange ##
        registerMock(new FooAssist() {
            @Override
            public String callAssist() {
                log("mock here");
                markHere("mock");
                return "sea";
            }
        });
        FooAction action = new FooAction();
        inject(action);

        // ## Act ##
        String result = action.index();

        // ## Assert ##
        Assertions.assertEquals("sea", result);
        assertMarked("mock");
    }

    @Test
    public void test_registerMock_secondLevel() {
        // ## Arrange ##
        registerMock(new FooLogic() {
            @Override
            public String callLogic() {
                log("mock here");
                markHere("mock");
                return "sea";
            }
        });
        FooAction action = new FooAction();
        inject(action);

        // ## Act ##
        String result = action.index();

        // ## Assert ##
        Assertions.assertEquals("sea", result);
        assertMarked("mock");
    }

    @Test
    public void test_registerMock_thirdLevel() {
        // ## Arrange ##
        registerMock(new FooBhv() {
            @Override
            public String callBhv() {
                Assertions.fail();
                return "sea";
            }
        });
        FooAction action = new FooAction();
        inject(action);

        // ## Act ##
        String result = action.index();

        // ## Assert ##
        Assertions.assertEquals("maihama", result); // cannot yet
    }
}
