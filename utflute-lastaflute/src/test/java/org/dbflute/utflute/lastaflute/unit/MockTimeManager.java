/*
 * Copyright 2014-2024 the original author or authors.
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
package org.dbflute.utflute.lastaflute.unit;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.TimeZone;

import org.dbflute.helper.HandyDate;
import org.dbflute.system.DBFluteSystem;
import org.lastaflute.core.time.TimeManager;

/**
 * @author jflute
 * @since 0.8.7 (2018/06/15 Friday)
 */
public class MockTimeManager implements TimeManager {

    @Override
    public LocalDate currentDate() {
        return DBFluteSystem.currentLocalDate();
    }

    @Override
    public LocalDateTime currentDateTime() {
        return DBFluteSystem.currentLocalDateTime();
    }

    @Override
    public HandyDate currentHandyDate() {
        return new HandyDate(currentDate());
    }

    @Override
    public long currentMillis() {
        return DBFluteSystem.currentTimeMillis();
    }

    @Override
    public Date currentUtilDate() {
        return currentHandyDate().getDate();
    }

    @Override
    public Timestamp currentTimestamp() {
        return currentHandyDate().getTimestamp();
    }

    @Override
    public Date flashDate() {
        return currentUtilDate();
    }

    @Override
    public boolean isBusinessDate(LocalDate targetDate) {
        return false;
    }

    @Override
    public Date getNextBusinessDate(LocalDate baseDate, int addedDay) {
        return null;
    }

    @Override
    public TimeZone getBusinessTimeZone() {
        return DBFluteSystem.getFinalTimeZone();
    }
}
