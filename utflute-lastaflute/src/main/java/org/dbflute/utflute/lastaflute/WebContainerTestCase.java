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
package org.dbflute.utflute.lastaflute;

import java.util.Date;
import java.util.Enumeration;
import java.util.function.Consumer;

import javax.annotation.Resource;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.utflute.lastadi.ContainerTestCase;
import org.dbflute.utflute.lastaflute.document.DocumentGenerator;
import org.dbflute.utflute.lastaflute.mock.MockResopnseBeanValidator;
import org.dbflute.utflute.lastaflute.mock.MockRuntimeFactory;
import org.dbflute.utflute.lastaflute.mock.TestingHtmlData;
import org.dbflute.utflute.lastaflute.mock.TestingJsonData;
import org.dbflute.utflute.lastaflute.mock.mail.MockMailMessageValidator;
import org.dbflute.utflute.lastaflute.mock.mail.TestingMailData;
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
import org.lastaflute.core.direction.FwCoreDirection;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.magic.TransactionTimeContext;
import org.lastaflute.core.magic.destructive.BowgunDestructiveAdjuster;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.di.core.ExternalContext;
import org.lastaflute.di.core.LaContainer;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.web.LastaFilter;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.request.RequestManager;

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
    private MockMailMessageValidator _xmailMessageValidator;

    // -----------------------------------------------------
    //                                  LastaFlute Component
    //                                  --------------------
    @Resource
    private FwAssistantDirector _assistantDirector;
    @Resource
    private TimeManager _timeManager;
    @Resource
    private JsonManager _jsonManager;
    @Resource
    private RequestManager _requestManager;

    // ===================================================================================
    //                                                                            Settings
    //                                                                            ========
    @Override
    protected void postTest() {
        super.postTest();
        xprocessMailValidation();
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
        initializeAssistantDirector();
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

    protected void initializeAssistantDirector() {
        final FwCoreDirection direction = _assistantDirector.assistCoreDirection();
        direction.assistCurtainBeforeHook().hook(_assistantDirector);
    }

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
    protected void showJson(Object jsonBean) {
        log(jsonBean.getClass().getSimpleName() + ":" + ln() + toJson(jsonBean));
    }

    protected String toJson(Object jsonBean) {
        final Object realBean;
        if (jsonBean instanceof JsonResponse<?>) {
            realBean = ((JsonResponse<?>) jsonBean).getJsonBean();
        } else {
            realBean = jsonBean;
        }
        return _jsonManager.toJson(realBean);
    }

    protected TestingHtmlData validateHtmlData(HtmlResponse response) {
        return new MockResopnseBeanValidator(_requestManager).validateHtmlData(response);
    }

    protected <BEAN> TestingJsonData<BEAN> validateJsonData(JsonResponse<BEAN> response) {
        return new MockResopnseBeanValidator(_requestManager).validateJsonData(response);
    }

    // ===================================================================================
    //                                                                     Mail Validation
    //                                                                     ===============
    protected void reserveMailValidation(Consumer<TestingMailData> oneArgLambda) {
        _xmailMessageValidator = new MockMailMessageValidator(oneArgLambda);
    }

    protected void xprocessMailValidation() {
        if (_xmailMessageValidator != null) {
            _xmailMessageValidator.validateMailData();
            _xmailMessageValidator = null;
        }
    }

    // ===================================================================================
    //                                                                         Destructive
    //                                                                         ===========
    protected void changeAsyncToNormalSync() {
        BowgunDestructiveAdjuster.unlock();
        BowgunDestructiveAdjuster.shootBowgunAsyncToNormalSync();
    }

    protected void changeRequiresNewToRequired() {
        BowgunDestructiveAdjuster.unlock();
        BowgunDestructiveAdjuster.shootBowgunRequiresNewToRequired();
    }

    // ===================================================================================
    //                                                                            LastaDoc
    //                                                                            ========
    protected void saveLastaDocMeta() {
        createDocumentGenerator().saveLastaDocMeta();
    }

    protected DocumentGenerator createDocumentGenerator() {
        return new DocumentGenerator();
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

    protected MockMailMessageValidator xgetMailMessageValidator() {
        return _xmailMessageValidator;
    }

    protected void xsetMailMessageValidator(MockMailMessageValidator xmailMessageValidator) {
        _xmailMessageValidator = xmailMessageValidator;
    }
}
