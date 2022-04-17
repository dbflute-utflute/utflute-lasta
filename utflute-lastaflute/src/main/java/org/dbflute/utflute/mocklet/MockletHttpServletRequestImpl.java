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
package org.dbflute.utflute.mocklet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.dbflute.utflute.mocklet.helper.MockletEmptyEnumeration;
import org.dbflute.utflute.mocklet.helper.MockletEnumerationAdapter;

/**
 * @author modified by jflute (originated in Seasar)
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class MockletHttpServletRequestImpl implements MockletHttpServletRequest {

    protected final ServletContext servletContext;
    protected final String servletPath;
    protected String authType;
    protected final List<Cookie> cookieList = new ArrayList<Cookie>();
    protected final Map<String, List<String>> headers = new HashMap<String, List<String>>();
    protected String method = "POST";
    protected String pathInfo;
    protected String pathTranslated;
    protected String queryString;
    protected MockletHttpSessionImpl session;
    protected String scheme = "http";
    protected int serverPort = 80;
    protected String protocol = "HTTP/1.1";
    protected String serverName = "localhost";
    protected final Map<String, Object> attributes = new HashMap<String, Object>();
    protected String characterEncoding = "ISO-8859-1";
    protected int contentLength;
    protected String contentType;
    protected final Map<String, String[]> parameters = new HashMap<String, String[]>();
    protected String remoteAddr;
    protected String remoteHost;
    protected int remotePort;
    protected String localAddr;
    protected String localName;
    protected int localPort;
    protected final List<Locale> locales = new ArrayList<Locale>();

    public MockletHttpServletRequestImpl(ServletContext servletContext, String servletPath) {
        this.servletContext = servletContext;
        if (servletPath.charAt(0) == '/') {
            this.servletPath = servletPath;
        } else {
            this.servletPath = "/" + servletPath;
        }
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public Cookie[] getCookies() {
        return cookieList.toArray(new Cookie[cookieList.size()]);
    }

    public void addCookie(Cookie cookie) {
        cookieList.add(cookie);
    }

    public long getDateHeader(String name) {
        String value = getHeader(name);
        return MockletHeaderUtil.getDateValue(value);
    }

    public String getHeader(String name) {
        List<String> values = getHeaderList(name);
        if (values != null) {
            return values.get(0);
        }
        return null;
    }

    public Enumeration<String> getHeaders(String name) {
        List<String> values = getHeaderList(name);
        if (values != null) {
            return new MockletEnumerationAdapter<String>(values.iterator());
        }
        return new MockletEmptyEnumeration<String>();
    }

    public Enumeration<String> getHeaderNames() {
        return new MockletEnumerationAdapter<String>(headers.keySet().iterator());
    }

    public int getIntHeader(String name) {
        String value = getHeader(name);
        return MockletHeaderUtil.getIntValue(value);
    }

    public void addHeader(String name, String value) {
        List<String> values = getHeaderList(name);
        if (values == null) {
            values = new ArrayList<String>();
        }
        values.add(value);
        headers.put(name.toLowerCase(), values);
    }

    public void addDateHeader(String name, long value) {
        addHeader(name, MockletHeaderUtil.getDateValue(value));
    }

    public void addIntHeader(String name, int value) {
        addHeader(name, "" + value);
    }

    private List<String> getHeaderList(String name) {
        name = name.toLowerCase();
        return headers.get(name);
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public String getPathTranslated() {
        return pathTranslated;
    }

    public void setPathTranslated(String pathTranslated) {
        this.pathTranslated = pathTranslated;
    }

    public String getContextPath() {
        return servletContext.getContextPath();
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getRemoteUser() {
        return System.getProperty("user.name");
    }

    public boolean isUserInRole(String arg0) {
        throw new UnsupportedOperationException();
    }

    public Principal getUserPrincipal() {
        throw new UnsupportedOperationException();
    }

    public String getRequestedSessionId() {
        String sessionId = getRequestedSessionIdFromCookie();
        if (sessionId != null) {
            return sessionId;
        }
        return getRequestedSessionIdFromURL();
    }

    protected String getRequestedSessionIdFromCookie() {
        Cookie[] cookies = getCookies();
        if (cookies == null) {
            return null;
        }
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if (cookie.getName().endsWith("sessionid")) {
                return cookie.getValue();
            }
        }
        return null;
    }

    protected String getRequestedSessionIdFromURL() {
        String uri = getRequestURI();
        int index = uri.lastIndexOf("sessionid");
        if (index < 0) {
            return null;
        }
        return uri.substring(index + "sessionid".length());
    }

    public String getRequestURI() {
        String contextPath = getContextPath();
        if (contextPath.equals("/")) {
            return servletPath;
        }
        return contextPath + servletPath;
    }

    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        url.append(scheme);
        url.append("://");
        url.append(serverName);
        if ((scheme.equals("http") && (serverPort != 80)) || (scheme.equals("https") && (serverPort != 443))) {

            url.append(':');
            url.append(serverPort);
        }
        url.append(getRequestURI());
        return url;
    }

    public String getServletPath() {
        return servletPath;
    }

    public HttpSession getSession(boolean create) {
        if (session != null) {
            return session;
        }
        if (create) {
            session = createMockletHttpSessionImpl(servletContext);
        }
        if (session != null) {
            session.access();
        }
        return session;
    }

    protected MockletHttpSessionImpl createMockletHttpSessionImpl(ServletContext servletContext) {
        return new MockletHttpSessionImpl(servletContext);
    }

    public HttpSession getSession() {
        return getSession(true);
    }

    public boolean isRequestedSessionIdValid() {
        if (session != null) {
            return session.isValid();
        }
        return false;
    }

    public boolean isRequestedSessionIdFromCookie() {
        return getRequestedSessionIdFromCookie() != null;
    }

    public boolean isRequestedSessionIdFromURL() {
        return getRequestedSessionIdFromURL() != null;
    }

    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public Enumeration<String> getAttributeNames() {
        return new MockletEnumerationAdapter<String>(attributes.keySet().iterator());
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) throws UnsupportedEncodingException {
        this.characterEncoding = characterEncoding;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public ServletInputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    public String getParameter(String name) {
        final String[] values = parameters.get(name);
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }

    public Enumeration<String> getParameterNames() {
        return new MockletEnumerationAdapter<String>(parameters.keySet().iterator());
    }

    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    public void addParameter(String name, String value) {
        final String[] values = getParameterValues(name);
        if (values == null) {
            setParameter(name, value);
        } else {
            final String[] newArray = new String[values.length + 1];
            System.arraycopy(values, 0, newArray, 0, values.length);
            newArray[newArray.length - 1] = value;
            parameters.put(name, newArray);
        }
    }

    public void addParameter(String name, String[] values) {
        if (values == null) {
            setParameter(name, (String) null);
            return;
        }
        final String[] vals = getParameterValues(name);
        if (vals == null) {
            setParameter(name, values);
        } else {
            final String[] newArray = new String[vals.length + values.length];
            System.arraycopy(vals, 0, newArray, 0, vals.length);
            System.arraycopy(values, 0, newArray, vals.length, values.length);
            parameters.put(name, newArray);
        }
    }

    public void setParameter(String name, String value) {
        parameters.put(name, new String[] { value });
    }

    public void setParameter(String name, String[] values) {
        parameters.put(name, values);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public BufferedReader getReader() throws IOException {
        throw new UnsupportedOperationException();
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public String getLocalAddr() {
        return localAddr;
    }

    public void setLocalAddr(String localAddr) {
        this.localAddr = localAddr;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public Locale getLocale() {
        if (locales.isEmpty()) {
            return Locale.getDefault(); // according to JavaDoc
        }
        return locales.get(0);
    }

    public void setLocale(Locale locale) {
        locales.clear();
        locales.add(locale);
    }

    public Enumeration<Locale> getLocales() {
        return new MockletEnumerationAdapter<Locale>(locales.iterator());
    }

    public boolean isSecure() {
        return false;
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return new MockletRequestDispatcherImpl();
    }

    @Deprecated
    public String getRealPath(String path) {
        return path;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    // ===================================================================================
    //                                                                         since 3.0.1
    //                                                                         ===========
    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {
    }

    @Override
    public void logout() throws ServletException {
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return null;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }
}