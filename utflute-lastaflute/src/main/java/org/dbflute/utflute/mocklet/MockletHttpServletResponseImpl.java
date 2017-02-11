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
package org.dbflute.utflute.mocklet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dbflute.utflute.mocklet.helper.MockletEmptyEnumeration;
import org.dbflute.utflute.mocklet.helper.MockletEnumerationAdapter;
import org.dbflute.utflute.mocklet.helper.MockletSPrintWriter;

/**
 * @author modified by jflute (originated in Seasar)
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class MockletHttpServletResponseImpl implements MockletHttpServletResponse {

    protected final List<Cookie> cookieList;
    protected final Map<String, List<String>> headers = new HashMap<String, List<String>>();
    protected boolean committed = false;
    protected int status;
    protected String message;
    protected byte[] buffer = new byte[1024];
    protected Locale locale;
    protected String characterEncoding;
    protected final PrintWriter writer = new MockletSPrintWriter();
    protected final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    protected final ServletOutputStream outputStream = new MockletServletOutputStreamImpl(byteArrayOutputStream);
    protected boolean getWriterCalled;
    protected boolean getOutputStreamCalled;

    public MockletHttpServletResponseImpl(HttpServletRequest request) {
        cookieList = new ArrayList<Cookie>(Arrays.asList(request.getCookies()));
        locale = request.getLocale();
        characterEncoding = request.getCharacterEncoding();
    }

    public Cookie[] getCookies() {
        return cookieList.toArray(new Cookie[cookieList.size()]);
    }

    public void addCookie(Cookie cookie) {
        cookieList.add(cookie);
    }

    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    public String encodeURL(String url) {
        return url;
    }

    public String encodeRedirectURL(String url) {
        return url;
    }

    @Deprecated
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    @Deprecated
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public void sendError(int status, String message) throws IOException {
        setStatus(status, message);
    }

    public void sendError(int status) throws IOException {
        setStatus(status);
    }

    public void sendRedirect(String path) throws IOException {
    }

    public Enumeration<String> getHeaders(String name) {
        List<String> values = getHeaderList(name);
        if (values != null) {
            return new MockletEnumerationAdapter<String>(values.iterator());
        }
        return new MockletEmptyEnumeration<String>();
    }

    public String getHeader(String name) {
        List<String> values = getHeaderList(name);
        if (values != null) {
            return values.get(0);
        }
        return null;
    }

    public Enumeration<String> getHeaderNames() {
        return new MockletEnumerationAdapter<String>(headers.keySet().iterator());
    }

    public void setDateHeader(String name, long value) {
        setHeader(name, MockletHeaderUtil.getDateValue(value));
    }

    public void addDateHeader(String name, long value) {
        addHeader(name, MockletHeaderUtil.getDateValue(value));
    }

    public void setHeader(String name, String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        headers.put(name.toLowerCase(), values);
    }

    public void addHeader(String name, String value) {
        List<String> values = getHeaderList(name);
        if (values == null) {
            values = new ArrayList<String>();
        }
        values.add(value);
        headers.put(name.toLowerCase(), values);
    }

    public int getIntHeader(String name) {
        String value = getHeader(name);
        return MockletHeaderUtil.getIntValue(value);
    }

    public void setIntHeader(String name, int value) {
        setHeader(name, value + "");
    }

    public void addIntHeader(String name, int value) {
        addHeader(name, value + "");
    }

    private List<String> getHeaderList(String name) {
        name = name.toLowerCase();
        return headers.get(name);
    }

    public void setStatus(int status) {
        setStatus(status, getResponseStatusMessage(status));
    }

    private static String getResponseStatusMessage(int status) {
        switch (status) {
        case HttpServletResponse.SC_OK:
            return "OK";
        case HttpServletResponse.SC_ACCEPTED:
            return "Accepted";
        case HttpServletResponse.SC_BAD_GATEWAY:
            return "Bad Gateway";
        case HttpServletResponse.SC_BAD_REQUEST:
            return "Bad Request";
        case HttpServletResponse.SC_CONFLICT:
            return "Conflict";
        case HttpServletResponse.SC_CONTINUE:
            return "Continue";
        case HttpServletResponse.SC_CREATED:
            return "Created";
        case HttpServletResponse.SC_EXPECTATION_FAILED:
            return "Expectation Failed";
        case HttpServletResponse.SC_FORBIDDEN:
            return "Forbidden";
        case HttpServletResponse.SC_GATEWAY_TIMEOUT:
            return "Gateway Timeout";
        case HttpServletResponse.SC_GONE:
            return "Gone";
        case HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED:
            return "HTTP Version Not Supported";
        case HttpServletResponse.SC_INTERNAL_SERVER_ERROR:
            return "Internal Server Error";
        case HttpServletResponse.SC_LENGTH_REQUIRED:
            return "Length Required";
        case HttpServletResponse.SC_METHOD_NOT_ALLOWED:
            return "Method Not Allowed";
        case HttpServletResponse.SC_MOVED_PERMANENTLY:
            return "Moved Permanently";
        case HttpServletResponse.SC_MOVED_TEMPORARILY:
            return "Moved Temporarily";
        case HttpServletResponse.SC_MULTIPLE_CHOICES:
            return "Multiple Choices";
        case HttpServletResponse.SC_NO_CONTENT:
            return "No Content";
        case HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION:
            return "Non-Authoritative Information";
        case HttpServletResponse.SC_NOT_ACCEPTABLE:
            return "Not Acceptable";
        case HttpServletResponse.SC_NOT_FOUND:
            return "Not Found";
        case HttpServletResponse.SC_NOT_IMPLEMENTED:
            return "Not Implemented";
        case HttpServletResponse.SC_NOT_MODIFIED:
            return "Not Modified";
        case HttpServletResponse.SC_PARTIAL_CONTENT:
            return "Partial Content";
        case HttpServletResponse.SC_PAYMENT_REQUIRED:
            return "Payment Required";
        case HttpServletResponse.SC_PRECONDITION_FAILED:
            return "Precondition Failed";
        case HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED:
            return "Proxy Authentication Required";
        case HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE:
            return "Request Entity Too Large";
        case HttpServletResponse.SC_REQUEST_TIMEOUT:
            return "Request Timeout";
        case HttpServletResponse.SC_REQUEST_URI_TOO_LONG:
            return "Request URI Too Long";
        case HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE:
            return "Requested Range Not Satisfiable";
        case HttpServletResponse.SC_RESET_CONTENT:
            return "Reset Content";
        case HttpServletResponse.SC_SEE_OTHER:
            return "See Other";
        case HttpServletResponse.SC_SERVICE_UNAVAILABLE:
            return "Service Unavailable";
        case HttpServletResponse.SC_SWITCHING_PROTOCOLS:
            return "Switching Protocols";
        case HttpServletResponse.SC_UNAUTHORIZED:
            return "Unauthorized";
        case HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE:
            return "Unsupported Media Type";
        case HttpServletResponse.SC_USE_PROXY:
            return "Use Proxy";
        case 207:
            return "Multi-Status";
        case 422:
            return "Unprocessable Entity";
        case 423:
            return "Locked";
        case 507:
            return "Insufficient Storage";
        default:
            return "HTTP Response Status " + status;
        }
    }

    @Deprecated
    public void setStatus(int status, String message) {
        assertNotCommitted();
        this.status = status;
        this.message = message;
        resetBuffer();
    }

    private void assertNotCommitted() {
        if (isCommitted()) {
            throw new IllegalStateException("Already committed");
        }
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (getWriterCalled) {
            throw new IllegalStateException();
        }
        if (!getOutputStreamCalled) {
            getOutputStreamCalled = true;
        }
        return outputStream;
    }

    public PrintWriter getWriter() throws IOException {
        if (getOutputStreamCalled) {
            throw new IllegalStateException();
        }
        if (!getWriterCalled) {
            getWriterCalled = true;
        }
        return writer;
    }

    public void setContentLength(int contentLength) {
        setIntHeader("content-length", contentLength);
    }

    public int getContentLength() {
        return getIntHeader("content-length");
    }

    public String getContentType() {
        return getHeader("content-type");
    }

    public void setContentType(String contentType) {
        setHeader("content-type", contentType);
    }

    public void setBufferSize(int size) {
        assertNotCommitted();
        if (size <= buffer.length) {
            return;
        }
        buffer = new byte[size];
    }

    public int getBufferSize() {
        return buffer.length;
    }

    public void flushBuffer() throws IOException {
    }

    public void resetBuffer() {
        assertNotCommitted();
    }

    public boolean isCommitted() {
        return committed;
    }

    public void reset() {
        committed = false;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    public byte[] getResponseBytes() {
        return byteArrayOutputStream.toByteArray();
    }

    public String getResponseString() {
        return writer.toString();
    }
}
