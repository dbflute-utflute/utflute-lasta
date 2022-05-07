/*
 * Copyright 2014-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.utflute.core.transaction.TransactionResource;
import org.dbflute.util.Srl;

import junit.framework.AssertionFailedError;

/**
 * @author jflute
 * @since 0.3.8 (2014/02/25 Tuesday)
 */
public class CannonballDirector {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final CannonballStaff _cannonballHelper;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public CannonballDirector(CannonballStaff cannonballHelper) {
        _cannonballHelper = cannonballHelper;
    }

    // ===================================================================================
    //                                                                         Thread Fire
    //                                                                         ===========
    public void readyGo(CannonballRun execution, CannonballOption option) {
        if (execution == null) {
            String msg = "The argument 'execution' should be not null.";
            throw new IllegalArgumentException(msg);
        }
        if (option == null) {
            String msg = "The argument 'option' should be not null.";
            throw new IllegalArgumentException(msg);
        }
        final List<CannonballRetireException> retireExList = new ArrayList<CannonballRetireException>();
        try {
            try {
                for (int i = 0; i < option.getRepeatCount(); i++) {
                    final List<Object> resultList = doThreadFire(execution, option);
                    for (Object result : resultList) {
                        if (result instanceof CannonballRetireException) {
                            retireExList.add((CannonballRetireException) result);
                        }
                    }
                }
            } finally {
                final CannonballFinalizer finalizer = option.getFinalizer();
                if (finalizer != null) {
                    try {
                        log("...Running finalizer for fired threads");
                        finalizer.run();
                    } catch (RuntimeException continued) {
                        handleFinalizerException(continued);
                    }
                }
            }
        } catch (CannonballRetireException e) {
            retireExList.add(e);
        }
        if (option.isCheckExpectedExceptionAny()) {
            handleExpectedExceptionAny(option, retireExList);
        } else {
            handleNormalException(retireExList);
        }
    }

    protected List<Object> doThreadFire(CannonballRun execution, CannonballOption option) {
        // ## Arrange ##
        final ExecutorService service = Executors.newCachedThreadPool();
        final int threadCount = option.getThreadCount();
        final CountDownLatch ready = new CountDownLatch(threadCount);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch goal = new CountDownLatch(threadCount);
        final CannonballLogger logger = createLogger();
        final CannonballLatch ourLatch = new CannonballLatch(threadCount, logger);
        final Object lockObj = new Object();
        final List<Future<Object>> futureList = new ArrayList<Future<Object>>();
        for (int i = 0; i < threadCount; i++) { // basically synchronized with parameter size
            final int entryNumber = i + 1;
            final Callable<Object> callable = createCallable(execution, option, ready, start, goal, ourLatch, entryNumber, lockObj, logger);
            final Future<Object> future = service.submit(callable);
            futureList.add(future);
        }

        // ## Act ##
        log("/- - - - - - - - - - - - - - - - - - - - - -");
        log("                                 Cannon-ball");
        log("                                 - - - - - -");
        start.countDown();
        try {
            // wait until all threads are finished
            goal.await();
        } catch (InterruptedException e) {
            String msg = "goal.await() was interrupted!";
            throw new IllegalStateException(msg, e);
        }
        log("- - - - - - - - -/ *All threads were fired");

        // ## Assert ##
        final List<Object> resultList = handleFuture(option, futureList);
        assertSameResultIfExpected(option, resultList);
        return resultList;
    }

    protected CannonballLogger createLogger() {
        return new CannonballLogger() {
            public void log(Object... msgs) {
                CannonballDirector.this.log(msgs);
            }
        };
    }

    protected List<Object> handleFuture(CannonballOption option, List<Future<Object>> futureList) {
        final List<Object> resultList = new ArrayList<Object>();
        for (Future<Object> future : futureList) {
            Object result = null;
            try {
                result = future.get();
            } catch (InterruptedException e) {
                String msg = "future.get() was interrupted!";
                throw new IllegalStateException(msg, e);
            } catch (ExecutionException continued) {
                String msg = "Failed to fire the thread: " + future;
                result = new CannonballRetireException(msg, continued.getCause());
            }
            resultList.add(result);
        }
        return resultList;
    }

    protected <RESULT> void assertSameResultIfExpected(CannonballOption option, List<RESULT> resultList) {
        if (option.isExpectedSameResult()) {
            RESULT preResult = null;
            for (RESULT result : resultList) {
                log(result);
                if (preResult == null) {
                    preResult = result;
                    continue;
                }
                assertEquals(preResult, result);
            }
        }
    }

