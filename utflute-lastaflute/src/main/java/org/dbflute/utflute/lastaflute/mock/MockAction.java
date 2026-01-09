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

import org.dbflute.hook.AccessContext;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.db.dbflute.accesscontext.AccessContextArranger;
import org.lastaflute.web.Execute;
import org.lastaflute.web.TypicalAction;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.login.UserBean;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;

/**
 * @author jflute
 */
public class MockAction extends TypicalAction {

    // ===================================================================================
    //                                                                           Framework
    //                                                                           =========
    @Override
    protected AccessContextArranger newAccessContextArranger() {
        return resource -> new AccessContext();
    }

    @Override
    protected OptionalThing<? extends UserBean<?>> getUserBean() {
        return OptionalThing.empty();
    }

    @Override
    protected String myAppType() {
        return "MCK";
    }

    @Override
    protected OptionalThing<String> myUserType() {
        return OptionalThing.empty();
    }

    @Override
    protected OptionalThing<LoginManager> myLoginManager() {
        return OptionalThing.empty();
    }

    // ===================================================================================
    //                                                                             Execute
    //                                                                             =======
    @Execute
    public HtmlResponse sea() {
        return HtmlResponse.fromForwardPath("/");
    }

    @Execute
    public JsonResponse<Object> land() {
        return asJson(new Object());
    }
}
