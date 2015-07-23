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
package org.dbflute.utflute.core.transaction;

import java.sql.SQLException;

/**
 * The performer's callback of transaction.
 * <pre>
 * performNewTransaction(new TransactionPerformer() {
 *     public boolean perform() { <span style="color: #3F7E5E">// transaction scope</span>
 *         ...
 *         return false; <span style="color: #3F7E5E">// true: commit, false: roll-back</span>
 *     }
 * });
 * </pre>
 * @author jflute
 * @since 0.3.0 (2013/06/22 Saturday)
 */
public interface TransactionPerformer {

    /**
     * Perform the process in new transaction.
     * @return Does it commit the transaction? (false: roll-back)
     * @throws SQLException When it fails to handle the SQL in the performance.
     */
    boolean perform() throws SQLException;
}