    protected void handleFinalizerException(RuntimeException continued) {
        final StringBuilder sb = new StringBuilder();
        sb.append("*Failed to run finalizer:");
        sb.append(ln()).append("/= = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =");
        sb.append(ln()).append(continued.getMessage());
        sb.append(ln()).append("= = = = = = = = = =/");
        final String msg = sb.toString();
        log(msg);
    }

    // ===================================================================================
    //                                                                            Callable
    //                                                                            ========
    protected Callable<Object> createCallable(final CannonballRun run, final CannonballOption option, final CountDownLatch ready,
            final CountDownLatch start, final CountDownLatch goal, final CannonballLatch ourLatch, final int entryNumber,
            final Object lockObj, final CannonballLogger logger) {
        return new Callable<Object>() {
            public Object call() { // each thread here
                final long threadId = Thread.currentThread().getId();
                final CannonballCar car = createCar(threadId, ourLatch, entryNumber, lockObj, option, logger);
                boolean failure = false;
                try {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        String msg = "start.await() was interrupted: start=" + start;
                        throw new IllegalStateException(msg, e);
                    }
                    prepareBeginning();
                    prepareAccessContext();
                    TransactionResource txRes = null;
                    if (!option.isSuppressTransaction()) {
                        txRes = beginTransaction();
                    }
                    Object result = null;
                    try {
                        run.drive(car);
                        result = car.getRunResult();
                    } catch (RuntimeException e) {
                        failure = true;
                        throw e;
                    } catch (Error e) {
                        failure = true;
                        throw e;
                    } finally {
                        if (txRes != null) {
                            try {
                                if (!failure && option.isCommitTransaction()) {
                                    txRes.commit();
                                } else {
                                    txRes.rollback();
                                }
                            } catch (Exception continued) {
                                log("*Failed to commit or roll-back: " + continued.getMessage());
                            }
                        }
                        clearAccessContext();
                    }
                    return result;
                } finally {
                    goal.countDown();

                    // release waiting threads
                    final boolean suppressDecrement = car.isSuppressDecrementWhenBreakAway();
                    if (failure) {
                        ourLatch.breakAway(entryNumber, suppressDecrement);
                    } else {
                        ourLatch.complete(entryNumber, suppressDecrement);
                    }
                }
            }
        };
    }

    protected CannonballCar createCar(long threadId, CannonballLatch ourLatch, int entryNumber, Object lockObj, CannonballOption option,
            CannonballLogger logger) {
        final int countOfEntry = option.getThreadCount();
        return new CannonballCar(threadId, ourLatch, entryNumber, lockObj, countOfEntry, logger);
    }

    // ===================================================================================
    //                                                                  Exception Handling
    //                                                                  ==================
    // -----------------------------------------------------
    //                                    Expected Exception
    //                                    ------------------
    protected void handleExpectedExceptionAny(CannonballOption option, List<CannonballRetireException> retireExList) {
        final List<Throwable> expectedCauseList = new ArrayList<Throwable>();
        final List<Throwable> unexpectedCauseList = new ArrayList<Throwable>();
        final String expectedExpceptionAnyExp = option.getExpectedExpceptionAnyExp();
        if (retireExList.isEmpty()) {
            fail("The cannonball cars should throw the exception: " + expectedExpceptionAnyExp);
        }
        for (CannonballRetireException retireEx : retireExList) {
            final Throwable cause = retireEx.getCause();
            final Throwable targetCause;
            if (cause != null) {
                if (cause instanceof AssertionFailedError) { // already asserted
                    throw (AssertionFailedError) cause; // it comes first
                }
                targetCause = cause;
            } else {
                targetCause = retireEx;
            }
            if (option.isMatchExpectedExceptionAny(targetCause)) {
                expectedCauseList.add(targetCause);
            } else {
                unexpectedCauseList.add(targetCause);
            }
        }
        if (expectedCauseList.isEmpty()) { // and unexpected exception found
            throwUnexpectedExceptionFound(unexpectedCauseList, expectedExpceptionAnyExp);
        } else { // expected cause exists
            handleExpectedExceptionFound(expectedCauseList, unexpectedCauseList, expectedExpceptionAnyExp);
        }
    }

    protected void throwUnexpectedExceptionFound(List<Throwable> unexpectedCauseList, String expectedExpceptionAnyExp) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The unexpected exception was thrown in cannonball().");
        br.addItem("Advice");
        br.addElement("Expect the exception is");
        br.addElement("  '" + expectedExpceptionAnyExp + "'.");
        br.addElement("But such an exception was not found,");
        br.addElement("while unexpected exception was found.");
        br.addItem("Unexpected Exception");
        br.addElement("The unexpected exception are following.");
        br.addElement("(And you can also see for the detail in your log)");
        for (Throwable unexpectedCause : unexpectedCauseList) {
            final String oneLine = buildOneLineExceptionMessage(unexpectedCause);
            br.addElement(unexpectedCause.getClass().getName());
            br.addElement("  '" + oneLine + "'"); // one line
            log(unexpectedCause); // for detail
        }
        final String msg = br.buildExceptionMessage();
        throw new AssertionFailedError(msg);
    }

    protected String buildOneLineExceptionMessage(Throwable unexpectedCause) {
        final String message = unexpectedCause.getMessage();
        if (message == null) {
            return "null";
        }
        if (message.contains(ln())) {
            return Srl.substringFirstFront(message, "\n") + "...";
        }
        return message;
    }

    protected void handleExpectedExceptionFound(List<Throwable> expectedCauseList, List<Throwable> unexpectedCauseList,
            String expectedExpceptionAnyExp) {
        StringBuilder sb = new StringBuilder();
        sb.append(ln()).append("_/_/_/_/_/_/_/_/_/_/");
        sb.append(ln()).append(" Expected exception");
        sb.append(ln()).append("_/_/_/_/_/_/_/_/_/_/");
        buildExceptionOverview(sb, expectedCauseList);
        sb.append(ln());
        final boolean unexpected = !unexpectedCauseList.isEmpty();
        if (unexpected) {
            sb.append(ln()).append("_/_/_/_/_/_/_/_/_/_/_/");
            sb.append(ln()).append(" Unexpected Exception");
            sb.append(ln()).append("_/_/_/_/_/_/_/_/_/_/_/");
            buildExceptionOverview(sb, unexpectedCauseList);
            sb.append(ln());
        }
        sb.append(ln());
        final String supplement = (unexpected ? " (with unexpected)" : "");
        sb.append("*The expected exception" + supplement + " was found: " + expectedExpceptionAnyExp);
        log(sb.toString());
    }

    protected void buildExceptionOverview(StringBuilder sb, List<Throwable> unexpectedCauseList) {
        for (Throwable unexpectedCause : unexpectedCauseList) {
            final String msg = unexpectedCause.getMessage(); // output all string instead of no stack trace
            sb.append(ln()).append(unexpectedCause.getClass().getName()).append(": ").append(msg);
        }
    }

    // -----------------------------------------------------
    //                                      Normal Exception
    //                                      ----------------
    protected void handleNormalException(List<CannonballRetireException> retireExList) {
        if (retireExList.isEmpty()) {
            return;
        }
        boolean titleDone = false;
        for (CannonballRetireException retireEx : retireExList) {
            final Throwable cause = retireEx.getCause();
            if (cause instanceof AssertionFailedError) {
                throw (AssertionFailedError) cause;
            }
            if (!titleDone) {
                log("_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/");
                log(" Cannonball Retire Exception");
                log("_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/");
                titleDone = true;
            }
            log(cause != null ? cause : retireEx);
        }
        throw retireExList.get(0); // first exception is delegated
    }

    // ===================================================================================
    //                                                                         Fire Helper
    //                                                                         ===========
    protected TransactionResource beginTransaction() {
        return _cannonballHelper.help_beginTransaction();
    }

    protected void prepareBeginning() {
        _cannonballHelper.help_prepareBeginning();
    }

    protected void prepareAccessContext() {
        _cannonballHelper.help_prepareAccessContext();
    }

    protected void clearAccessContext() {
        _cannonballHelper.help_clearAccessContext();
    }

    protected void assertEquals(Object expected, Object actual) {
        _cannonballHelper.help_assertEquals(expected, actual);
    }

    protected void fail(String msg) {
        _cannonballHelper.help_fail(msg);
    }

    protected void log(Object... msgs) {
        _cannonballHelper.help_log(msgs);
    }

    protected String ln() {
        return _cannonballHelper.help_ln();
    }
}
