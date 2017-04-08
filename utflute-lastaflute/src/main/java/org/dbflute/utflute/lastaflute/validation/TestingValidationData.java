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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.junit.Assert;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.message.UserMessage;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.validation.exception.ValidationErrorException;
import org.lastaflute.web.validation.exception.ValidationSuccessAttributeCannotCastException;

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
        final UserMessages messages = _cause.getMessages();
        final Iterator<UserMessage> ite = messages.silentAccessByIteratorOf(property);
        while (ite.hasNext()) {
            final UserMessage message = ite.next();
            final OptionalThing<Annotation> optAnno = message.getValidatorAnnotation();
            if (optAnno.isPresent()) {
                final Annotation anno = optAnno.get();
                if (annotationType.isAssignableFrom(anno.annotationType())) { // found
                    return; // OK
                }
            }
        }
        Assert.fail(buildNoValidationErrorMessage(messages, property, annotationType));
    }

    /**
     * Assert the message is required as validation error.
     * <pre>
     * data.requiredMessageOf("sea", DocksideMessages.ERRORS_...);
     * </pre>
     * @param property the name of property, which may have user messages. (NotNull)
     * @param messageKey The key of message as validation error, can be found by message manager. (NotNull)
     */
    public void requiredMessageOf(String property, String messageKey) {
        if (property == null) {
            throw new IllegalArgumentException("The argument 'property' should not be null.");
        }
        if (messageKey == null) {
            throw new IllegalArgumentException("The argument 'messageKey' should not be null.");
        }
        final UserMessages messages = _cause.getMessages();
        if (!messages.hasMessageOf(property, messageKey)) { // can determine annotation message since lastaflute-0.9.4
            Assert.fail(buildNoValidationErrorMessage(messages, property, messageKey));
        }
    }

    protected String buildNoValidationErrorMessage(UserMessages messages, String property, Class<? extends Annotation> annotationType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("No validation error for the validator annotation.");
        br.addItem("Property");
        br.addElement(property);
        br.addItem("Validator Annotation");
        br.addElement(annotationType);
        br.addItem("UserMessages");
        setupUserMessagesDisplay(messages, br);
        return br.buildExceptionMessage();
    }

    protected String buildNoValidationErrorMessage(UserMessages messages, String property, String messageKey) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("No validation error for the message key.");
        br.addItem("Property");
        br.addElement(property);
        br.addItem("Message Key");
        br.addElement(messageKey);
        br.addItem("UserMessages");
        setupUserMessagesDisplay(messages, br);
        return br.buildExceptionMessage();
    }

    protected void setupUserMessagesDisplay(UserMessages messages, final ExceptionMessageBuilder br) {
        final Set<String> propertySet = messages.toPropertySet();
        for (String current : propertySet) {
            br.addElement(current);
            final Iterator<UserMessage> ite = messages.silentAccessByIteratorOf(current);
            while (ite.hasNext()) {
                final UserMessage userMessage = ite.next();
                br.addElement("  " + userMessage);
            }
        }
    }

    // ===================================================================================
    //                                                                   Success Attribute
    //                                                                   =================
    /**
     * Assert the success attribute for the key is required.
     * @param <ATTRIBUTE> The type of attribute.
     * @param key The key of attribute. (NotNull)
     * @param attributeType The generic type of attribute to cast. (NotNull)
     * @return The found attribute by the key. (NotNull: if not found, assertion failure)
     */
    public <ATTRIBUTE> ATTRIBUTE requiredSuccessAttribute(String key, Class<ATTRIBUTE> attributeType) {
        final Map<String, Object> successAttributeMap = _cause.getMessages().getSuccessAttributeMap();
        final Object original = successAttributeMap.get(key);
        if (original == null) {
            Assert.fail(buildSuccessAttributeFailureMessage(successAttributeMap, key));
        }
        try {
            return attributeType.cast(original);
        } catch (ClassCastException e) { // similar to ValidationSuccess
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Cannot cast the validation success attribute");
            br.addItem("Attribute Key");
            br.addElement(key);
            br.addItem("Specified Type");
            br.addElement(attributeType);
            br.addItem("Existing Attribute");
            br.addElement(original.getClass());
            br.addElement(original);
            br.addItem("Attribute Map");
            br.addElement(successAttributeMap);
            final String msg = br.buildExceptionMessage();
            throw new ValidationSuccessAttributeCannotCastException(msg);
        }
    }

    protected String buildSuccessAttributeFailureMessage(Map<String, Object> successAttributeMap, String key) {
        return "Not found the success attribute: key=" + key + ", existing=" + successAttributeMap.keySet();
    }

    // ===================================================================================
    //                                                                          Error Hook
    //                                                                          ==========
    /**
     * Evaluate validation error hook for action response.
     * @param <RESPONSE> The type of action response.
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
        return "constraints." + annotationType.getSimpleName() + ".message";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public ValidationErrorException getCause() {
        return _cause;
    }
}
