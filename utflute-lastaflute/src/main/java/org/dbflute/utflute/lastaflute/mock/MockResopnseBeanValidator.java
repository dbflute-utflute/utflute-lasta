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
package org.dbflute.utflute.lastaflute.mock;

import java.util.Map;

import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.response.render.RenderData;
import org.lastaflute.web.ruts.process.validatebean.ResponseHtmlBeanValidator;
import org.lastaflute.web.ruts.process.validatebean.ResponseJsonBeanValidator;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 */
public class MockResopnseBeanValidator {

    protected final RequestManager requestManager;

    public MockResopnseBeanValidator(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    public TestingHtmlData validateHtmlData(HtmlResponse response) {
        final RenderData data = new RenderData();
        response.getRegistrationList().forEach(reg -> reg.register(data));
        final Map<String, Object> dataMap = data.getDataMap();
        final ResponseHtmlBeanValidator validator = createResponseHtmlBeanValidator(response);
        dataMap.forEach((key, value) -> validator.validate(key, value));
        return createTestingHtmlData(dataMap);
    }

    protected ResponseHtmlBeanValidator createResponseHtmlBeanValidator(HtmlResponse response) {
        return new ResponseHtmlBeanValidator(requestManager, this, false, response);
    }

    protected TestingHtmlData createTestingHtmlData(Map<String, Object> dataMap) {
        return new TestingHtmlData(dataMap);
    }

    public <RESULT> TestingJsonData<RESULT> validateJsonBean(JsonResponse<RESULT> response) {
        final RESULT jsonResult = response.getJsonBean(); // #future change to getJsonResult()
        createResponseJsonBeanValidator(response).validate(jsonResult);
        return createTestingJsonResult(jsonResult);
    }

    protected <BEAN> ResponseJsonBeanValidator createResponseJsonBeanValidator(JsonResponse<BEAN> response) {
        return new ResponseJsonBeanValidator(requestManager, this, false, response);
    }

    protected <RESULT> TestingJsonData<RESULT> createTestingJsonResult(RESULT jsonResult) {
        return new TestingJsonData<RESULT>(jsonResult);
    }
}
