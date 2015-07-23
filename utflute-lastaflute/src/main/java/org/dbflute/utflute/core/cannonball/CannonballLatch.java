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
package org.dbflute.utflute.core.cannonball;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jflute
 * @since 0.3.8 (2014/02/25 Tuesday)
 */
public class CannonballLatch {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String DEFAULT_LATCH_NAME = "df:defaultLatch";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected int _activeCount; // might be decremented e.g. when breakaway
    protected final int _initialCount; // to check too many increment
    protected final CannonballLogger _logger;

    /** The map of latch related to latch name. (NotNull) */
    // basically synchronized so plain HashMap is allowed but just in case
    protected final Map<String, CannonballVaryingLatch> _ourLatchMap = new ConcurrentHashMap<String, CannonballVaryingLatch>();

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public CannonballLatch(int threadCount, CannonballLogger logger) {
        _activeCount = threadCount;
        _initialCount = threadCount;
        _logger = logger;
    }

    // ===================================================================================
    //                                                                               Await
    //                                                                               =====
    public void await(String latchName, int entryNumber) {
        doAwait(latchName, entryNumber, false);
    }

    public void awaitSilently(String latchName, int entryNumber) {
        doAwait(latchName, entryNumber, true);
    }

    protected void doAwait(String latchName, int entryNumber, boolean silently) {
        final CannonballVaryingLatch latch;
        final boolean last;
        synchronized (this) {
            latch = prepareLatch(latchName);
            if (latch.isReleasedLatch()) { // already released
                return;
            }
            last = latch.isLastCountLatch();
            if (last) {
                destroyLatchIfNeeds(latchName);
                if (!silently) {
                    _logger.log("Ready...Go! (restart): entryNumber=" + entryNumber);
                }
            }
            latch.countDown(); // ready go if last
        }
        if (!last) {
            if (isWaitingLatch(latchName)) {
                // to be exact, possible that threads after restart come here but no problem
                if (!silently) {
                    _logger.log("...Awaiting arrivals: entryNumber=" + entryNumber + ", count=" + latch.getCount());
                }
                latch.await();
            }
        }
    }

    protected synchronized CannonballVaryingLatch prepareLatch(String latchName) {
        CannonballVaryingLatch latch = _ourLatchMap.get(latchName);
        if (latch == null) {
            latch = new CannonballVaryingLatch(_activeCount);
            _ourLatchMap.put(latchName, latch);
        }
        return latch;
    }

    protected synchronized boolean isWaitingLatch(String latchName) {
        final CannonballVaryingLatch latch = _ourLatchMap.get(latchName);
        return latch != null && latch.getCount() > 0;
    }

    protected boolean isDefaultLatch(String latchName) {
        return DEFAULT_LATCH_NAME.equals(latchName);
    }

    protected synchronized CannonballVaryingLatch getDefaultLatch() {
        return _ourLatchMap.get(DEFAULT_LATCH_NAME);
    }

    protected synchronized void destroyLatchIfNeeds(String latchName) {
        if (isDefaultLatch(latchName)) {
            _ourLatchMap.remove(latchName);
        }
    }

    // ===================================================================================
    //                                                                            ProjectA
    //                                                                            ========
    public void lineUpProjectA(String projectAKey, int executionNumber, int currenNumber) { // no synchronized here
        awaitSilently(generateProjectALineUpLatchName(projectAKey), currenNumber);
    }

    protected String generateProjectALineUpLatchName(String projectAKey) {
        return projectAKey + "::lineUp";
    }

    public void waitForProjectA(String projectAKey, int executionNumber, int currenNumber) { // no synchronized here
        awaitSilently(generateProjectAWaitForLatchName(projectAKey), currenNumber);
    }

    protected String generateProjectAWaitForLatchName(String projectAKey) {
        return projectAKey + "::waitFor";
    }

    public synchronized void leaveProjectAAlone(String projectAKey, int entryNumber) { // e.g. when projectA overtime
        _logger.log("*Leaving the projectA car alone as overtime: entryNumber=" + entryNumber);
        decrementThreadCount();
        reset(generateProjectALineUpLatchName(projectAKey));
        reset(generateProjectAWaitForLatchName(projectAKey));
    }

    public synchronized void comeBackFromOvertimeProjectA(String projectAKey, int entryNumber) { // e.g. when projectA end with overtime
        _logger.log("*Coming back from overtime projectA finally: entryNumber=" + entryNumber);
        incrementThreadCount();
        if (isWaitingLatch(DEFAULT_LATCH_NAME)) {
            final CannonballVaryingLatch defaultLatch = getDefaultLatch();
            if (defaultLatch != null) {
                defaultLatch.incrementBufferCount();
            }
        }
    }

    // ===================================================================================
    //                                                              Break Away or Complete
    //                                                              ======================
    public synchronized void breakAway(int entryNumber, boolean suppressDecrement) {
        final String decrementExp = !suppressDecrement ? "(decrement)" : "";
        _logger.log("*Breaking away from cannonball race " + decrementExp + ": entryNumber=" + entryNumber);
        if (!suppressDecrement) {
            decrementThreadCount();
        }
        reset(DEFAULT_LATCH_NAME);
    }

    public synchronized void complete(int entryNumber, boolean suppressDecrement) {
        if (!suppressDecrement) {
            decrementThreadCountSilently();
        }
        reset(DEFAULT_LATCH_NAME); // may be enough to count down only... but just in case 
    }

    public synchronized void reset(String latchName) {
        final CannonballVaryingLatch latch = _ourLatchMap.get(latchName);
        if (latch == null) {
            return;
        }
        final long count = latch.getCount();
        if (count > 0) {
            _logger.log("...Resetting your latch: count=" + count);
            for (int i = 0; i < count; i++) {
                latch.countDown(); // is thread safe and allowed over count down
            }
            destroyLatchIfNeeds(latchName);
        }
    }

    // ===================================================================================
    //                                                                  Adjust ThreadCount
    //                                                                  ==================
    protected synchronized void incrementThreadCount() {
        if (_initialCount > _activeCount) {
            _logger.log("...Incrementing active thread count: " + _activeCount + " to " + (_activeCount + 1));
            ++_activeCount;
        } else {
            _logger.log("*Too many increment of thread count: first=" + _initialCount + ", current=" + _activeCount);
        }
    }

    protected synchronized void decrementThreadCount() {
        doDecrementThreadCount(false);
    }

    protected synchronized void decrementThreadCountSilently() {
        doDecrementThreadCount(true);
    }

    protected synchronized void doDecrementThreadCount(boolean silently) {
        if (_activeCount > 0) {
            if (!silently) {
                _logger.log("...Decrementing active thread count: " + _activeCount + " to " + (_activeCount - 1));
            }
            --_activeCount;
        } else {
            // output if silently because of warning
            _logger.log("*Too many decrement of thread count: current=" + _activeCount);
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public int getActiveCount() {
        return _activeCount;
    }

    public int getInitialCount() {
        return _initialCount;
    }
}
