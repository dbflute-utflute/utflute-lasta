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
package org.dbflute.utflute.core;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.dbflute.hook.AccessContext;
import org.dbflute.hook.CallbackContext;
import org.dbflute.hook.SqlResultHandler;
import org.dbflute.hook.SqlResultInfo;
import org.dbflute.system.DBFluteSystem;
import org.dbflute.system.provider.DfCurrentDateProvider;
import org.dbflute.utflute.core.beanorder.BeanOrderValidator;
import org.dbflute.utflute.core.beanorder.ExpectedBeanOrderBy;
import org.dbflute.utflute.core.cannonball.CannonballDirector;
import org.dbflute.utflute.core.cannonball.CannonballOption;
import org.dbflute.utflute.core.cannonball.CannonballRun;
import org.dbflute.utflute.core.cannonball.CannonballStaff;
import org.dbflute.utflute.core.dbflute.GatheredExecutedSqlHolder;
import org.dbflute.utflute.core.exception.ExceptionExaminer;
import org.dbflute.utflute.core.exception.ExceptionExpectationAfter;
import org.dbflute.utflute.core.filesystem.FileLineHandler;
import org.dbflute.utflute.core.filesystem.FilesystemPlayer;
import org.dbflute.utflute.core.markhere.MarkHereManager;
import org.dbflute.utflute.core.policestory.PoliceStory;
import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassHandler;
import org.dbflute.utflute.core.policestory.jspfile.PoliceStoryJspFileHandler;
import org.dbflute.utflute.core.policestory.miscfile.PoliceStoryMiscFileHandler;
import org.dbflute.utflute.core.policestory.pjresource.PoliceStoryProjectResourceHandler;
import org.dbflute.utflute.core.policestory.webresource.PoliceStoryWebResourceHandler;
import org.dbflute.utflute.core.transaction.TransactionPerformFailureException;
import org.dbflute.utflute.core.transaction.TransactionPerformer;
import org.dbflute.utflute.core.transaction.TransactionResource;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfResourceUtil;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 0.1.0 (2011/07/24 Sunday)
 */
public abstract class PlainTestCase {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /** The logger instance for sub class. (NotNull) */
    private final Logger _xlogger = LoggerFactory.getLogger(getClass());
    // UTFlute wants to use logger for caller output
    // but should remove the dependency to Log4j
    // (logging through commons-logging gives us fixed caller...)
    //protected final Logger _xlogger = Logger.getLogger(getClass());

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The method name of test. (NullAllowed: before preparation) */
    private String _xtestMethodName;

    /** The manager of mark here. (NullAllowed: lazy-loaded) */
    private MarkHereManager _xmarkHereManager;

    /** The reserved title for logging test case beginning. (NullAllowed: before preparation or already showed) */
    private String _xreservedTitle;

    /** Does it use gatheredExecutedSql in this test case? */
    private boolean _xuseGatheredExecutedSql;

    /** Does it use switchedCurrentDate in this test case? */
    private boolean _xuseSwitchedCurrentDate;

    // ===================================================================================
    //                                                                            Settings
    //                                                                            ========
    @BeforeEach
    protected void setUp(TestInfo testInfo) throws Exception {
        xkeepTestMethodName(testInfo);
        xreserveShowTitle();
        if (!xisSuppressTestCaseAccessContext()) {
            initializeTestCaseAccessContext();
        }
    }

    protected void xkeepTestMethodName(TestInfo testInfo) {
        _xtestMethodName = testInfo.getTestMethod().map(md -> md.getName()).orElse("unknown");
    }

    protected void xreserveShowTitle() {
        // lazy-logging (no logging test case, no title)
        _xreservedTitle = "<<< " + xgetCaseDisp() + " >>>";
    }

    protected String xgetCaseDisp() {
        return getClass().getSimpleName() + "." + getTestMethodName() + "()";
    }

    // TODO jflute how do I do in junit5? (or needed?) (2020/06/15)
    //    protected void runTest() throws Throwable {
    //        try {
    //            super.runTest();
    //            postTest();
    //        } catch (Throwable e) { // to record in application log
    //            log("Failed to finish the test: " + xgetCaseDisp(), e);
    //            throw e;
    //        }
    //    }
    //
    //    protected void postTest() {
    //    }

    @AfterEach
    protected void tearDown() throws Exception {
        xclearAccessContextOnThread();
        xclearGatheredExecutedSql();
        xclearSwitchedCurrentDate();
        xclearMark(); // last process to be able to be used in tearDown()
    }

