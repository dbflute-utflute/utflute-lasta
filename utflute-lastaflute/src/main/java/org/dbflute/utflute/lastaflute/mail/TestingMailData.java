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
package org.dbflute.utflute.lastaflute.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.junit.jupiter.api.Assertions;
import org.lastaflute.core.mail.LaMailPostcard;

/**
 * @author jflute
 * @since 0.6.5 (2016/12/04 Sunday at bay maihama)
 */
public class TestingMailData {

    protected final Map<Class<?>, List<TestingPreparedMessage>> _messageMap = new ConcurrentHashMap<>();

    /**
     * @param postcardType The postcard type of MailFlute. (NotNull)
     * @return The list of prepared message for testing. (NotNull, NotEmpty)
     */
    public List<TestingPreparedMessage> required(Class<? extends LaMailPostcard> postcardType) {
        final List<TestingPreparedMessage> messageList = _messageMap.get(postcardType);
        if (messageList == null) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Not found the postcard in the prepared messages.");
            br.addItem("NotFound Postcard");
            br.addElement(postcardType);
            br.addItem("Existing Postcards");
            br.addElement(_messageMap.keySet());
            final String msg = br.buildExceptionMessage();
            Assertions.fail(msg);
        }
        if (messageList.isEmpty()) { // no way
            String msg = "Found the empty message list in the prepared messages: " + postcardType;
            Assertions.fail(msg);
        }
        return messageList;
    }

    public void saveMessage(Class<?> postcardType, TestingPreparedMessage message) { // for framework
        List<TestingPreparedMessage> messageList = _messageMap.get(postcardType);
        if (messageList == null) {
            messageList = new ArrayList<>();
            _messageMap.put(postcardType, messageList);
        }
        messageList.add(message);
    }

    public boolean isEmpty() {
        return _messageMap.isEmpty();
    }
}
