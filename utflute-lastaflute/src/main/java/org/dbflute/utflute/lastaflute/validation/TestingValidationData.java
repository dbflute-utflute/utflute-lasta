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

import java.lang.annotation.Annotation;

import org.junit.Assert;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.validation.exception.ValidationErrorException;

/**
 * @author jflute
 * @since 0.7.2 (2017/04/08 Saturday)
 */
public class TestingValidationData {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ValidationErrorException _cause;
    protected final MessageManager _messageManager;
    protected final RequestManager _requestManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TestingValidationData(ValidationErrorException cause, MessageManager messageManager, RequestManager requestManager) {
        _cause = cause;
        _messageManager = messageManager;
        _requestManager = requestManager;
    }

    // ===================================================================================
    //                                                                            Required
    //                                                                            ========
    /**
     * Assert the message is required as validation error.
     * <pre>
     * data.requiredMessageOf("sea", Required.class);
     * </pre>
     * @param property the name of property, which may have user messages. (NotNull)
     * @param annotationType The type of validator annotation to get message. (NotNull)
     */
    public void requiredMessageOf(String property, Class<? extends Annotation> annotationType) {
        if (property == null) {
            throw new IllegalArgumentException("The argument 'property' should not be null.");
        }
        if (annotationType == null) {
            throw new IllegalArgumentException("The argument 'annotationType' should not be null.");
        }
        final String messageKey = toDefinedMessageKey(annotationType);
        final UserMessages messages = _cause.getMessages();
        Assert.assertTrue("No validation error for the message: " + annotationType, hasMessageOf(messages, property, messageKey));
    }

    /**
     * Assert the message is required as validation error.
     * <pre>
     * data.requiredMessageOf("sea", DocksideMessages.ERRORS_...);
     * </pre>
     * @param property the name of property, which may have user messages. (NotNull)
     * @param messageKey The key of message as validation error. (NotNull)
     */
    public void requiredMessageOf(String property, String messageKey) {
        if (property == null) {
            throw new IllegalArgumentException("The argument 'property' should not be null.");
        }
        if (messageKey == null) {
            throw new IllegalArgumentException("The argument 'messageKey' should not be null.");
        }
        final UserMessages messages = _cause.getMessages();
        Assert.assertTrue("No validation error for the message: " + messageKey, hasMessageOf(messages, property, messageKey));
    }

    // ===================================================================================
    //                                                                          Error Hook
    //                                                                          ==========
    /**
     * Hook validation error returning action response.
     * @return The action response for validation error. (NotNull)
     */
    @SuppressWarnings("unchecked")
    public <RESPONSE extends ActionResponse> RESPONSE hookError() { // basically for HTML response
        return (RESPONSE) _cause.getErrorHook().hook();
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected String toDefinedMessageKey(Class<?> annotationType) {
        return "constraints." + annotationType.getSimpleName() + "message";
    }

    /**
     * Do the messages have the property and the message key?
     * <pre>
     * assertTrue(<span style="color: #CC4747">hasMessageOf</span>(<span style="color: #553000">messages</span>, "account", DocksideMessages.CONSTRAINTS_Required_MESSAGE));
     * </pre>
     * @param messages The messages for user as e.g. validation errors. (NotNull)
     * @param property the name of property, which may have user messages. (NotNull)
     * @param messageKey The message key defined in your [app]_message.properties. (NotNull)
     * @return The determination, true or false.
     */
    protected boolean hasMessageOf(UserMessages messages, String property, String messageKey) {
        if (messages.hasMessageOf(property, messageKey)) {
            return true;
        }
        String message = _messageManager.getMessage(_requestManager.getUserLocale(), messageKey);
        return messages.hasMessageOf(property, message);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public ValidationErrorException getCause() {
        return _cause;
    }
}
