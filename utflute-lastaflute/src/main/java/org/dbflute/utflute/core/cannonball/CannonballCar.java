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
package org.dbflute.utflute.core.cannonball;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.opentest4j.AssertionFailedError;

/**
 * @author jflute
 * @since 0.3.8 (2014/02/25 Tuesday)
 */
public class CannonballCar {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final long _threadId;
    protected final CannonballLatch _ourLatch;
    protected final int _entryNumber;
    protected final Object _lockObj;
    protected final int _countOfEntry; // to check
    protected final CannonballLogger _logger;
    protected Object _runResult;
    protected Long _projectATimeLimit;
    protected boolean _suppressDecrementWhenBreakAway;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public CannonballCar(long threadId, CannonballLatch ourLatch, int entryNumber, Object lockObj, int countOfEntry,
            CannonballLogger logger) {
        _threadId = threadId;
        _ourLatch = ourLatch;
        _entryNumber = entryNumber;
        _lockObj = lockObj;
        _countOfEntry = countOfEntry;
        _logger = logger;
    }

    // ===================================================================================
    //                                                               Basic Thread Handling 
    //                                                               =====================
    public void teaBreak(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            String msg = "Failed to have a tea break but I want to...";
            throw new IllegalStateException(msg, e);
        }
    }

    // ===================================================================================
    //                                                                     CountDown Latch
    //                                                                     ===============
    /**
     * All cars restart from here.
     * <pre>
     * cannonball(new CannonballRun() {
     *     public void drive(CannonballCar car) {
     *         ...
     *         // all cars stop and wait for other cars coming here
     *         car.restart();
     *         ...
     *     }
     * }, new CannonballOption());
     * </pre>
     */
    public void restart() {
        _ourLatch.await(CannonballLatch.DEFAULT_LATCH_NAME, getEntryNumber());
    }

    /**
     * Execute project A plan for the entry. (other cars wait for the plan completion) <br>
     * But if the plan costs 3 seconds (as default), other cars go without waiting the plan completion.
     * <pre>
     * cannonball(new CannonballRun() {
     *     public void drive(CannonballCar car) {
     *         ...
     *         // entry number 1 only execute the plan, other cars wait for the plan completion.
     *         car.projectA(new CannonballProjectA() {
     *             public void plan() {
     *                 ...
     *             }
     *         }, 1);
     *         ...
     *     }
     * }, new CannonballOption());
     * </pre>
     * @param projectA The callback for the project A plan. (NotNull)
     * @param entryNumber The entry number that executes the plan. e.g. 1, 2, 3, ...(till thread count)
     */
    public void projectA(CannonballProjectA projectA, final int entryNumber) {
        checkEntryNumber(entryNumber);
        final String projectAKey = generateProjectAKey(projectA, entryNumber);
        _ourLatch.lineUpProjectA(projectAKey, entryNumber, getEntryNumber()); // all cars gathers at first
        final CannonballWatchingStatus watchingStatus = new CannonballWatchingStatus(projectAKey);
        CannonballDragon dragon = null;
        if (isEntryNumber(entryNumber)) {
            _logger.log("...Executing projectA: " + entryNumber);
            dragon = createDragon(watchingStatus);
            dragon.releaseIfOvertime(getFallbackOvertimeLimit()); // fall-back watch
            executeProjectA(projectA, dragon); // watching thread release waiting cars when exception
            synchronized (watchingStatus) { // with watching thread
                watchingStatus.markDone(); // to suppress unnecessary forcedly count down
            }
        }
        synchronized (watchingStatus) { // with watching thread
            final boolean forcedly = watchingStatus.containsForcedly();
            if (!forcedly) { // except forcedly car
                _ourLatch.waitForProjectA(projectAKey, entryNumber, getEntryNumber());
            } else { // forcedly car
                _ourLatch.comeBackFromOvertimeProjectA(projectAKey, entryNumber);
            }
            if (isEntryNumber(entryNumber)) {
                if (dragon.isExpectedNormallyDone()) {
                    if (forcedly) {
                        String msg = "expected: normally done, but was: the plan overtime: entryNumber=" + entryNumber;
                        throw new AssertionFailedError(msg);
                    }
                }
                if (dragon.isExpectedOvertime()) {
                    if (!forcedly) {
                        String msg = "expected: overtime, but was: the plan normally done: entryNumber=" + entryNumber;
                        throw new AssertionFailedError(msg);
                    }
                }
            }
        }
        teaBreak(100); // wait for broken car handling when assertion failure (for safety but inexact)
    }

    protected int getFallbackOvertimeLimit() {
        return 3000; // as default
    }

    protected String generateProjectAKey(CannonballProjectA projectA, int entryNumber) {
        return Integer.toHexString((projectA.getClass().getName() + String.valueOf(entryNumber)).hashCode());
    }

    protected CannonballDragon createDragon(CannonballWatchingStatus watchingStatus) {
        return new CannonballDragon(this, watchingStatus);
    }

    protected void executeProjectA(CannonballProjectA projectA, CannonballDragon dragon) {
        try {
            projectA.plan(dragon); // execute the plan
        } catch (RuntimeException e) {
            adjustDecrementWhenBreakAway();
            throw e;
        } catch (Error e) {
            adjustDecrementWhenBreakAway();
            throw e;
        }
    }

    protected void adjustDecrementWhenBreakAway() {
        // watching thread will release waiting cars and decrement thread count later
        _suppressDecrementWhenBreakAway = true;
    }

    // ===================================================================================
    //                                                                        Entry Number
    //                                                                        ============
    /**
     * Is this car same as the specified entry number?
     * @param entryNumber The entry number to compare.
     * @return The determination, true or false.
     */
    public boolean isEntryNumber(int entryNumber) {
        checkEntryNumber(entryNumber);
        return _entryNumber == entryNumber;
    }

    protected void checkEntryNumber(int entryNumber) {
        if (entryNumber > _countOfEntry) {
            throwCannonballEntryNumberOverException(entryNumber);
        }
    }

    protected void throwCannonballEntryNumberOverException(int entryNumber) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The entry number is over count of entries.");
        br.addItem("Entry Number");
        br.addElement(entryNumber);
        br.addItem("Count of Entry");
        br.addElement(_countOfEntry);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }

    // ===================================================================================
    //                                                                          Run Result
    //                                                                          ==========
    /**
     * Set the run result of the car to assert it.
     * <pre>
     * cannonball(new CannonballRun() {
     *     public void drive(final CannonballCar car) {
     *         ...
     *         Object result = ...;
     *         car.goal(result); // *here
     *     }
     * }, new CannonballOption().expectSameResult());
     * </pre>
     * @param runResult The result instance of the run. (NullAllowed)
     */
    public void goal(Object runResult) {
        this._runResult = runResult;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "{" + _threadId + ", " + _entryNumber + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * Get the thread ID of the car (current thread).
     * @return The long value of thread ID.
     */
    public long getThreadId() {
        return _threadId;
    }

    /**
     * Get the our latch to handle the threads.
     * @return The instance of our latch for cannon-ball. (NotNull)
     */
    public CannonballLatch getOurLatch() {
        return _ourLatch;
    }

    /**
     * Get the entry number of the car (current thread).
     * @return The assigned number. e.g. 1, 2, 3... (NotNull)
     */
    public int getEntryNumber() {
        return _entryNumber;
    }

    /**
     * Get the lock object to handle threads as you like it.
     * @return The common instance for all cars. (NotNull)
     */
    public Object getLockObj() {
        return _lockObj;
    }

    /**
     * Get the logger for the cannon-ball.
     * @return The common instance for all cars. (NotNull) 
     */
    public CannonballLogger getLogger() {
        return _logger;
    }

    /**
     * Get the run result for the car.
     * @return The object that is provided from the car. (NullAllowed: if no set)
     */
    public Object getRunResult() {
        return _runResult;
    }

    /**
     * Does it suppress decrementing active thread count when break-away? (internal)
     * @return The determination, true or false.
     */
    public boolean isSuppressDecrementWhenBreakAway() {
        return _suppressDecrementWhenBreakAway;
    }
}
