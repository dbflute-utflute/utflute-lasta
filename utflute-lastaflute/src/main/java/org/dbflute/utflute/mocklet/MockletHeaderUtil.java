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
package org.dbflute.utflute.mocklet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author modified by jflute (originated in Seasar)
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class MockletHeaderUtil {

    private static SimpleDateFormat[] DATE_FORMATTERS = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US), new SimpleDateFormat("yyyy-MM-dd"),
            new SimpleDateFormat("yyyy/MM/dd") };

    protected MockletHeaderUtil() {
    }

    public static long getDateValue(String value) {
        if (value == null) {
            return -1L;
        }
        for (int i = 0; i < DATE_FORMATTERS.length; i++) {
            try {
                return DATE_FORMATTERS[i].parse(value).getTime();
            } catch (ParseException ignore) {
            }
        }
        throw new IllegalArgumentException(value);
    }

    public static int getIntValue(String value) {
        if (value == null) {
            return -1;
        }
        return Integer.parseInt(value);
    }

    public static String getDateValue(long value) {
        return DATE_FORMATTERS[0].format(new Date(value));
    }
}
