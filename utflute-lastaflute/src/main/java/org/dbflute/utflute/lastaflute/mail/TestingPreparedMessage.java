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
package org.dbflute.utflute.lastaflute.mail;

import java.util.List;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.mail.CardView;
import org.dbflute.mail.send.supplement.SMailPostingDiscloser;
import org.dbflute.mail.send.supplement.attachment.SMailReadAttachedData;
import org.junit.Assert;

/**
 * @author jflute
 * @since 0.6.5 (2016/12/04 Sunday at bay maihama)
 */
public class TestingPreparedMessage {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final CardView _cardView;
    protected final SMailPostingDiscloser _discloser;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TestingPreparedMessage(CardView cardView, SMailPostingDiscloser discloser) {
        _cardView = cardView;
        _discloser = discloser;
    }

    // ===================================================================================
    //                                                                     Required Access
    //                                                                     ===============
    public InternetAddress requiredFrom() { // as internet address
        return (InternetAddress) _discloser.getSavedFrom().get();
    }

    public List<InternetAddress> requiredToList() { // as internet address
        final List<Address> addrList = _discloser.getSavedToList();
        assertTrue("Found the empty to-address list: " + toPostcardDisp(), !addrList.isEmpty());
        return addrList.stream().map(addr -> (InternetAddress) addr).collect(Collectors.toList());
    }

    public List<InternetAddress> requiredCcList() { // as internet address
        final List<Address> addrList = _discloser.getSavedCcList();
        assertTrue("Found the empty cc-address list: " + toPostcardDisp(), !addrList.isEmpty());
        return addrList.stream().map(addr -> (InternetAddress) addr).collect(Collectors.toList());
    }

    public List<InternetAddress> requiredBccList() { // as internet address
        final List<Address> addrList = _discloser.getSavedBccList();
        assertTrue("Found the empty bcc-address list: " + toPostcardDisp(), !addrList.isEmpty());
        return addrList.stream().map(addr -> (InternetAddress) addr).collect(Collectors.toList());
    }

    public List<InternetAddress> requiredReplyToList() { // as internet address
        final List<Address> addrList = _discloser.getSavedReplyToList();
        assertTrue("Found the empty reply-to-address list: " + toPostcardDisp(), !addrList.isEmpty());
        return addrList.stream().map(addr -> (InternetAddress) addr).collect(Collectors.toList());
    }

    public String requiredReturnPath() {
        return _discloser.getSavedReturnPath().get();
    }

    public String requiredSubject() {
        return _discloser.getSavedSubject().get();
    }

    public String requiredPlainText() {
        return _discloser.getSavedPlainText().get();
    }

    public String requiredHtmlText() {
        return _discloser.getSavedHtmlText().get();
    }

    public SMailReadAttachedData requiredAttachment(String filenameOnHeader) {
        final SMailReadAttachedData attachedData = _discloser.getSavedAttachmentMap().get(filenameOnHeader);
        if (attachedData == null) {
            fail("Not found the attachment in the mail message: filenameOnHeader=" + filenameOnHeader + ", postcard=" + toPostcardDisp());
        }
        return attachedData;
    }

    // ===================================================================================
    //                                                                  Prepared Assertion
    //                                                                  ==================
    public void assertSubjectContains(String keyword) { // ignoring case
        final String lowerText = requiredSubject().toLowerCase();
        assertContainsKeyword("subject", lowerText, keyword);
    }

    public void assertPlainTextContains(String keyword) { // ignoring case
        final String lowerText = requiredPlainText().toLowerCase();
        assertContainsKeyword("plainText", lowerText, keyword);
    }

    public void assertHtmlTextContains(String keyword) { // ignoring case
        final String lowerText = requiredHtmlText().toLowerCase();
        assertContainsKeyword("htmlText", lowerText, keyword);
    }

    protected void assertContainsKeyword(String title, String lowerText, String keyword) {
        final boolean result = lowerText.contains(keyword);
        if (!result) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Not found the keyword in the mail message.");
            br.addItem("NotFound Keyword");
            br.addElement(keyword);
            br.addItem("Checked Text Part");
            br.addElement(title);
            br.addItem("Your Postcard");
            br.addElement(toPostcardDisp());
            final String msg = br.buildExceptionMessage();
            fail(msg);
        }
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void fail(String msg) {
        Assert.fail(msg);
    }

    protected void assertTrue(String msg, boolean condition) {
        Assert.assertTrue(msg, condition);
    }

    protected Object toPostcardDisp() {
        return _discloser.getOfficeManagedLoggingMap();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public CardView getCardView() {
        return _cardView;
    }

    public SMailPostingDiscloser getDiscloser() {
        return _discloser;
    }
}
