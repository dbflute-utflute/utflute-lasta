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
package org.dbflute.utflute.lastadi;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.dbflute.utflute.lastaflute.mail.MailMessageAssertion;
import org.dbflute.utflute.lastaflute.mail.TestingMailData;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.FwCoreDirection;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.magic.TransactionTimeContext;
import org.lastaflute.core.magic.destructive.BowgunDestructiveAdjuster;
import org.lastaflute.core.time.SimpleTimeManager;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.web.response.JsonResponse;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public abstract class ContainerTestCase extends LastaDiTestCase {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static Boolean _xexistsLastaJob; // lazy-loaded for performance
    protected static boolean _xjobSchedulingSuppressed;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The (main) data source for database. (NotNull: after injection) */
    @Resource
    private DataSource _xdataSource;

    @Resource
    private FwAssistantDirector _assistantDirector;
    @Resource
    private TimeManager _timeManager;
    @Resource
    private JsonManager _jsonManager;

    // -----------------------------------------------------
    //                                       Mail Validation
    //                                       ---------------
    private MailMessageAssertion _xmailMessageAssertion;

    // ===================================================================================
    //                                                                            Settings
    //                                                                            ========
    @Override
    public void setUp() throws Exception {
        xsuppressJobSchedulingIfNeeds();
        super.setUp();
        if (isUseJobScheduling()) {
            xrebootJobSchedulingIfNeeds();
        }
        initializeAssistantDirector(); // nearly actual timing
        initializeThreadCacheContext(); // me too
    }

    protected void initializeAssistantDirector() {
        final FwCoreDirection direction = _assistantDirector.assistCoreDirection();
        direction.assistCurtainBeforeHook().hook(_assistantDirector);
    }

    protected void initializeThreadCacheContext() {
        ThreadCacheContext.initialize();
    }

    @Override
    protected void postTest() {
        super.postTest();
        xprocessMailAssertion();
    }

    @Override
    public void tearDown() throws Exception {
        ThreadCacheContext.clear();
        if (BowgunDestructiveAdjuster.hasAnyBowgun()) {
            BowgunDestructiveAdjuster.unlock();
            BowgunDestructiveAdjuster.restoreBowgunAll();
        }
        xdestroyJobSchedulingIfNeeds(); // always destroy if scheduled to avoid job trouble
        super.tearDown();
    }

    // -----------------------------------------------------
    //                                     Begin Transaction
    //                                     -----------------
    @Override
    protected void xbeginTestCaseTransaction() {
        initializePreparedAccessContext(); // nearly actual timing
        initializeTransactionTime(); // me too
        super.xbeginTestCaseTransaction();
    }

    protected void initializeTransactionTime() {
        // because of non-UserTransaction transaction in UTFlute
        final Date transactionTime = _timeManager.flashDate();
        TransactionTimeContext.setTransactionTime(transactionTime);
    }

    protected void initializePreparedAccessContext() {
        // though non-UserTransaction, for e.g. transaction in asynchronous
        PreparedAccessContext.setAccessContextOnThread(getAccessContext()); // inherit one of test case
    }

    // -----------------------------------------------------
    //                                       End Transaction
    //                                       ---------------
    @Override
    protected void xrollbackTestCaseTransaction() {
        super.xrollbackTestCaseTransaction();
        TransactionTimeContext.clear();
        PreparedAccessContext.clearAccessContextOnThread();
    }

    // ===================================================================================
    //                                                                       JSON Handling
    //                                                                       =============
    /**
     * Show JSON string for the JSON bean.
     * @param jsonBean The JSON bean to be serialized. (NotNull)
     */
    protected void showJson(Object jsonBean) {
        log(jsonBean.getClass().getSimpleName() + ":" + ln() + toJson(jsonBean));
    }

    /**
     * Convert to JSON string from the JSON bean.
     * @param jsonBean The JSON bean to be serialized. (NotNull)
     * @return The JSON string. (NotNull)
     */
    protected String toJson(Object jsonBean) {
        final Object realBean;
        if (jsonBean instanceof JsonResponse<?>) {
            realBean = ((JsonResponse<?>) jsonBean).getJsonBean();
        } else {
            realBean = jsonBean;
        }
        return _jsonManager.toJson(realBean);
    }

    // ===================================================================================
    //                                                                      Mail Assertion
    //                                                                      ==============
    /**
     * Reserve mail assertion, should be called before action execution.
     * <pre>
     * <span style="color: #3F7E5E">// ## Arrange ##</span>
     * SignupAction <span style="color: #553000">action</span> = <span style="color: #70226C">new</span> SignupAction();
     * inject(<span style="color: #553000">action</span>);
     * SignupForm <span style="color: #553000">form</span> = <span style="color: #70226C">new</span> SignupForm();
     * <span style="color: #CC4747">reserveMailAssertion</span>(<span style="color: #553000">data</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #553000">data</span>.required(<span style="color: #994747">WelcomeMemberPostcard</span>.<span style="color: #70226C">class</span>).forEach(<span style="color: #553000">message</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *        <span style="color: #553000">message</span>.requiredToList().forEach(<span style="color: #553000">addr</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *            assertContains(<span style="color: #553000">addr</span>.getAddress(), <span style="color: #553000">form</span>.memberAccount); <span style="color: #3F7E5E">// e.g. land@docksidestage.org</span>
     *        });
     *        <span style="color: #553000">message</span>.assertPlainTextContains(<span style="color: #553000">form</span>.memberName);
     *        <span style="color: #553000">message</span>.assertPlainTextContains(<span style="color: #553000">form</span>.memberAccount);
     *     });
     * });
     *
     * <span style="color: #3F7E5E">// ## Act ##</span>
     * HtmlResponse <span style="color: #553000">response</span> = <span style="color: #553000">action</span>.signup(<span style="color: #553000">form</span>);
     * ...
     * </pre>
     * @param dataLambda The callback for mail data. (NotNull)
     */
    protected void reserveMailAssertion(Consumer<TestingMailData> dataLambda) {
        _xmailMessageAssertion = new MailMessageAssertion(dataLambda);
    }

    protected void xprocessMailAssertion() {
        if (_xmailMessageAssertion != null) {
            _xmailMessageAssertion.assertMailData();
            _xmailMessageAssertion = null;
        }
    }

    // ===================================================================================
    //                                                                         Destructive
    //                                                                         ===========
    // -----------------------------------------------------
    //                                          Asynchronous
    //                                          ------------
    /**
     * Change asynchronous process to normal synchronous, to be easy to assert. <br>
     * (Invalidate AsyncManager)
     * <pre>
     * <span style="color: #3F7E5E">// ## Arrange ##</span>
     * <span style="color: #CC4747">changeAsyncToNormalSync()</span>;
     * ...
     * </pre>
     */
    protected void changeAsyncToNormalSync() {
        BowgunDestructiveAdjuster.unlock();
        BowgunDestructiveAdjuster.shootBowgunAsyncToNormalSync();
    }

    /**
     * Restore asynchronous process to normal synchronous. <br>
     * (async() is executed as asynchronous)
     */
    protected void restoreAsyncToNormalSync() {
        BowgunDestructiveAdjuster.unlock();
        BowgunDestructiveAdjuster.restoreBowgunAsyncToNormalSync();
    }

    // -----------------------------------------------------
    //                                           Transaction
    //                                           -----------
    /**
     * Change requires-new transaction to required transaction, to be easy to assert. <br>
     * (All transactions can be in test transaction)
     * <pre>
     * <span style="color: #3F7E5E">// ## Arrange ##</span>
     * <span style="color: #CC4747">changeRequiresNewToRequired()</span>;
     * ...
     * </pre>
     */
    protected void changeRequiresNewToRequired() {
        BowgunDestructiveAdjuster.unlock();
        BowgunDestructiveAdjuster.shootBowgunRequiresNewToRequired();
    }

    /**
     * Restore requires-new transaction to required transaction. <br>
     * (requiresNew() is executed as requires-new)
     */
    protected void restoreRequiresNewToRequired() {
        BowgunDestructiveAdjuster.unlock();
        BowgunDestructiveAdjuster.restoreBowgunRequiresNewToRequired();
    }

    // -----------------------------------------------------
    //                                          Current Date
    //                                          ------------
    // to be geared with LastaFlute
    @Override
    protected void switchCurrentDate(Supplier<LocalDateTime> dateTimeSupplier) {
        super.switchCurrentDate(dateTimeSupplier);
        SimpleTimeManager.shootBowgunCurrentTimeProvider(() -> {
            return DfTypeUtil.toDate(dateTimeSupplier.get()).getTime();
        });
    }

    @Override
    protected void xclearSwitchedCurrentDate() {
        if (xisUseSwitchedCurrentDate()) {
            SimpleTimeManager.shootBowgunCurrentTimeProvider(null);
        }
        super.xclearSwitchedCurrentDate();
    }

    // ===================================================================================
    //                                                                            LastaJob
    //                                                                            ========
    protected void xsuppressJobSchedulingIfNeeds() {
        if (!xexistsLastaJob()) {
            return;
        }
        if (_xjobSchedulingSuppressed) { // to avoid duplicate calls when batch execution of unit test
            return;
        }
        _xjobSchedulingSuppressed = true;
        try {
            // reflection on parade not to depends on LastaJob library
            final Class<?> jobManagerType = Class.forName("org.lastaflute.job.SimpleJobManager");
            final Method unlockMethod = jobManagerType.getMethod("unlock", (Class[]) null);
            unlockMethod.invoke(null, (Object[]) null);
            final Method shootMethod = jobManagerType.getMethod("shootBowgunEmptyScheduling", (Class[]) null);
            shootMethod.invoke(null, (Object[]) null);
        } catch (Exception continued) {
            log("*Failed to suppress job scheduling", continued);
        }
    }

    protected boolean isUseJobScheduling() { // you can override, for e.g. heavy scheduling (using e.g. DB)
        return false; // you can set true only when including LastaJob
    }

    protected void xrebootJobSchedulingIfNeeds() { // called when isUseJobScheduling()
        if (!xexistsLastaJob()) {
            return;
        }
        try {
            // reflection on parade not to depends on LastaJob library
            final Class<?> jobManagerType = xforNameJobManager();
            final Object jobManager = getComponent(jobManagerType);
            if (!xisJobSchedulingDone(jobManagerType, jobManager)) {
                xcallNoArgInstanceJobMethod(jobManagerType, jobManager, "reboot");
            }
        } catch (Exception continued) {
            log("*Failed to reboot job scheduling", continued);
        }
    }

    protected void xdestroyJobSchedulingIfNeeds() { // always called from tearDown()
        if (!xexistsLastaJob()) {
            return;
        }
        try {
            // reflection on parade not to depends on LastaJob library
            final Class<?> jobManagerType = xforNameJobManager();
            if (hasComponent(jobManagerType)) { // e.g. in classpath and include lasta_job.xml
                final Object jobManager = getComponent(jobManagerType);
                if (xisJobSchedulingDone(jobManagerType, jobManager)) {
                    xcallNoArgInstanceJobMethod(jobManagerType, jobManager, "destroy");
                }
            }
        } catch (Exception continued) {
            log("*Failed to destroy job scheduling", continued);
        }
    }

    protected static Class<?> xforNameJobManager() throws ClassNotFoundException {
        return Class.forName("org.lastaflute.job.JobManager");
    }

    protected boolean xisJobSchedulingDone(Class<?> jobManagerType, Object jobManager) throws ReflectiveOperationException {
        return (boolean) xcallNoArgInstanceJobMethod(jobManagerType, jobManager, "isSchedulingDone");
    }

    private Object xcallNoArgInstanceJobMethod(Class<?> jobManagerType, Object jobManager, String methodName)
            throws ReflectiveOperationException {
        final Method rebootMethod = jobManagerType.getMethod(methodName, (Class[]) null);
        return rebootMethod.invoke(jobManager, (Object[]) null);
    }

    protected boolean xexistsLastaJob() {
        if (_xexistsLastaJob != null) {
            return _xexistsLastaJob;
        }
        try {
            xforNameJobManager();
            _xexistsLastaJob = true;
            // this method is called outside container so cannot determine it
            //_xexistsLastaJob = hasComponent(jobManagerType);
        } catch (ClassNotFoundException e) {
            _xexistsLastaJob = false;
        }
        return _xexistsLastaJob;
    }

    // ===================================================================================
    //                                                                         JDBC Helper
    //                                                                         ===========
    /** {@inheritDoc} */
    @Override
    protected DataSource getDataSource() { // user method
        return _xdataSource;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    protected MailMessageAssertion xgetMailMessageValidator() {
        return _xmailMessageAssertion;
    }

    protected void xsetMailMessageValidator(MailMessageAssertion xmailMessageValidator) {
        _xmailMessageAssertion = xmailMessageValidator;
    }
}
