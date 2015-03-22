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

import java.util.Enumeration;

import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.lasta.di.core.ExternalContext;
import org.dbflute.lasta.di.core.LaContainer;
import org.dbflute.lasta.di.core.factory.SingletonLaContainerFactory;
import org.dbflute.lastaflute.web.servlet.filter.LastaContainerFilter;
import org.dbflute.utflute.lasta.ContainerTestCase;
import org.dbflute.utflute.lastaflute.web.ActionUrlPatternChecker;
import org.dbflute.utflute.mocklet.MockletHttpServletRequest;
import org.dbflute.utflute.mocklet.MockletHttpServletRequestImpl;
import org.dbflute.utflute.mocklet.MockletHttpServletResponse;
import org.dbflute.utflute.mocklet.MockletHttpServletResponseImpl;
import org.dbflute.utflute.mocklet.MockletHttpSession;
import org.dbflute.utflute.mocklet.MockletServletConfig;
import org.dbflute.utflute.mocklet.MockletServletConfigImpl;
import org.dbflute.utflute.mocklet.MockletServletContext;
import org.dbflute.utflute.mocklet.MockletServletContextImpl;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public abstract class WebContainerTestCase extends ContainerTestCase {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The cached configuration of servlet. (NullAllowed: when no web mock or beginning or ending) */
    protected static MockletServletConfig _xcachedServletConfig;

    // -----------------------------------------------------
    //                                              Web Mock
    //                                              --------
    /** The mock request of the test case execution. (NullAllowed: when no web mock or beginning or ending) */
    protected MockletHttpServletRequest _xmockRequest;

    /** The mock response of the test case execution. (NullAllowed: when no web mock or beginning or ending) */
    protected MockletHttpServletResponse _xmockResponse;

    // ===================================================================================
    //                                                                            Settings
    //                                                                            ========
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

    @Override
    public void tearDown() throws Exception {
        _xmockRequest = null;
        _xmockResponse = null;
        super.tearDown();
    }

    // ===================================================================================
    //                                                                     Seasar Handling
    //                                                                     ===============
    // -----------------------------------------------------
    //                                            Initialize
    //                                            ----------
    @Override
    protected void xinitializeContainer(String configFile) {
        if (isSuppressWebMock()) { // library
            super.xinitializeContainer(configFile);
        } else { // web (Seasar contains web components as default)
            log("...Initializing seasar as web: " + configFile);
            xdoInitializeContainerAsWeb(configFile);
        }
    }

    protected void xdoInitializeContainerAsWeb(String configFile) {
        final ServletConfig servletConfig = xprepareMockServletConfig(configFile);
        final LastaContainerFilter containerServlet = xcreateLastaContainerFilter();
        try {
            containerServlet.init(new FilterConfig() {
                public String getFilterName() {
                    return "containerFilter";
                }

                public ServletContext getServletContext() {
                    return servletConfig.getServletContext();
                }

                public Enumeration<?> getInitParameterNames() {
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

    protected LastaContainerFilter xcreateLastaContainerFilter() {
        return new LastaContainerFilter();
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
    //                                               Request
    //                                               -------
    protected MockletHttpServletRequest getMockRequest() {
        return (MockletHttpServletRequest) _xmockRequest;
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
        return (MockletHttpServletResponse) _xmockResponse;
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
    //                                                                    SAStruts Checker
    //                                                                    ================
    protected void checkActionUrlPattern() {
        doCheckActionUrlPattern("Action");
    }

    protected void doCheckActionUrlPattern(String actionSuffix) {
        policeStoryOfJavaClassChase(createActionUrlPatternChecker(actionSuffix));
    }

    protected ActionUrlPatternChecker createActionUrlPatternChecker(String actionSuffix) {
        return new ActionUrlPatternChecker(actionSuffix);
    }
}
