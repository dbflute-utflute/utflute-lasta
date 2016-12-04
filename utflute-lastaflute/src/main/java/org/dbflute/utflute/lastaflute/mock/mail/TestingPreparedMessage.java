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

import org.dbflute.mail.CardView;
import org.dbflute.mail.send.supplement.SMailPostingDiscloser;
import org.junit.Assert;

/**
 * @author jflute
 * @since 0.6.5 (2016/12/04 Sunday at bay maihama)
 */
public class TestingPreparedMessage {

    protected final CardView _cardView;
    protected final SMailPostingDiscloser _discloser;

    public TestingPreparedMessage(CardView cardView, SMailPostingDiscloser discloser) {
        _cardView = cardView;
        _discloser = discloser;
    }

    public void assertSubjectContainsAll(String... keywords) {
        // TODO jflute xxxxxxxx (2016/12/05)
        String text = _discloser.getSavedSubject().get();
        for (String keyword : keywords) {
            String msg = "Not found the keyword in the subject: keyword=" + keyword;
            Assert.assertTrue(msg, text.contains(keyword));
        }
    }

    public void assertPlainTextContainsAll(String... keywords) {
        String text = _discloser.getSavedPlainText().get();
        for (String keyword : keywords) {
            String msg = "Not found the keyword in the plain text: keyword=" + keyword;
            Assert.assertTrue(msg, text.contains(keyword));
        }
    }

    public void assertHtmlTextContainsAll(String... keywords) {
        String text = _discloser.getSavedHtmlText().get();
        for (String keyword : keywords) {
            String msg = "Not found the keyword in the html text: keyword=" + keyword;
            Assert.assertTrue(msg, text.contains(keyword));
        }
    }

    public CardView getCardView() {
        return _cardView;
    }

    public SMailPostingDiscloser getDiscloser() {
        return _discloser;
    }
}