    // -----------------------------------------------------
    //                                            Basic Info
    //                                            ----------
    protected Method getTestMethod() {
        String methodName = getTestMethodName();
        try {
            return getClass().getMethod(methodName, (Class[]) null);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException("Not found the method: " + methodName, e);
        }
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    // -----------------------------------------------------
    //                                             Exception
    //                                             ---------
    /**
     * Assert that the callback throws the exception.
     * <pre>
     * String <span style="color: #553000">str</span> = <span style="color: #70226C">null</span>;
     * <span style="color: #CC4747">assertException</span>(NullPointerException.<span style="color: #70226C">class</span>, () <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> <span style="color: #553000">str</span>.toString());
     * 
     * <span style="color: #CC4747">assertException</span>(NullPointerException.<span style="color: #70226C">class</span>, () <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> <span style="color: #553000">str</span>.toString()).<span style="color: #994747">handle</span>(<span style="color: #553000">cause</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     assertContains(<span style="color: #553000">cause</span>.getMessage(), ...);
     * });
     * </pre>
     * @param <CAUSE> The type of expected cause exception. 
     * @param exceptionType The expected exception type. (NotNull)
     * @param noArgInLambda The callback for calling methods that should throw the exception. (NotNull)
     * @return The after object that has handler of expected cause for chain call. (NotNull) 
     */
    protected <CAUSE extends Throwable> ExceptionExpectationAfter<CAUSE> assertException(Class<CAUSE> exceptionType,
            ExceptionExaminer noArgInLambda) {
        assertNotNull(exceptionType);
        final String expected = exceptionType.getSimpleName();
        Throwable cause = null;
        try {
            noArgInLambda.examine();
        } catch (Throwable e) {
            cause = e;
            final Class<? extends Throwable> causeClass = cause.getClass();
            final String exp = buildExceptionSimpleExp(cause);
            if (!exceptionType.isAssignableFrom(causeClass)) {
                final String actual = causeClass.getSimpleName();
                log("*Different exception, expected: {} but...", exceptionType.getName(), cause);
                fail("*Different exception, expected: " + expected + " but: " + actual + " => " + exp);
            } else {
                log("expected: " + exp);
            }
        }
        if (cause == null) {
            fail("*No exception, expected: " + expected);
        }
        @SuppressWarnings("unchecked")
        final CAUSE castCause = (CAUSE) cause;
        return new ExceptionExpectationAfter<CAUSE>(castCause);
    }

    private String buildExceptionSimpleExp(Throwable cause) {
        final StringBuilder sb = new StringBuilder();
        final String firstMsg = cause.getMessage();
        boolean line = firstMsg != null && firstMsg.contains(ln());
        sb.append("(").append(cause.getClass().getSimpleName()).append(")").append(firstMsg);
        final Throwable secondCause = cause.getCause();
        if (secondCause != null) {
            final String secondMsg = secondCause.getMessage();
            line = line || secondMsg != null && secondMsg.contains(ln());
            sb.append(line ? ln() : " / ");
            sb.append("(").append(secondCause.getClass().getSimpleName()).append(")").append(secondMsg);
            final Throwable thirdCause = secondCause.getCause();
            if (thirdCause != null) {
                final String thirdMsg = thirdCause.getMessage();
                line = line || thirdMsg != null && thirdMsg.contains(ln());
                sb.append(line ? ln() : " / ");
                sb.append("(").append(thirdCause.getClass().getSimpleName()).append(")").append(thirdMsg);
            }
        }
        final String whole = sb.toString();
        return (whole.contains(ln()) ? ln() : "") + whole;
    }

    // -----------------------------------------------------
    //                                                 Order
    //                                                 -----
    /**
     * Assert that the bean list is ordered as expected specification.
     * <pre>
     * assertOrder(memberList, orderBy -&gt; {
     *     orderBy.desc(mb -&gt; mb.getBirthdate()).asc(mb -&gt; mb.getMemberId());
     * });
     * </pre>
     * @param <BEAN> The type of element of ordered list.
     * @param beanList The list of bean. (NotNull)
     * @param oneArgLambda The callback for order specification. (NotNull)
     */
    protected <BEAN> void assertOrder(List<BEAN> beanList, Consumer<ExpectedBeanOrderBy<BEAN>> oneArgLambda) {
        assertNotNull(beanList);
        assertNotNull(oneArgLambda);
        assertFalse(beanList.isEmpty());
        new BeanOrderValidator<BEAN>(oneArgLambda).validateOrder(beanList, vio -> {
            fail("[Order Failure] " + vio); // for now
        });
    }

    // -----------------------------------------------------
    //                                             Mark Here
    //                                             ---------
    /**
     * Mark here to assert that it goes through the road.
     * <pre>
     * memberBhv.selectCursor(<span style="color: #553000">cb</span> -&gt; ..., entity -&gt; {
     *     <span style="color: #FD4747">markHere</span>("cursor");
     * });
     * <span style="color: #994747">assertMarked</span>("cursor"); <span style="color: #3F7E5E">// the callback called</span>
     * </pre>
     * @param mark The your original mark expression as string. (NotNull)
     */
    protected void markHere(String mark) {
        assertNotNull(mark);
        xgetMarkHereManager().mark(mark);
    }

    /**
     * Assert the mark is marked. (found in existing marks)
     * <pre>
     * memberBhv.selectCursor(<span style="color: #553000">cb</span> -&gt; ..., entity -&gt; {
     *     <span style="color: #994747">markHere</span>("cursor");
     * });
     * <span style="color: #FD4747">assertMarked</span>("cursor"); <span style="color: #3F7E5E">// the callback called</span>
     * </pre>
     * @param mark The your original mark expression as string. (NotNull)
     */
    protected void assertMarked(String mark) {
        assertNotNull(mark);
        xgetMarkHereManager().assertMarked(mark);
    }

    /**
     * Is the mark marked? (found the mark in existing marks?)
     * @param mark The your original mark expression as string. (NotNull)
     * @return The determination, true or false.
     */
    protected boolean isMarked(String mark) {
        assertNotNull(mark);
        return xgetMarkHereManager().isMarked(mark);
    }

    protected MarkHereManager xgetMarkHereManager() {
        if (_xmarkHereManager == null) {
            _xmarkHereManager = new MarkHereManager();
        }
        return _xmarkHereManager;
    }

    protected boolean xhasMarkHereManager() {
        return _xmarkHereManager != null;
    }

    protected void xdestroyMarkHereManager() {
        _xmarkHereManager = null;
    }

    protected void xclearMark() {
        if (xhasMarkHereManager()) {
            xgetMarkHereManager().checkNonAssertedMark();
            xgetMarkHereManager().clearMarkMap();
            xdestroyMarkHereManager();
        }
    }

    // ===================================================================================
    //                                                                      Logging Helper
    //                                                                      ==============
    /**
     * Log the messages. <br>
     * If you set an exception object to the last element, it shows stack traces.
     * <pre>
     * Member member = ...;
     * <span style="color: #FD4747">log</span>(member.getMemberName(), member.getBirthdate());
     * <span style="color: #3F7E5E">// -&gt; Stojkovic, 1965/03/03</span>
     *
     * Exception e = ...;
     * <span style="color: #FD4747">log</span>(member.getMemberName(), member.getBirthdate(), e);
     * <span style="color: #3F7E5E">// -&gt; Stojkovic, 1965/03/03</span>
     * <span style="color: #3F7E5E">//  (and stack traces)</span>
     * </pre>
     * @param msgs The array of messages. (NotNull)
     */
    protected void log(Object... msgs) {
        if (msgs == null) {
            throw new IllegalArgumentException("The argument 'msgs' should not be null.");
        }
        Throwable cause = null;
        final int arrayLength = msgs.length;
        if (arrayLength > 0) {
            final Object lastElement = msgs[arrayLength - 1];
            if (lastElement instanceof Throwable) {
                cause = (Throwable) lastElement;
            }
        }
        final StringBuilder sb = new StringBuilder();
        int index = 0;
        int skipCount = 0;
        for (Object msg : msgs) {
            if (index == arrayLength - 1 && cause != null) { // last loop and it is cause
                break;
            }
            if (skipCount > 0) { // already resolved as variable
                --skipCount; // until count zero
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            final String appended;
            if (msg instanceof Timestamp) {
                appended = DfTypeUtil.toString(msg, "yyyy/MM/dd HH:mm:ss.SSS");
            } else if (msg instanceof Date) {
                appended = DfTypeUtil.toString(msg, "yyyy/MM/dd");
            } else {
                String strMsg = msg != null ? msg.toString() : null;
                int nextIndex = index + 1;
                skipCount = 0; // just in case
                while (strMsg != null && strMsg.contains("{}")) {
                    if (arrayLength <= nextIndex) {
                        break;
                    }
                    final Object nextObj = msgs[nextIndex];
                    final String replacement;
                    if (nextObj != null) {
                        // escape two special characters of replaceFirst() to avoid illegal group reference
                        replacement = Srl.replace(Srl.replace(nextObj.toString(), "\\", "\\\\"), "$", "\\$");
                    } else {
                        replacement = "null";
                    }
                    strMsg = strMsg.replaceFirst("\\{\\}", replacement);
                    ++skipCount;
                    ++nextIndex;
                }
                appended = strMsg;
            }
            sb.append(appended);
            ++index;
        }
        final String msg = sb.toString();
        if (_xreservedTitle != null) {
            _xlogger.debug("");
            _xlogger.debug(_xreservedTitle);
            _xreservedTitle = null;
        }
        if (cause != null) {
            _xlogger.debug(msg, cause);
        } else {
            _xlogger.debug(msg);
        }
        // see comment for logger definition for the detail
        //_xlogger.log(PlainTestCase.class.getName(), Level.DEBUG, msg, cause);
    }

    // ===================================================================================
    //                                                                       System Helper
    //                                                                       =============
    /**
     * Get the line separator. (LF fixedly)
     * @return The string of the line separator. (NotNull)
     */
    protected String ln() {
        return "\n";
    }

    // ===================================================================================
    //                                                                         Transaction
    //                                                                         ===========
    // reserved interfaces
    /**
     * Begin new transaction (even if the transaction has already been begun). <br>
     * You can manually commit or roll-back at your favorite timing by returned transaction resource. <br>
     * On the other hand, you might have mistake of transaction handling. <br>
     * So, also you can use {@link #performNewTransaction(TransactionPerformer)}. (easier)
     * @return The resource of transaction, you can commit or roll-back it. (basically NotNull: if null, transaction unsupported)
     */
    protected TransactionResource beginNewTransaction() {
        // should be overridden by DI container's test case
        return null;
    }

    /**
     * Commit the specified transaction.
     * @param resource The resource of transaction provided by beginNewTransaction(). (NotNull)
     */
    protected void commitTransaction(TransactionResource resource) {
    }

    /**
     * Roll-back the specified transaction.
     * @param resource The resource of transaction provided by beginNewTransaction(). (NotNull)
     */
    protected void rollbackTransaction(TransactionResource resource) {
    }

    /**
     * Perform the process in new transaction (even if the transaction has already been begun). <br>
     * You can select commit or roll-back by returned value of the callback method.
     * <pre>
     * performNewTransaction(new TransactionPerformer() {
     *     public boolean perform() { <span style="color: #3F7E5E">// transaction scope</span>
     *         ...
     *         return false; <span style="color: #3F7E5E">// true: commit, false: roll-back</span>
     *     }
     * });
     * </pre>
     * @param performer The callback for the transaction process. (NotNull)
     * @throws TransactionPerformFailureException When the performance fails.
     */
    protected void performNewTransaction(TransactionPerformer performer) {
        assertNotNull(performer);
        final TransactionResource resource = beginNewTransaction();
        Exception cause = null;
        boolean commit = false;
        try {
            commit = performer.perform();
        } catch (RuntimeException e) {
            cause = e;
        } catch (SQLException e) {
            cause = e;
        } finally {
            if (commit && cause == null) {
                try {
                    commitTransaction(resource);
                } catch (RuntimeException e) {
                    cause = e;
                }
            } else {
                try {
                    rollbackTransaction(resource);
                } catch (RuntimeException e) {
                    if (cause != null) {
                        log(e.getMessage());
                    } else {
                        cause = e;
                    }
                }
            }
        }
        if (cause != null) {
            String msg = "Failed to perform the process in transaction: " + performer;
            throw new TransactionPerformFailureException(msg, cause);
        }
    }

    protected void xassertTransactionResourceNotNull(TransactionResource resource) {
        if (resource == null) {
            String msg = "The argument 'resource' should not be null.";
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Get the (main) data source for database.
     * @return The instance from DI container. (basically NotNull: if null, data source unsupported or cannot be resolved)
     */
    protected DataSource getDataSource() {
        // should be overridden by DI container's test case
        return null;
    }

    // ===================================================================================
    //                                                                         Cannon-ball
    //                                                                         ===========
    /**
     * Execute the cannon-ball run. (Do you know cannon-ball run?) <br>
     * Default thread count is 10, and repeat count is 1.
     * <pre>
     * <span style="color: #FD4747">cannonball</span>(new CannonballRun() {
     *     public void drive(CannonballCar car) {
     *         ... <span style="color: #3F7E5E">// 10 threads is running at the same time</span>
     *     }
     * }, new CannonballOption().expect...);
     * </pre>
     * @param run The callback for the run. (NotNull)
     * @param option The option for the run. (NotNull)
     */
    protected void cannonball(CannonballRun run, CannonballOption option) {
        assertNotNull(run);
        assertNotNull(option);
        createCannonballDirector().readyGo(run, option);
    }

    /**
     * Create the instance of cannon-ball director.
     * @return The new-created instance of the director. (NotNull)
     */
    protected CannonballDirector createCannonballDirector() {
        return newCannonballDirector(xcreateCannonballStaff());
    }

    /**
     * New the instance of cannon-ball director.
     * @param cannonballStaff The staff for cannon-ball. (NotNull)
     * @return The new-created instance of the director. (NotNull)
     */
    protected CannonballDirector newCannonballDirector(CannonballStaff cannonballStaff) { // you can override
        return new CannonballDirector(cannonballStaff);
    }

    /**
     * Create the instance of cannon-ball staff.
     * @return The new-created instance of the staff. (NotNull)
     */
    protected CannonballStaff xcreateCannonballStaff() {
        return new CannonballStaff() {

            public void help_prepareBeginning() {
                xprepareCannonballBeginning();
            }

            public void help_prepareAccessContext() {
                xprepareCannonballAccessContext();
            }

            public TransactionResource help_beginTransaction() {
                return beginNewTransaction();
            }

            public void help_clearAccessContext() {
                xclearAccessContextOnThread();
            }

            public void help_assertEquals(Object expected, Object actual) {
                assertEquals(expected, actual);
            }

            public void help_fail(String msg) {
                fail(msg);
            }

            public void help_log(Object... msgs) {
                log(msgs);
            }

            public String help_ln() {
                return "\n";
            }
        };
    }

    protected void xprepareCannonballBeginning() {
    }

    protected void xprepareCannonballAccessContext() {
        xputTestCaseAccessContextOnThread();
    }

    /**
     * Sleep the current thread.
     * @param millis The millisecond to sleep.
     */
    protected void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            String msg = "Failed to sleep but I want to sleep here...Zzz...";
            throw new IllegalStateException(msg, e);
        }
    }

    // ===================================================================================
    //                                                                        Police Story
    //                                                                        ============
    /**
     * Tell me about your police story of Java class chase. (default: '.java' files under src/main/java)
     * <pre>
     * policeStoryOfJavaClassChase(new PoliceStoryJavaClassHandler() {
     *     public void handle(File srcFile, Class&lt;?&gt; clazz) {
     *         <span style="color: #3F7E5E">// handle the class as you like it</span>
     *         <span style="color: #3F7E5E">// e.g. clazz.getMethods(), readLine(srcFile, ...)</span>
     *     }
     * });
     * </pre>
     * @param handler The handler of Java class. (NotNull)
     */
    public void policeStoryOfJavaClassChase(PoliceStoryJavaClassHandler handler) {
        assertNotNull(handler);
        createPoliceStory().chaseJavaClass(handler);
    }

    /**
     * Tell me about your police story of JSP file chase. (default: '.jsp' files under src/main/webapp)
     * <pre>
     * policeStoryOfJspFileChase(new PoliceStoryJspFileHandler() {
     *     public void handle(File jspFile) {
     *         <span style="color: #3F7E5E">// handle the class as you like it</span>
     *         <span style="color: #3F7E5E">// e.g. readLine(jspFile, ...)</span>
     *     }
     * });
     * </pre>
     * @param handler The handler of JSP file. (NotNull)
     */
    public void policeStoryOfJspFileChase(PoliceStoryJspFileHandler handler) {
        assertNotNull(handler);
        createPoliceStory().chaseJspFile(handler);
    }

    /**
     * Tell me about your police story of miscellaneous resource chase.
     * <pre>
     * policeStoryOfMiscFileChase(new PoliceStoryMiscFileHandler() {
     *     public void handle(File miscFile) {
     *         <span style="color: #3F7E5E">// handle the class as you like it</span>
     *         <span style="color: #3F7E5E">// e.g. readLine(miscFile, ...)</span>
     *     }
     * }, miscDir); <span style="color: #3F7E5E">// you can specify base directory of file</span>
     * </pre>
     * @param handler The handler of miscellaneous resource. (NotNull)
     * @param baseDir The base directory for the miscellaneous file. (NotNull)
     */
    public void policeStoryOfMiscFileChase(PoliceStoryMiscFileHandler handler, File baseDir) {
        assertNotNull(handler);
        assertNotNull(baseDir);
        createPoliceStory().chaseMiscFile(handler, baseDir);
    }

    /**
     * Tell me about your police story of project resource chase. (default: under target/test-classes/../../)
     * <pre>
     * policeStoryOfProjectResourceChase(new PoliceStoryProjectResourceHandler() {
     *     public void handle(File resourceFile) {
     *         <span style="color: #3F7E5E">// handle the class as you like it</span>
     *         <span style="color: #3F7E5E">// e.g. readLine(resourceFile, ...)</span>
     *     }
     * });
     * </pre>
     * @param handler The handler of project resource. (NotNull)
     */
    public void policeStoryOfProjectResourceChase(PoliceStoryProjectResourceHandler handler) {
        assertNotNull(handler);
        createPoliceStory().chaseProjectResource(handler);
    }

    /**
     * Tell me about your police story of web resource chase. (default: under src/main/webapp)
     * <pre>
     * policeStoryOfWebResourceChase(new PoliceStoryWebResourceHandler() {
     *     public void handle(File resourceFile) {
     *         <span style="color: #3F7E5E">// handle the class as you like it</span>
     *         <span style="color: #3F7E5E">// e.g. readLine(resourceFile, ...)</span>
     *     }
     * });
     * </pre>
     * @param handler The handler of web resource. (NotNull)
     */
    public void policeStoryOfWebResourceChase(PoliceStoryWebResourceHandler handler) {
        assertNotNull(handler);
        createPoliceStory().chaseWebResource(handler);
    }

    /**
     * Create the instance of police story for many story.
     * @return The new-created instance of the police story. (NotNull)
     */
    protected PoliceStory createPoliceStory() {
        return newPoliceStory(this, getProjectDir());
    }

    /**
     * New the instance of police story for many story.
     * @param testCase The instsance of test case, basically this. (NotNull)
     * @param projectDir The root directory of project. (NotNull)
     * @return The new-created instance of the police story. (NotNull)
     */
    protected PoliceStory newPoliceStory(Object testCase, File projectDir) { // you can override
        return new PoliceStory(testCase, projectDir);
    }

    // ===================================================================================
    //                                                                          Filesystem
    //                                                                          ==========
    /**
     * Read the line of the text file.
     * @param textFile The file object of text. (NotNull)
     * @param encoding The encoding of the file. (NotNull)
     * @param handler The handler of line string for the text file. (NotNull)
     * @throws IllegalStateException When it fails to read the text file.
     */
    protected void readLine(File textFile, String encoding, FileLineHandler handler) {
        assertNotNull(textFile);
        assertNotNull(encoding);
        assertNotNull(handler);
        final FilesystemPlayer reader = createFilesystemPlayer();
        reader.readLine(textFile, encoding, handler);
    }

    /**
     * Create the filesystem player for e.g. reading line.
     * @return The new-created instance of the player. (NotNull)
     */
    protected FilesystemPlayer createFilesystemPlayer() { // you can override
        return new FilesystemPlayer();
    }

    /**
     * Get the directory object of the (application or Eclipse) project.
     * @return The file object of the directory. (NotNull)
     */
    protected File getProjectDir() { // you can override
        final Set<String> markSet = defineProjectDirMarkSet();
        for (File dir = getTestCaseBuildDir(); dir != null; dir = dir.getParentFile()) {
            if (dir.isDirectory()) {
                if (Arrays.stream(dir.listFiles()).anyMatch(file -> markSet.contains(file.getName()))) {
                    return dir;
                }
            }
        }
        throw new IllegalStateException("Not found the project dir marks: " + markSet);
    }

    /**
     * Define the marks of the (application or Eclipse) project.
     * @return the set of mark file name for the project. (NotNull)
     */
    protected Set<String> defineProjectDirMarkSet() {
        return DfCollectionUtil.newHashSet("build.xml", "pom.xml", "build.gradle", ".project", ".idea");
    }

    /**
     * Get the directory object of the build for the test case. (default: target/test-classes)
     * @return The file object of the directory. (NotNull)
     */
    protected File getTestCaseBuildDir() {
        return DfResourceUtil.getBuildDir(getClass()); // target/test-classes
    }

    // ===================================================================================
    //                                                                             DBFlute
    //                                                                             =======
    // -----------------------------------------------------
    //                                         AccessContext
    //                                         -------------
    protected boolean xisSuppressTestCaseAccessContext() {
        return false;
    }

    protected void initializeTestCaseAccessContext() {
        xputTestCaseAccessContextOnThread();
    }

    protected void xputTestCaseAccessContextOnThread() {
        AccessContext.setAccessContextOnThread(createTestCaseAccessContext());
    }

    protected AccessContext createTestCaseAccessContext() {
        final AccessContext context = new AccessContext();
        context.setAccessLocalDate(DBFluteSystem.currentLocalDate());
        context.setAccessLocalDateTime(DBFluteSystem.currentLocalDateTime());
        context.setAccessTimestamp(DBFluteSystem.currentTimestamp());
        context.setAccessDate(DBFluteSystem.currentDate());
        context.setAccessUser(Thread.currentThread().getName());
        context.setAccessProcess(getClass().getSimpleName());
        context.setAccessModule(getClass().getSimpleName());
        return context;
    }

    /**
     * Get the access context for common column auto setup of DBFlute.
     * @return The instance of access context on the thread. (basically NotNull)
     */
    protected AccessContext getAccessContext() { // user method
        return AccessContext.getAccessContextOnThread();
    }

    protected void xclearAccessContextOnThread() {
        AccessContext.clearAccessContextOnThread();
    }

    // -----------------------------------------------------
    //                                       CallbackContext
    //                                       ---------------
    protected GatheredExecutedSqlHolder gatherExecutedSql() {
        _xuseGatheredExecutedSql = true;
        final GatheredExecutedSqlHolder holder = new GatheredExecutedSqlHolder();
        CallbackContext.setSqlResultHandlerOnThread(new SqlResultHandler() {
            public void handle(SqlResultInfo info) {
                holder.addSqlResultInfo(info);
            }
        });
        return holder;
    }

    protected void xclearGatheredExecutedSql() {
        if (_xuseGatheredExecutedSql) {
            CallbackContext.clearSqlResultHandlerOnThread();
        }
    }

    // -----------------------------------------------------
    //                                         DBFluteSystem
    //                                         -------------
    protected void switchCurrentDate(Supplier<LocalDateTime> dateTimeSupplier) {
        assertNotNull(dateTimeSupplier);
        if (DBFluteSystem.hasCurrentDateProvider()) {
            String msg = "The current date provider already exists, cannot use new provider: " + dateTimeSupplier;
            throw new IllegalStateException(msg);
        }
        _xuseSwitchedCurrentDate = true;
        DBFluteSystem.unlock();
        DBFluteSystem.setCurrentDateProvider(new DfCurrentDateProvider() {
            public long currentTimeMillis() {
                final LocalDateTime currentDateTime = dateTimeSupplier.get();
                assertNotNull(currentDateTime);
                return DfTypeUtil.toDate(currentDateTime).getTime();
            }
        });
    }

    protected void xclearSwitchedCurrentDate() {
        if (_xuseSwitchedCurrentDate) {
            DBFluteSystem.unlock();
            DBFluteSystem.setCurrentDateProvider(null);
        }
    }

    // ===================================================================================
    //                                                                          Compatible
    //                                                                          ==========
    private void assertEquals(Object expected, Object actual) { // for compatible
        Assertions.assertEquals(expected, actual);
    }

    private void assertNotNull(Object actual) { // for compatible
        Assertions.assertNotNull(actual);
    }

    private void assertFalse(boolean condition) { // for compatible
        Assertions.assertFalse(condition);
    }

    private void fail(String msg) { // for compatible
        Assertions.fail(msg);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    protected String getTestMethodName() {
        return _xtestMethodName; // not null after preparation
    }

    protected Logger xgetLogger() {
        return _xlogger;
    }

    protected String xgetReservedTitle() {
        return _xreservedTitle;
    }

    protected void xsetReservedTitle(String reservedTitle) {
        _xreservedTitle = reservedTitle;
    }

    public boolean xisUseSwitchedCurrentDate() {
        return _xuseSwitchedCurrentDate;
    }
}
