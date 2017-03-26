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
package org.dbflute.utflute.lastaflute.mail;

import java.util.Map;
import java.util.function.Consumer;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.mail.CardView;
import org.dbflute.mail.PostOffice;
import org.dbflute.mail.send.hook.SMailCallbackContext;
import org.dbflute.mail.send.hook.SMailPreparedMessageHook;
import org.dbflute.mail.send.supplement.SMailPostingDiscloser;

import junit.framework.Assert;

/**
 * @author jflute
 * @since 0.6.5 (2016/12/04 Sunday at bay maihama)
 */
public class MailMessageAssertion {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Consumer<TestingMailData> _testingMailDataCall;
    protected final TestingMailData _testingMailData;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MailMessageAssertion(Consumer<TestingMailData> oneArgLambda) {
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

    // ===================================================================================
    //                                                                        Hook Message
    //                                                                        ============
    protected void hookMessageSaving() {
        SMailCallbackContext.clearPreparedMessageHookOnThread(); // just in case
        SMailCallbackContext.setPreparedMessageHookOnThread(new SMailPreparedMessageHook() {
            public void hookPreparedMessage(CardView cardView, SMailPostingDiscloser discloser) {
                final TestingPreparedMessage message = new TestingPreparedMessage(cardView, discloser);
                assertBasicCorrectness(cardView, discloser);
                _testingMailData.saveMessage(cardView.getMessageTheme().get(), message);
            }
        });
    }

    // ===================================================================================
    //                                                                   Basic Correctness
    //                                                                   =================
    protected void assertBasicCorrectness(CardView cardView, SMailPostingDiscloser discloser) {
        discloser.getSavedPlainText().ifPresent(subject -> {
            assertNotContainsMistakeComment(cardView, discloser, "subject", subject);
        });
        discloser.getSavedPlainText().ifPresent(plainText -> {
            assertNotContainsMistakeComment(cardView, discloser, "plainText", plainText);
        });
        discloser.getSavedHtmlText().ifPresent(htmlText -> {
            assertNotContainsMistakeComment(cardView, discloser, "htmlText", htmlText);
        });
    }

    protected void assertNotContainsMistakeComment(CardView cardView, SMailPostingDiscloser discloser, String title, String text) {
        // basically failed before this process but just in case
        final String lowerText = text.toLowerCase();
        assertNoMistakePlaceholder(cardView, discloser, title, lowerText, "/*pmb.");
        assertNoMistakePlaceholder(cardView, discloser, title, lowerText, "/* pmb.");
        assertNoMistakePlaceholder(cardView, discloser, title, lowerText, "/*pnb.");
        assertNoMistakePlaceholder(cardView, discloser, title, lowerText, "/* pnb.");
        assertNoMistakePlaceholder(cardView, discloser, title, lowerText, "/*pmd.");
        assertNoMistakePlaceholder(cardView, discloser, title, lowerText, "/* pmd.");
    }

    protected void assertNoMistakePlaceholder(CardView cardView, SMailPostingDiscloser discloser, String title, String lowerText,
            String keyword) {
        final boolean result = lowerText.contains(keyword);
        if (result) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Detected the mistake placeholder in the mail message.");
            br.addItem("Advice");
            br.addElement("Fix your mistake placeholder like this:");
            br.addElement("For example:");
            br.addElement("  (x):");
            br.addElement("    /* pmb.xxx */");
            br.addElement("    /*pnb.xxx*/");
            br.addElement("    /*pmd.xxx*/");
            br.addElement("  (o):");
            br.addElement("    /*pmb.xxx*/");
            br.addItem("Mistake Placeholder");
            br.addElement(keyword);
            br.addItem("Checked Text Part");
            br.addElement(title);
            br.addItem("Your Postcard");
            br.addElement(toPostcardDisp(discloser));
            final String msg = br.buildExceptionMessage();
            fail(msg);
        }
    }

    // ===================================================================================
    //                                                                        for PostTest
    //                                                                        ============
    public void assertMailData() {
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
        br.addElement("    reserveMailAssertion(mailData -> { // *Bad");
        br.addElement("        ...");
        br.addElement("    });");
        br.addElement("  (o):");
        br.addElement("    reserveMailAssertion(mailData -> { // Good");
        br.addElement("        ...");
        br.addElement("    });");
        br.addElement("    HtmlResponse response = action.index(...); // mail here");
        final String msg = br.buildExceptionMessage();
        fail(msg);
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void fail(String msg) {
        Assert.fail(msg);
    }

    protected Object toPostcardDisp(SMailPostingDiscloser discloser) {
        final Map<String, Map<String, Object>> loggingMap = discloser.getOfficeManagedLoggingMap(); // contains various info
        final Map<String, Object> sysinfoMap = loggingMap.get(PostOffice.LOGGING_TITLE_SYSINFO);
        return sysinfoMap != null ? sysinfoMap : loggingMap;
    }
}
