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
package org.dbflute.utflute.lastaflute.mock;

import java.util.Map;
import java.util.function.Consumer;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.response.next.RoutingNext;
import org.lastaflute.web.response.render.RenderData;
import org.lastaflute.web.ruts.process.validatebean.ResponseHtmlBeanValidator;
import org.lastaflute.web.ruts.process.validatebean.ResponseJsonBeanValidator;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 */
public class MockResopnseBeanValidator {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RequestManager _requestManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MockResopnseBeanValidator(RequestManager requestManager) {
        _requestManager = requestManager;
    }

    // ===================================================================================
    //                                                                           HTML Data
    //                                                                           =========
    public TestingHtmlData validateHtmlData(HtmlResponse response) {
        final RoutingNext nextRouting = response.getNextRouting();
        final Map<String, Object> dataMap = evaluateDataMap(response);
        final OptionalThing<Object> pushedForm = evaluatePushedForm(response);
        return createTestingHtmlData(nextRouting, dataMap, pushedForm);
    }

    protected Map<String, Object> evaluateDataMap(HtmlResponse response) {
        final RenderData data = new RenderData();
        response.getRegistrationList().forEach(reg -> reg.register(data));
        final Map<String, Object> dataMap = data.getDataMap();
        final ResponseHtmlBeanValidator validator = createResponseHtmlBeanValidator(response);
        dataMap.forEach((key, value) -> validator.validate(key, value));
        return dataMap;
    }

    @SuppressWarnings("unchecked") // method level because of eclipse confusion
    protected OptionalThing<Object> evaluatePushedForm(HtmlResponse response) {
        final OptionalThing<Object> formOpt = response.getPushedFormInfo().flatMap(formInfo -> {
            return formInfo.getFormOption().map(formOption -> {
                final Object form = DfReflectionUtil.newInstance(formInfo.getFormType());
                final Consumer<Object> formSetupper = (Consumer<Object>) formOption.getFormSetupper();
                if (formSetupper != null) {
                    formSetupper.accept(form);
                }
                return form;
            });
        });
        return formOpt;
    }

    protected ResponseHtmlBeanValidator createResponseHtmlBeanValidator(HtmlResponse response) {
        return new ResponseHtmlBeanValidator(_requestManager, this, false, response);
    }

    protected TestingHtmlData createTestingHtmlData(RoutingNext nextRouting, Map<String, Object> dataMap,
            OptionalThing<Object> pushedForm) {
        return new TestingHtmlData(nextRouting, dataMap, pushedForm);
    }

    // ===================================================================================
    //                                                                           JSON Data
    //                                                                           =========
    public <RESULT> TestingJsonData<RESULT> validateJsonData(JsonResponse<RESULT> response) {
        final RESULT jsonResult = response.getJsonBean(); // #future change to getJsonResult()
        createResponseJsonBeanValidator(response).validate(jsonResult);
        return createTestingJsonResult(jsonResult);
    }

    protected <BEAN> ResponseJsonBeanValidator createResponseJsonBeanValidator(JsonResponse<BEAN> response) {
        return new ResponseJsonBeanValidator(_requestManager, this, false, response);
    }

    protected <RESULT> TestingJsonData<RESULT> createTestingJsonResult(RESULT jsonResult) {
        return new TestingJsonData<RESULT>(jsonResult);
    }
}
