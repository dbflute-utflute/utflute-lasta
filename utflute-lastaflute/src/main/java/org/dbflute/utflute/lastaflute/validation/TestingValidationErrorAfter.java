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
package org.dbflute.utflute.lastaflute.validation;

import java.util.function.Consumer;

import org.lastaflute.core.message.MessageManager;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.validation.exception.ValidationErrorException;

/**
 * @author jflute
 * @since 0.7.2 (2017/04/08 Saturday)
 */
public class TestingValidationErrorAfter {

    protected final ValidationErrorException _cause; // not null
    protected final MessageManager _messageManager;
    protected final RequestManager _requestManager;

    public TestingValidationErrorAfter(ValidationErrorException cause, MessageManager messageManager, RequestManager requestManager) {
        _cause = cause;
        _messageManager = messageManager;
        _requestManager = requestManager;
    }

    /**
     * Handle the expected cause to assert.
     * <pre>
     * <span style="color: #3F7E5E">// ## Act ##</span>
     * assertValidationError(() -&gt; <span style="color: #553000">action</span>.index(<span style="color: #553000">form</span>)).<span style="color: #CC4747">handle</span>(<span style="color: #553000">data</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #3F7E5E">// ## Assert ##</span>
     *     <span style="color: #553000">data</span>.<span style="color: #994747">requiredMessageOf</span>("sea", Required.class);
     * });
     * </pre>
     * @param dataLambda The callback for handling of validation error data. (NotNull)
     */
    public void handle(Consumer<TestingValidationData> dataLambda) {
        final TestingValidationData data = new TestingValidationData(_cause, _messageManager, _requestManager);
        dataLambda.accept(data);
    }

}
