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
package org.dbflute.utflute.lasta.bean;

import javax.annotation.Resource;
import javax.transaction.TransactionManager;

import org.dbflute.lasta.di.core.annotation.Binding;
import org.dbflute.lasta.di.core.annotation.BindingType;
import org.dbflute.utflute.lasta.dbflute.exbhv.FooBhv;

/**
 * @author jflute
 * @since 0.1.0 (2011/07/24 Sunday)
 */
public class FooAction {

    @Resource
    protected FooBhv fooBhv; // standard annotation

    protected FooController fooController; // no annotation, no setter

    @Resource
    protected FooController barController; // name wrong: but interface so injected

    @Resource
    protected FooFacade fooHelper; // name wrong: concrete so not injected

    @Resource
    private FooFacade fooFacade; // private field

    @Binding
    protected FooLogic fooLogic; // container annotation

    @Binding(bindingType = BindingType.NONE)
    protected FooService fooService; // specify none

    protected TransactionManager transactionManager; // setter only no annotation

    public FooFacade facadeInstance() {
        return fooFacade;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
