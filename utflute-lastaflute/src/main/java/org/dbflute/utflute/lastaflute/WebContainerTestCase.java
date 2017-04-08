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
package org.dbflute.utflute.lastaflute;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Resource;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.helper.function.IndependentProcessor;
import org.dbflute.utflute.lastadi.ContainerTestCase;
import org.dbflute.utflute.lastaflute.mail.MailMessageAssertion;
import org.dbflute.utflute.lastaflute.mail.TestingMailData;
import org.dbflute.utflute.lastaflute.mock.MockResopnseBeanValidator;
import org.dbflute.utflute.lastaflute.mock.MockRuntimeFactory;
import org.dbflute.utflute.lastaflute.mock.TestingHtmlData;
import org.dbflute.utflute.lastaflute.mock.TestingJsonData;
import org.dbflute.utflute.lastaflute.validation.TestingValidationErrorAfter;
import org.dbflute.utflute.mocklet.MockletHttpServletRequest;
import org.dbflute.utflute.mocklet.MockletHttpServletRequestImpl;
import org.dbflute.utflute.mocklet.MockletHttpServletResponse;
import org.dbflute.utflute.mocklet.MockletHttpServletResponseImpl;
import org.dbflute.utflute.mocklet.MockletHttpSession;
import org.dbflute.utflute.mocklet.MockletServletConfig;
import org.dbflute.utflute.mocklet.MockletServletConfigImpl;
import org.dbflute.utflute.mocklet.MockletServletContext;
import org.dbflute.utflute.mocklet.MockletServletContextImpl;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.magic.TransactionTimeContext;
import org.lastaflute.core.magic.destructive.BowgunDestructiveAdjuster;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.di.core.ExternalContext;
import org.lastaflute.di.core.LaContainer;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.doc.DocumentGenerator;
import org.lastaflute.web.LastaFilter;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.token.DoubleSubmitManager;
import org.lastaflute.web.token.DoubleSubmitTokenMap;
import org.lastaflute.web.validation.exception.ValidationErrorException;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public abstract class WebContainerTestCase extends ContainerTestCase {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The cached configuration of servlet. (NullAllowed: when no web mock or beginning or ending) */
    private static MockletServletConfig _xcachedServletConfig;

    // -----------------------------------------------------
    //                                              Web Mock
    //                                              --------
    /** The mock request of the test case execution. (NullAllowed: when no web mock or beginning or ending) */
    private MockletHttpServletRequest _xmockRequest;

    /** The mock response of the test case execution. (NullAllowed: when no web mock or beginning or ending) */
    private MockletHttpServletResponse _xmockResponse;

    // -----------------------------------------------------
    //                                       Mail Validation
    //                                       ---------------
    private MailMessageAssertion _xmailMessageAssertion;

    // -----------------------------------------------------
    //                                  LastaFlute Component
    //                                  --------------------
    @Resource
    private FwAssistantDirector _assistantDirector;
    @Resource
    private MessageManager _messageManager;
    @Resource
    private TimeManager _timeManager;
    @Resource
    private JsonManager _jsonManager;
    @Resource
    private RequestManager _requestManager;
    @Resource
    private DoubleSubmitManager _doubleSubmitManager;

    // ===================================================================================
    //                                                                            Settings
    //                                                                            ========
    @Override
    public void setUp() throws Exception {
        if (isSuppressJobScheduling()) {
            xsuppressJobScheduling();
        }
        super.setUp();
    }

    @Override
    protected void postTest() {
        super.postTest();
        xprocessMailAssertion();
    }

    @Override
    public void tearDown() throws Exception {
        if (BowgunDestructiveAdjuster.hasAnyBowgun()) {
            BowgunDestructiveAdjuster.unlock();
            BowgunDestructiveAdjuster.restoreBowgunAll();
        }
        super.tearDown();
    }

    // -----------------------------------------------------
    //                                     Prepare Container
    //                                     -----------------
    @Override
    protected void xprepareTestCaseContainer() {
        super.xprepareTestCaseContainer();
        xdoPrepareWebMockContext();
    }

    protected void xdoPrepareWebMockContext() {
        if (_xcachedServletConfig != null) {
            // the servletConfig has been already created when container initialization
            xregisterWebMockContext(_xcachedServletConfig);
        }
    }

    // -----------------------------------------------------
    //                                     Begin Transaction
    //                                     -----------------
    @Override
    protected void xbeginTestCaseTransaction() {
        xprepareLastaFluteContext(); // nearly actual timing
        super.xbeginTestCaseTransaction();
    }

    protected void xprepareLastaFluteContext() {
        initializeThreadCacheContext();
        initializeTransactionTime();
        initializePreparedAccessContext();
    }

    protected void initializeThreadCacheContext() {
        ThreadCacheContext.initialize();
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

    // *unneeded because web mock call curtain-before hook via LastaFilter
    //protected void initializeAssistantDirector() {
    //    final FwCoreDirection direction = _assistantDirector.assistCoreDirection();
    //    direction.assistCurtainBeforeHook().hook(_assistantDirector);
    //}

    // -----------------------------------------------------
    //                                       End Transaction
    //                                       ---------------
    @Override
    protected void xrollbackTestCaseTransaction() {
        super.xrollbackTestCaseTransaction();
        xclearLastaFluteContext();
        xclearWebMockContext();
    }

    protected void xclearLastaFluteContext() {
        PreparedAccessContext.clearAccessContextOnThread();
        TransactionTimeContext.clear();
        ThreadCacheContext.clear();
    }

    protected void xclearWebMockContext() {
        _xmockRequest = null;
        _xmockResponse = null;
    }

    // ===================================================================================
    //                                                                   Lasts Di Handling
    //                                                                   =================
    // -----------------------------------------------------
    //                                            Initialize
    //                                            ----------
    @Override
    protected void xinitializeContainer(String configFile) {
        if (isSuppressWebMock()) { // library
            super.xinitializeContainer(configFile);
        } else { // web (LastaFlute contains web components as default)
            log("...Initializing seasar as web: " + configFile);
            xdoInitializeContainerAsWeb(configFile);
        }
    }

    protected void xdoInitializeContainerAsWeb(String configFile) {
        SingletonLaContainerFactory.setConfigPath(configFile);
        final ServletConfig servletConfig = xprepareMockServletConfig(configFile);
        final LastaFilter filter = xcreateLastaFilter();
        try {
            filter.init(new FilterConfig() {
                public String getFilterName() {
                    return "containerFilter";
                }

                public ServletContext getServletContext() {
                    return servletConfig.getServletContext();
                }

                public Enumeration<String> getInitParameterNames() {
                    return null;
                }

                public String getInitParameter(String name) {
                    return null;
                }
            });
        } catch (ServletException e) {
            String msg = "Failed to initialize servlet config to servlet: " + servletConfig;
            throw new IllegalStateException(msg, e.getRootCause());
        }
    }

    // -----------------------------------------------------
    //                                              Web Mock
    //                                              --------
    protected ServletConfig xprepareMockServletConfig(String configFile) {
        _xcachedServletConfig = createMockletServletConfig(); // cache for request mocks
        _xcachedServletConfig.setServletContext(createMockletServletContext());
        return _xcachedServletConfig;
    }

    protected LastaFilter xcreateLastaFilter() {
        return new LastaFilter();
    }

    protected void xregisterWebMockContext(MockletServletConfig servletConfig) { // like S2ContainerFilter
        final LaContainer container = SingletonLaContainerFactory.getContainer();
        final ExternalContext externalContext = container.getExternalContext();
        final MockletHttpServletRequest request = createMockletHttpServletRequest(servletConfig.getServletContext());
        final MockletHttpServletResponse response = createMockletHttpServletResponse(request);
        externalContext.setRequest(request);
        externalContext.setResponse(response);
        xkeepMockRequestInstance(request, response); // for web mock handling methods
    }

    protected MockletServletConfig createMockletServletConfig() {
        return new MockletServletConfigImpl();
    }

    protected MockletServletContext createMockletServletContext() {
        return new MockletServletContextImpl("utservlet");
    }

    protected MockletHttpServletRequest createMockletHttpServletRequest(ServletContext servletContext) {
        return new MockletHttpServletRequestImpl(servletContext, prepareServletPath());
    }

    protected MockletHttpServletResponse createMockletHttpServletResponse(HttpServletRequest request) {
        return new MockletHttpServletResponseImpl(request);
    }

    protected String prepareServletPath() { // customize point
        return "/utflute";
    }

    protected void xkeepMockRequestInstance(MockletHttpServletRequest request, MockletHttpServletResponse response) {
        _xmockRequest = request;
        _xmockResponse = response;
    }

    // -----------------------------------------------------
    //                                               Destroy
    //                                               -------
    @Override
    protected void xdestroyContainer() {
        super.xdestroyContainer();
        _xcachedServletConfig = null;
    }

    // ===================================================================================
    //                                                                   Web Mock Handling
    //                                                                   =================
    // -----------------------------------------------------
    //                                            LastaFlute
    //                                            ----------
    protected ActionRuntime getMockHtmlRuntime() { // MockAction@sea()
        return new MockRuntimeFactory().createHtmlRuntime();
    }

    protected ActionRuntime getMockJsonRuntime() { // MockAction@land()
        return new MockRuntimeFactory().createJsonRuntime();
    }

    // -----------------------------------------------------
    //                                               Request
    //                                               -------
    protected MockletHttpServletRequest getMockRequest() {
        return _xmockRequest;
    }

    protected void addMockRequestHeader(String name, String value) {
        final MockletHttpServletRequest request = getMockRequest();
        if (request != null) {
            request.addHeader(name, value);
        }
    }

    @SuppressWarnings("unchecked")
    protected <ATTRIBUTE> ATTRIBUTE getMockRequestParameter(String name) {
        final MockletHttpServletRequest request = getMockRequest();
        return request != null ? (ATTRIBUTE) request.getParameter(name) : null;
    }

    protected void addMockRequestParameter(String name, String value) {
        final MockletHttpServletRequest request = getMockRequest();
        if (request != null) {
            request.addParameter(name, value);
        }
    }

    @SuppressWarnings("unchecked")
    protected <ATTRIBUTE> ATTRIBUTE getMockRequestAttribute(String name) {
        final MockletHttpServletRequest request = getMockRequest();
        return request != null ? (ATTRIBUTE) request.getAttribute(name) : null;
    }

    protected void setMockRequestAttribute(String name, Object value) {
        final MockletHttpServletRequest request = getMockRequest();
        if (request != null) {
            request.setAttribute(name, value);
        }
    }

    // -----------------------------------------------------
    //                                              Response
    //                                              --------
    protected MockletHttpServletResponse getMockResponse() {
        return _xmockResponse;
    }

    protected Cookie[] getMockResponseCookies() {
        final MockletHttpServletResponse response = getMockResponse();
        return response != null ? response.getCookies() : null;
    }

    protected int getMockResponseStatus() {
        final MockletHttpServletResponse response = getMockResponse();
        return response != null ? response.getStatus() : 0;
    }

    protected String getMockResponseString() {
        final MockletHttpServletResponse response = getMockResponse();
        return response != null ? response.getResponseString() : null;
    }

    // -----------------------------------------------------
    //                                               Session
    //                                               -------
    /**
     * @return The instance of mock session. (NotNull: if no session, new-created)
     */
    protected MockletHttpSession getMockSession() {
        return _xmockRequest != null ? (MockletHttpSession) _xmockRequest.getSession(true) : null;
    }

    protected void invalidateMockSession() {
        final MockletHttpSession session = getMockSession();
        if (session != null) {
            session.invalidate();
        }
    }

    @SuppressWarnings("unchecked")
    protected <ATTRIBUTE> ATTRIBUTE getMockSessionAttribute(String name) {
        final MockletHttpSession session = getMockSession();
        return session != null ? (ATTRIBUTE) session.getAttribute(name) : null;
    }

    protected void setMockSessionAttribute(String name, Object value) {
        final MockletHttpSession session = getMockSession();
        if (session != null) {
            session.setAttribute(name, value);
        }
    }

    // ===================================================================================
    //                                                                 Response Validation
    //                                                                 ===================
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

    /**
     * Validate HTML data, evaluating HTML bean's validator annotations.
     * <pre>
     * <span style="color: #3F7E5E">// ## Act ##</span>
     * HtmlResponse <span style="color: #553000">response</span> = <span style="color: #553000">action</span>.index(<span style="color: #553000">form</span>);
     *
     * <span style="color: #3F7E5E">// ## Assert ##</span>
     * TestingHtmlData <span style="color: #553000">htmlData</span> = <span style="color: #CC4747">validateHtmlData</span>(<span style="color: #553000">response</span>);
     * <span style="color: #553000">htmlData</span>.<span style="color: #994747">requiredList</span>("beans", ProductBean.<span style="color: #70226C">class</span>).forEach(bean <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     assertEquals("...", <span style="color: #553000">bean</span>.productName);
     * });
     * </pre>
     * @param response The HTML response to be validated. (NotNull)
     * @return The HTML data for testing. (NotNull)
     */
    protected TestingHtmlData validateHtmlData(HtmlResponse response) {
        return new MockResopnseBeanValidator(_requestManager).validateHtmlData(response);
    }

    /**
     * Validate JSON data, evaluating JSON result's validator annotations.
     * <pre>
     * <span style="color: #3F7E5E">// ## Act ##</span>
     * JsonResponse&lt;ProductRowResult&gt; <span style="color: #553000">response</span> = <span style="color: #553000">action</span>.index(<span style="color: #553000">form</span>);
     *
     * <span style="color: #3F7E5E">// ## Assert ##</span>
     * TestingJsonData&lt;ProductRowResult&gt; <span style="color: #553000">jsonData</span> = <span style="color: #CC4747">validateJsonData</span>(<span style="color: #553000">response</span>);
     * ProductRowResult <span style="color: #553000">result</span> = <span style="color: #553000">jsonData</span>.getJsonResult();
     * ...
     * </pre>
     * @param <BEAN> The type of JSON bean.
     * @param response The HTML response to be validated. (NotNull)
     * @return The HTML data for testing. (NotNull)
     */
    protected <BEAN> TestingJsonData<BEAN> validateJsonData(JsonResponse<BEAN> response) {
        return new MockResopnseBeanValidator(_requestManager).validateJsonData(response);
    }

    // ===================================================================================
    //                                                                    Validation Error
    //                                                                    ================
    /**
     * Assert validation error of action.
     * <pre>
     * <span style="color: #3F7E5E">// ## Arrange ##</span>
     * SignupAction <span style="color: #553000">action</span> = <span style="color: #70226C">new</span> SignupAction();
     * inject(<span style="color: #553000">action</span>);
     * SignupForm <span style="color: #553000">form</span> = <span style="color: #70226C">new</span> SignupForm();
    
     * <span style="color: #3F7E5E">// ## Act ##</span>
     * <span style="color: #CC4747">assertValidationError</span>(() -&gt; <span style="color: #553000">action</span>.index(<span style="color: #553000">form</span>)).handle(<span style="color: #553000">data</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #3F7E5E">// ## Assert ##</span>
     *     <span style="color: #553000">data</span>.requiredMessageOf("sea", Required.class);
     * });
     * </pre>
     * @param noArgInLambda The callback for calling methods that should throw the validation error exception. (NotNull)
     * @return The after object that has handler of expected cause for chain call. (NotNull) 
     */
    protected TestingValidationErrorAfter assertValidationError(IndependentProcessor noArgInLambda) {
        final Set<ValidationErrorException> causeSet = new HashSet<ValidationErrorException>();
        assertException(ValidationErrorException.class, () -> noArgInLambda.process()).handle(cause -> causeSet.add(cause));
        return new TestingValidationErrorAfter(causeSet.iterator().next(), _messageManager, _requestManager);
    }

    /**
     * Evaluate validation error hook for action response.
     * <pre>
     * <span style="color: #3F7E5E">// if HTML response</span>
     * assertException(ValidationErrorException.<span style="color: #70226C">class</span>, () <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> <span style="color: #553000">action</span>.update(<span style="color: #553000">form</span>)).handle(<span style="color: #553000">cause</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     HtmlResponse <span style="color: #553000">response</span> = <span style="color: #CC4747">hookValidationError</span>(<span style="color: #553000">cause</span>);
     *     TestingHtmlData <span style="color: #553000">htmlData</span> = validateHtmlData(<span style="color: #553000">response</span>);
     *     ...
     * });
     * </pre>
     * @param <RESPONSE> The type of action response, e.g. HtmlResponse, JsonResponse.
     * @param cause The exception of validation error. (NotNull)
     * @return The action response from validation error hook.
     * @deprecated use assertValidationError()
     */
    @SuppressWarnings("unchecked")
    protected <RESPONSE extends ActionResponse> RESPONSE hookValidationError(ValidationErrorException cause) {
        return (RESPONSE) cause.getErrorHook().hook();
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
    //                                                                     Token Assertion
    //                                                                     ===============
    /**
     * Assert double submit token is saved in action execution.
     * <pre>
     * <span style="color: #3F7E5E">// ## Act ##</span>
     * HtmlResponse <span style="color: #553000">response</span> = <span style="color: #553000">action</span>.index(<span style="color: #553000">memberId</span>); <span style="color: #3F7E5E">// calls saveToken()</span>
     *
     * <span style="color: #3F7E5E">// ## Assert ##</span>
     * <span style="color: #CC4747">assertTokenSaved</span>(<span style="color: #553000">action</span>.getClass());
     * </pre>
     * @param groupType The group type to get double submit token, basically action type. (NotNull)
     */
    protected void assertTokenSaved(Class<?> groupType) { // for action that calls saveToken()
        final DoubleSubmitTokenMap tokenMap = _doubleSubmitManager.getSessionTokenMap().get();
        final boolean condition = tokenMap.get(groupType).isPresent();
        assertTrue("Not found the transaction token saved in session, so call saveToken(): tokenMap=" + tokenMap, condition);
    }

    /**
     * Mock double submit token is requested in the test process.
     * <pre>
     * <span style="color: #3F7E5E">// ## Arrange ##</span>
     * MemberEditAction <span style="color: #553000">action</span> = <span style="color: #70226C">new</span> MemberEditAction();
     * inject(<span style="color: #553000">action</span>);
     * <span style="color: #CC4747">mockTokenRequested</span>(<span style="color: #553000">action</span>.getClass());
     * ...
     *
     * <span style="color: #3F7E5E">// ## Act ##</span>
     * HtmlResponse <span style="color: #553000">response</span> = <span style="color: #553000">action</span>.update(<span style="color: #553000">form</span>); <span style="color: #3F7E5E">// calls verifyToken()</span>
     *
     * <span style="color: #3F7E5E">// ## Assert ##</span>
     * <span style="color: #994747">assertTokenVerified()</span>;
     * </pre>
     * @param groupType The group type to get double submit token, basically action type. (NotNull)
     */
    protected void mockTokenRequested(Class<?> groupType) { // for action that calls verityToken()
        final String savedToken = _doubleSubmitManager.saveToken(groupType);
        getMockRequest().setParameter(LastaWebKey.TRANSACTION_TOKEN_KEY, savedToken);
    }

    /**
     * Mock double submit token is requested as double submit in the test process.
     * <pre>
     * <span style="color: #3F7E5E">// ## Arrange ##</span>
     * MemberEditAction <span style="color: #553000">action</span> = <span style="color: #70226C">new</span> MemberEditAction();
     * inject(<span style="color: #553000">action</span>);
     * <span style="color: #CC4747">mockTokenRequestedAsDoubleSubmit</span>(<span style="color: #553000">action</span>.getClass());
     * ...
     *
     * <span style="color: #3F7E5E">// ## Act ##</span>
     * <span style="color: #3F7E5E">// ## Assert ##</span>
     * assertException(<span style="color: #994747">DoubleSubmittedRequestException</span>.<span style="color: #70226C">class</span>, () <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> <span style="color: #553000">action</span>.update(<span style="color: #553000">form</span>));
     * </pre>
     * @param groupType The group type to get double submit token, basically action type. (NotNull)
     */
    protected void mockTokenRequestedAsDoubleSubmit(Class<?> groupType) { // for action that calls verityToken()
        final String savedToken = _doubleSubmitManager.saveToken(groupType);
        getMockRequest().setParameter(LastaWebKey.TRANSACTION_TOKEN_KEY, savedToken);
        _doubleSubmitManager.verifyToken(groupType, () -> { // means first request done
            throw new IllegalStateException("no way");
        });
    }

    /**
     * Assert double submit token is verified in action execution.
     * <pre>
     * <span style="color: #3F7E5E">// ## Arrange ##</span>
     * MemberEditAction <span style="color: #553000">action</span> = <span style="color: #70226C">new</span> MemberEditAction();
     * inject(<span style="color: #553000">action</span>);
     * <span style="color: #994747">mockTokenRequested</span>(<span style="color: #553000">action</span>.getClass());
     * ...
     *
     * <span style="color: #3F7E5E">// ## Act ##</span>
     * HtmlResponse <span style="color: #553000">response</span> = <span style="color: #553000">action</span>.update(<span style="color: #553000">form</span>); <span style="color: #3F7E5E">// calls verityToken()</span>
     *
     * <span style="color: #3F7E5E">// ## Assert ##</span>
     * <span style="color: #CC4747">assertTokenVerified()</span>;
     * </pre>
     */
    protected void assertTokenVerified() { // for action that calls verityToken()
        final boolean condition = _doubleSubmitManager.isFirstSubmittedRequest();
        assertTrue("Not found the transaction token verification, so call verifyToken().", condition);
    }

    // ===================================================================================
    //                                                                         Destructive
    //                                                                         ===========
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

    // ===================================================================================
    //                                                                            LastaDoc
    //                                                                            ========
    /**
     * Save meta data for rich LastaDoc.
     * <pre>
     * 1. call this method in your unit test
     * 2. execute FreeGen task (manage.sh 12) of DBFlute
     * 3. see auto-generated LastaDoc
     * </pre>
     */
    protected void saveLastaDocMeta() {
        createDocumentGenerator().saveLastaDocMeta();
    }

    /**
     * Create document generator for rich LastaDoc.
     * @return The new-created document generator. (NotNull)
     */
    protected DocumentGenerator createDocumentGenerator() {
        return new DocumentGenerator();
    }

    // ===================================================================================
    //                                                                            LastaJob
    //                                                                            ========
    protected boolean isSuppressJobScheduling() { // you can override, for e.g. heavy scheduling (using e.g. DB)
        return false; // you can set true only when including LastaJob
    }

    protected void xsuppressJobScheduling() {
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

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    protected static MockletServletConfig xgetCachedServletConfig() {
        return _xcachedServletConfig;
    }

    protected static void xsetCachedServletConfig(MockletServletConfig xcachedServletConfig) {
        _xcachedServletConfig = xcachedServletConfig;
    }

    protected MockletHttpServletRequest xgetMockRequest() {
        return _xmockRequest;
    }

    protected void xsetMockRequest(MockletHttpServletRequest xmockRequest) {
        _xmockRequest = xmockRequest;
    }

    protected MockletHttpServletResponse xgetMockResponse() {
        return _xmockResponse;
    }

    protected void xsetMockResponse(MockletHttpServletResponse xmockResponse) {
        _xmockResponse = xmockResponse;
    }

    protected MailMessageAssertion xgetMailMessageValidator() {
        return _xmailMessageAssertion;
    }

    protected void xsetMailMessageValidator(MailMessageAssertion xmailMessageValidator) {
        _xmailMessageAssertion = xmailMessageValidator;
    }
}
