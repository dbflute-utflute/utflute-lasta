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

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.helper.function.IndependentProcessor;
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
import org.junit.jupiter.api.Assertions;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.di.core.ExternalContext;
import org.lastaflute.di.core.LaContainer;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.meta.DocumentGenerator;
import org.lastaflute.meta.SwaggerGenerator;
import org.lastaflute.meta.web.LaActionSwaggerable;
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
 * The base class of test cases for web environment with DI container. <br>
 * You can use tests of LastaFlute components e.g. action, assist, logic, job.
 * 
 * <p>Standard application structure:</p>
 * <pre>
 * WebContainerTestCase
 *  |-Unit[App]TestCase
 *      |-[Your]ActionTest
 * </pre>
 * 
 * <p>You can test like this:</p>
 * <pre>
 * public void test_yourMethod() {
 *     <span style="color: #3F7E5E">// ## Arrange ##</span>
 *     YourAction action = new YourAction();
 *     <span style="color: #FD4747">inject</span>(action);
 * 
 *     <span style="color: #3F7E5E">// ## Act ##</span>
 *     action.submit();
 * 
 *     <span style="color: #3F7E5E">// ## Assert ##</span>
 *     assertTrue(action...);
 * }
 * </pre>
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public abstract class WebContainerTestCase extends LastaFluteTestCase {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                          Request Mock
    //                                          ------------
    /** The mock request of the test case execution. (NullAllowed: when no web mock or beginning or ending) */
    private MockletHttpServletRequest _xmockRequest;

    /** The mock response of the test case execution. (NullAllowed: when no web mock or beginning or ending) */
    private MockletHttpServletResponse _xmockResponse;

    // -----------------------------------------------------
    //                                  LastaFlute Component
    //                                  --------------------
    @Resource
    private FwAssistantDirector _assistantDirector;
    @Resource
    private MessageManager _messageManager;
    @Resource
    private RequestManager _requestManager;
    @Resource
    private DoubleSubmitManager _doubleSubmitManager;

    // ===================================================================================
    //                                                                            Settings
    //                                                                            ========
    // -----------------------------------------------------
    //                                     Prepare Container
    //                                     -----------------
    @Override
    protected void xprepareTestCaseContainer() {
        super.xprepareTestCaseContainer();
        if (!isSuppressRequestMock()) {
            xdoPrepareRequestMockContext();
        }
    }

    /**
     * Does it suppress web-request mock? e.g. HttpServletRequest, HttpSession
     * @return The determination, true or false.
     */
    protected boolean isSuppressRequestMock() {
        return false;
    }

    protected void xdoPrepareRequestMockContext() {
        // the servletConfig has been already created when container initialization
        final MockletServletConfig servletConfig = xgetCachedServletConfig();
        if (servletConfig != null) { // basically true, just in case (e.g. might be overridden)
            xregisterRequestMockContext(servletConfig);
        }
    }

    protected void xregisterRequestMockContext(MockletServletConfig servletConfig) { // like S2ContainerFilter
        final LaContainer container = SingletonLaContainerFactory.getContainer();
        final ExternalContext externalContext = container.getExternalContext();
        final MockletHttpServletRequest request = createMockletHttpServletRequest(servletConfig.getServletContext());
        final MockletHttpServletResponse response = createMockletHttpServletResponse(request);
        externalContext.setRequest(request);
        externalContext.setResponse(response);
        xkeepMockRequestInstance(request, response); // for web mock handling methods
    }

    protected MockletHttpServletRequest createMockletHttpServletRequest(ServletContext servletContext) {
        return new MockletHttpServletRequestImpl(servletContext, prepareMockServletPath());
    }

    protected MockletHttpServletResponse createMockletHttpServletResponse(HttpServletRequest request) {
        return new MockletHttpServletResponseImpl(request);
    }

    protected String prepareMockServletPath() { // you can override
        return "/utservlet";
    }

    protected void xkeepMockRequestInstance(MockletHttpServletRequest request, MockletHttpServletResponse response) {
        _xmockRequest = request;
        _xmockResponse = response;
    }

    @Override
    protected boolean maybeContainerResourceOverridden() {
        return super.maybeContainerResourceOverridden() || xisMethodOverridden("prepareMockServletPath");
    }

    // -----------------------------------------------------
    //                                     Destroy Container
    //                                     -----------------
    @Override
    protected void xdestroyTestCaseContainer() {
        xclearRequestMockContext();
        super.xdestroyTestCaseContainer();
    }

    protected void xclearRequestMockContext() {
        final LaContainer container = SingletonLaContainerFactory.getContainer();
        final ExternalContext externalContext = container.getExternalContext();
        if (externalContext != null) { // just in case
            externalContext.setRequest(null);
            externalContext.setResponse(null);
        }
        xreleaseMockRequestInstance();
    }

    protected void xreleaseMockRequestInstance() {
        _xmockRequest = null;
        _xmockResponse = null;
    }

    // ===================================================================================
    //                                                                        Request Mock
    //                                                                        ============
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
        Assertions.assertTrue(condition, "Not found the transaction token saved in session, so call saveToken(): tokenMap=" + tokenMap);
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
        Assertions.assertTrue(condition, "Not found the transaction token verification, so call verifyToken().");
    }

    // ===================================================================================
    //                                                                Mock HTML Validation
    //                                                                ====================
    /**
     * Mock HTML validate() call for thread cache adjustment. <br>
     * For example, in LastaRemoteApi, client error translation of HTML validation error needs this.
     * <pre>
     * // in yourDefaultRule()
     * rule.translateClientError(resource -&gt; {
     *     ...
     *     return resource.asHtmlValidationError(messages);
     * });
     * 
     * ...
     * 
     * // in unit test
     * mockHtmlValidateCall();
     * assertValidationError(() -&gt; bhv.requestProductList(param));
     * </pre>
     */
    protected void mockHtmlValidateCall() { // for e.g. remote api unit test
        ThreadCacheContext.registerValidatorErrorHook(() -> ActionResponse.undefined()); // dummy
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
    //                                                                             Swagger
    //                                                                             =======
    /**
     * Save meta data to use rich swagger in war deployment. <br>
     * <pre>
     * public void test_swaggerJson() {
     *     saveSwaggerMeta(new SwaggerAction());
     * }
     * </pre>
     * @param swaggerable The new-created swagger-able action to get swagger JSON. (NotNull)
     */
    protected void saveSwaggerMeta(LaActionSwaggerable swaggerable) {
        Assertions.assertNotNull(swaggerable);
        inject(swaggerable);
        createSwaggerGenerator().saveSwaggerMeta(swaggerable);
    }

    /**
     * Create swagger generator for rich swagger.
     * @return The new-created swagger generator. (NotNull)
     */
    protected SwaggerGenerator createSwaggerGenerator() {
        return new SwaggerGenerator();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
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
}
