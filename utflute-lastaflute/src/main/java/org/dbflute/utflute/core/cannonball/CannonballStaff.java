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
package org.dbflute.utflute.core.cannonball;

import org.dbflute.utflute.core.transaction.TransactionResource;

/**
 * @author jflute
 * @since 0.3.8 (2014/02/25 Tuesday)
 */
public interface CannonballStaff {

    TransactionResource help_beginTransaction();

    void help_prepareAccessContext();

    void help_clearAccessContext();

    void help_assertEquals(Object expected, Object actual);

    void help_fail(String msg);

    void help_log(Object... msges);

    String help_ln();
}
