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
package org.dbflute.utflute.lastadi.bean;

import org.dbflute.utflute.lastadi.ContainerTestCase;
import org.dbflute.utflute.lastadi.dbflute.exbhv.FooBhv;

/**
 * @author jflute
 * @since 0.1.0 (2011/07/24 Sunday)
 */
public class FooActionTest extends ContainerTestCase {

    public void test_inject_basic() throws Exception {
        // ## Arrange ##
        FooAction action = new FooAction();

        // ## Act ##
        inject(action);

        // ## Assert ##
        log(action.fooBhv);
        log(action.fooController);
        log(action.fooHelper);
        log(action.fooLogic);
        log(action.fooService);
        log(action.transactionManager);

        assertNotNull(action.fooBhv);
        assertNotNull(action.fooBhv.getTransactionManager());
        assertNull(action.fooController);
        assertNotNull(action.barController);
        assertNotNull(action.fooHelper);
        assertNotNull(action.facadeInstance());
        assertNotNull(action.facadeInstance().myBehaviorInstance());
        assertNotNull(action.facadeInstance().superBehaviorInstance()); // can inject it
        assertNull(action.facadeInstance().fooService);
        assertNotNull(action.fooLogic);
        assertNotNull(action.fooLogic.behaviorToString());
        assertNotNull(action.fooLogic.fooHelper);
        assertNotNull(action.fooLogic.fooService); // can inject under org.dbflute
        assertNotNull(action.fooLogic.getTransactionManager());
        assertNull(action.fooService);
        assertNotNull(action.transactionManager);
    }

    public void test_inject_mockInstance_injected() throws Exception {
        // ## Arrange ##
        FooAction action = new FooAction();
        FooBhv bhv = new FooBhv();
        FooController controller = new FooControllerImpl();
        FooLogic logic = new FooLogic();
        inject(bhv);
        inject(controller);
        inject(logic);
        registerMockInstance(bhv);
        registerMockInstance(controller);
        registerMockInstance(logic);

        // ## Act ##
        inject(action);

        // ## Assert ##
        log(action.fooBhv);
        log(action.transactionManager);
        assertNotNull(action.fooBhv);
        assertNotNull(action.fooBhv.getTransactionManager());
        assertNull(action.fooController);
        assertNotNull(action.barController);
        assertNotNull(action.barController.facadeInstance());
        assertNotNull(action.fooLogic);
        assertNotNull(action.fooLogic.behaviorToString());
        assertNotNull(action.fooLogic.fooHelper);
        assertNotNull(action.fooLogic.fooService);
        assertNotNull(action.fooLogic.getTransactionManager());
        assertNotNull(action.transactionManager);
        assertSame(bhv, action.fooBhv);
        assertSame(logic, action.fooLogic);
    }

    public void test_inject_mockInstance_plain() throws Exception {
        // ## Arrange ##
        FooAction action = new FooAction();
        FooBhv bhv = new FooBhv();
        FooController controller = new FooControllerImpl();
        FooLogic logic = new FooLogic();
        registerMockInstance(bhv);
        registerMockInstance(controller);
        registerMockInstance(logic);

        // ## Act ##
        inject(action);

        // ## Assert ##
        log(action.fooBhv);
        log(action.transactionManager);
        assertNotNull(action.fooBhv);
        assertNull(action.fooBhv.getTransactionManager());
        assertNull(action.fooController);
        assertNotNull(action.barController);
        assertNull(action.barController.facadeInstance());
        assertNotNull(action.fooLogic);
        assertNull(action.fooLogic.behaviorToString());
        assertNull(action.fooLogic.fooHelper);
        assertNull(action.fooLogic.fooService);
        assertNull(action.fooLogic.getTransactionManager());
        assertNotNull(action.transactionManager);
        assertSame(bhv, action.fooBhv);
        assertSame(logic, action.fooLogic);
    }

    public void test_inject_superClass_injected() throws Exception {
        // ## Arrange ##
        FooFacade facade = new FooFacade();

        // ## Act ##
        inject(facade);

        // ## Assert ##
        assertNotNull(facade.myBehaviorInstance());
        assertNotNull(facade.superBehaviorInstance()); // different
        assertNull(facade.transactionManager);
        assertNull(facade.fooService);
    }
}
