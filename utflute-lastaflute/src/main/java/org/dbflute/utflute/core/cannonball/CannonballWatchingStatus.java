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
package org.dbflute.utflute.core.cannonball;

import java.util.HashSet;
import java.util.Set;

/**
 * The status of watching project A plan. <br>
 * This methods should be called in synchronized scope.
 * @author jflute
 * @since 0.3.8 (2014/03/03 Monday)
 */
public class CannonballWatchingStatus {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String DONE_MARK = "done";
    public static final String FORCEDLY_MARK = "forcedly";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Set<String> _watchingMarkSet = new HashSet<String>();
    protected final String _projectAKey;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public CannonballWatchingStatus(String projectAKey) {
        _projectAKey = projectAKey;
    }

    // ===================================================================================
    //                                                                         Mark Status
    //                                                                         ===========
    public void markDone() {
        _watchingMarkSet.add(DONE_MARK);
    }

    public void markForecdly() {
        _watchingMarkSet.add(FORCEDLY_MARK);
    }

    // ===================================================================================
    //                                                                        Check Status
    //                                                                        ============
    public boolean containsForcedly() {
        return _watchingMarkSet.contains(FORCEDLY_MARK);
    }

    public boolean isWaiting() {
        // both are checked because of two or more watch threads
        return !_watchingMarkSet.contains(DONE_MARK) && !_watchingMarkSet.contains(FORCEDLY_MARK);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getProjectAKey() {
        return _projectAKey;
    }
}
