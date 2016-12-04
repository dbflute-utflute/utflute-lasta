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
package org.dbflute.utflute.lastaflute.mock.mail;

import java.util.function.Consumer;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.mail.CardView;
import org.dbflute.mail.send.hook.SMailCallbackContext;
import org.dbflute.mail.send.hook.SMailPreparedMessageHook;
import org.dbflute.mail.send.supplement.SMailPostingDiscloser;

import junit.framework.Assert;

/**
 * @author jflute
 * @since 0.6.5 (2016/12/04 Sunday at bay maihama)
 */
public class MockMailMessageValidator {

    protected final Consumer<TestingMailData> _testingMailDataCall;
    protected final TestingMailData _testingMailData;

    public MockMailMessageValidator(Consumer<TestingMailData> oneArgLambda) {
        if (oneArgLambda == null) {
            throw new IllegalArgumentException("The argument 'oneArgLambda' should not be null.");
        }
        _testingMailDataCall = oneArgLambda;
        _testingMailData = newTestingMailData();
        hookMessageSaving();
    }

    protected TestingMailData newTestingMailData() {
        return new TestingMailData();
    }

    protected void hookMessageSaving() {
        SMailCallbackContext.clearPreparedMessageHookOnThread(); // just in case
        SMailCallbackContext.setPreparedMessageHookOnThread(new SMailPreparedMessageHook() {

            public void hookPreparedMessage(CardView cardView, SMailPostingDiscloser discloser) {
                final TestingPreparedMessage message = new TestingPreparedMessage(cardView, discloser);
                assertBasicCorrectness(discloser);
                _testingMailData.saveMessage(cardView.getMessageTheme().get(), message);
            }

            private void assertBasicCorrectness(SMailPostingDiscloser discloser) {
                discloser.getSavedPlainText().ifPresent(subject -> {
                    assertNotContainsMistakeComment(subject);
                });
                discloser.getSavedPlainText().ifPresent(plainText -> {
                    assertNotContainsMistakeComment(plainText);
                });
                discloser.getSavedHtmlText().ifPresent(htmlText -> {
                    assertNotContainsMistakeComment(htmlText);
                });
            }

            private void assertNotContainsMistakeComment(String text) {
                final String lowerText = text.toLowerCase();
                Assert.assertFalse(lowerText.contains("/*pmb."));
                Assert.assertFalse(lowerText.contains("/* pmb."));
                Assert.assertFalse(lowerText.contains("/*pnb."));
                Assert.assertFalse(lowerText.contains("/* pnb."));
                Assert.assertFalse(lowerText.contains("/*pmd."));
                Assert.assertFalse(lowerText.contains("/* pmd."));
            }
        });
    }

    public void validateMailData() {
        SMailCallbackContext.clearPreparedMessageHookOnThread();
        if (_testingMailData.isEmpty()) {
            throwMailSendingNotFoundException();
        } else {
            _testingMailDataCall.accept(_testingMailData);
        }
    }

    protected void throwMailSendingNotFoundException() {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the mail sending in this test case.");
        br.addItem("Advice");
        br.addElement("Make sure your mail sending logic. (really send?)");
        br.addElement("Or call the validation method before your sending logic.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    HtmlResponse response = action.index(...); // mail here");
        br.addElement("    reserveMailValidation(mailData -> { // *Bad");
        br.addElement("        ...");
        br.addElement("    });");
        br.addElement("  (o):");
        br.addElement("    reserveMailValidation(mailData -> { // Good");
        br.addElement("        ...");
        br.addElement("    });");
        br.addElement("    HtmlResponse response = action.index(...); // mail here");
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }
}
