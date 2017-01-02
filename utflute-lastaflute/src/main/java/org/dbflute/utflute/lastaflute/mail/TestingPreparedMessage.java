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
import java.util.Map;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.mail.CardView;
import org.dbflute.mail.PostOffice;
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
    protected final CardView _cardView; // not null
    protected final SMailPostingDiscloser _discloser; // not null

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
    public InternetAddress requiredFrom() { // not null, as internet address
        return (InternetAddress) _discloser.getSavedFrom().get();
    }

    public List<InternetAddress> requiredToList() { // not null, as internet address
        final List<Address> addrList = _discloser.getSavedToList();
        assertTrue("Not found the to-address: postcard=" + toPostcardDisp(), !addrList.isEmpty());
        return addrList.stream().map(addr -> (InternetAddress) addr).collect(Collectors.toList());
    }

    public List<InternetAddress> requiredCcList() { // not null, as internet address
        final List<Address> addrList = _discloser.getSavedCcList();
        assertTrue("Not found the cc-address: postcard=" + toPostcardDisp(), !addrList.isEmpty());
        return addrList.stream().map(addr -> (InternetAddress) addr).collect(Collectors.toList());
    }

    public List<InternetAddress> requiredBccList() { // not null, as internet address
        final List<Address> addrList = _discloser.getSavedBccList();
        assertTrue("Not found the bcc-address: postcard=" + toPostcardDisp(), !addrList.isEmpty());
        return addrList.stream().map(addr -> (InternetAddress) addr).collect(Collectors.toList());
    }

    public List<InternetAddress> requiredReplyToList() { // not null, as internet address
        final List<Address> addrList = _discloser.getSavedReplyToList();
        assertTrue("Not found the reply-to-address: postcard=" + toPostcardDisp(), !addrList.isEmpty());
        return addrList.stream().map(addr -> (InternetAddress) addr).collect(Collectors.toList());
    }

    public String requiredReturnPath() { // not null
        return _discloser.getSavedReturnPath().get();
    }

    public String requiredSubject() { // not null
        return _discloser.getSavedSubject().get();
    }

    public String requiredPlainText() { // not null
        return _discloser.getSavedPlainText().get();
    }

    public String requiredHtmlText() { // not null
        return _discloser.getSavedHtmlText().get();
    }

    public SMailReadAttachedData requiredAttachment(String filenameOnHeader) { // not null
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
        assertTrue("The argument 'keyword' should not be null.", keyword != null);
        assertContainsKeyword("subject", requiredSubject().toLowerCase(), keyword.toLowerCase());
    }

    public void assertPlainTextContains(String keyword) { // ignoring case
        assertTrue("The argument 'keyword' should not be null.", keyword != null);
        assertContainsKeyword("plainText", requiredPlainText().toLowerCase(), keyword.toLowerCase());
    }

    public void assertHtmlTextContains(String keyword) { // ignoring case
        assertTrue("The argument 'keyword' should not be null.", keyword != null);
        assertContainsKeyword("htmlText", requiredHtmlText().toLowerCase(), keyword.toLowerCase());
    }

    protected void assertContainsKeyword(String title, String text, String keyword) {
        final boolean result = text.contains(keyword);
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
        final Map<String, Map<String, Object>> loggingMap = _discloser.getOfficeManagedLoggingMap(); // contains various info
        final Map<String, Object> sysinfoMap = loggingMap.get(PostOffice.LOGGING_TITLE_SYSINFO);
        return sysinfoMap != null ? sysinfoMap : loggingMap;
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
