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

/**
 * The option of cannon-ball.
 * <pre>
 * cannonball(new CannonballRun() {
 *     public void drive(final CannonballCar car) {
 *         ...
 *     }
 * }, new CannonballOption().expectExceptionAny("Deadlock"));
 * </pre>
 * @author jflute
 * @since 0.3.8 (2014/02/25 Tuesday)
 */
public class CannonballOption {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final int DEFAULT_THREAD_COUNT = 10;
    public static final int DEFAULT_REPEAT_COUNT = 1;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected int _threadCount = DEFAULT_THREAD_COUNT;
    protected int _repeatCount = DEFAULT_REPEAT_COUNT;
    protected boolean _expectedSameResult;
    protected boolean _commitTransaction;
    protected boolean _suppressTransaction;
    protected String _expectedExceptionMessageAny;
    protected Class<? extends Throwable> _expectedExceptionTypeAny;
    protected CannonballFinalizer _finalizer;

    // ===================================================================================
    //                                                                         Easy-to-Use
    //                                                                         ===========
    /**
     * Set the count of thread (car) for the race. (default: 10)
     * @param threadCount The count of thread (car) for the race. (NotZero, NotMinus)
     * @return this. (NotNull)
     */
    public CannonballOption threadCount(int threadCount) {
        _threadCount = threadCount;
        return this;
    }

    /**
     * Set the count of repeat for the race. (default: 1)
     * @param repeatCount The count of repeat for the race. (NotZero, NotMinus)
     * @return this. (NotNull)
     */
    public CannonballOption repeatCount(int repeatCount) {
        _repeatCount = repeatCount;
        return this;
    }

    /**
     * Commit the transactions for the cars if no exception. (default: roll-back)
     * @return this. (NotNull)
     */
    public CannonballOption commitTx() {
        _commitTransaction = true;
        return this;
    }

    /**
     * Suppress the transactions for the cars. (default: begin transaction)
     * @return this. (NotNull)
     */
    public CannonballOption suppressTx() {
        _suppressTransaction = true;
        return this;
    }

    /**
     * Expect the goal results of all cars are same. <br>
     * You can set goal result like this:
     * <pre>
     * cannonball(new CannonballRun() {
     *     public void drive(final CannonballCar car) {
     *         ...
     *         Object result = ...;
     *         car.goal(result); // *here
     *     }
     * }, new CannonballOption().expectSameResult());
     * </pre>
     * @return this. (NotNull)
     */
    public CannonballOption expectSameResult() {
        _expectedSameResult = true;
        return this;
    }

    /**
     * Expect the any car throws the exception.
     * @param expectedExceptionType The type of expected exception. (NotNull)
     * @return this. (NotNull)
     */
    public CannonballOption expectExceptionAny(Class<? extends Throwable> expectedExceptionType) {
        _expectedExceptionTypeAny = expectedExceptionType;
        return this;
    }

    /**
     * Expect the any car throws the exception that contains the part of message.
     * @param expectedExceptionMessage The part of message for expected exception. (NotNull)
     * @return this. (NotNull)
     */
    public CannonballOption expectExceptionAny(String expectedExceptionMessage) {
        _expectedExceptionMessageAny = expectedExceptionMessage;
        return this;
    }

    public boolean isCheckExpectedExceptionAny() { // internal
        return _expectedExceptionTypeAny != null || _expectedExceptionMessageAny != null;
    }

    public boolean isMatchExpectedExceptionAny(Throwable cause) { // internal
        if (_expectedExceptionTypeAny != null) {
            return _expectedExceptionTypeAny.isAssignableFrom(cause.getClass());
        }
        if (_expectedExceptionMessageAny != null) {
            final String msg = cause.getMessage();
            return msg != null && msg.contains(_expectedExceptionMessageAny);
        }
        return false;
    }

    public String getExpectedExpceptionAnyExp() { // internal
        return _expectedExceptionTypeAny != null ? _expectedExceptionTypeAny.toString() : _expectedExceptionMessageAny;
    }

    /**
     * Set the callback for after care.
     * @param finalizer The callback for after care. (NotNull) 
     * @return this. (NotNull)
     */
    public CannonballOption finalizer(CannonballFinalizer finalizer) {
        _finalizer = finalizer;
        return this;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public int getThreadCount() {
        return _threadCount;
    }

    public void setRepeatCount(int repeatCount) {
        this._repeatCount = repeatCount;
    }

    public int getRepeatCount() {
        return _repeatCount;
    }

    public void setThreadCount(int threadCount) {
        this._threadCount = threadCount;
    }

    public boolean isExpectedSameResult() {
        return _expectedSameResult;
    }

    public void setExpectedSameResult(boolean expectedSameResult) {
        this._expectedSameResult = expectedSameResult;
    }

    public boolean isCommitTransaction() {
        return _commitTransaction;
    }

    public void setCommitTransaction(boolean commitTransaction) {
        this._commitTransaction = commitTransaction;
    }

    public boolean isSuppressTransaction() {
        return _suppressTransaction;
    }

    public void setSuppressTransaction(boolean suppressTransaction) {
        this._suppressTransaction = suppressTransaction;
    }

    public String getExpectedExceptionMessageAny() {
        return _expectedExceptionMessageAny;
    }

    public void setExpectedExceptionMessageAny(String expectedExceptionMessageAny) {
        this._expectedExceptionMessageAny = expectedExceptionMessageAny;
    }

    public Class<? extends Throwable> getExpectedExceptionTypeAny() {
        return _expectedExceptionTypeAny;
    }

    public void setExpectedExceptionTypeAny(Class<? extends Exception> expectedExceptionTypeAny) {
        this._expectedExceptionTypeAny = expectedExceptionTypeAny;
    }

    public CannonballFinalizer getFinalizer() {
        return _finalizer;
    }
}
