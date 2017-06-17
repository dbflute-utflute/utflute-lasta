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

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.dbflute.utflute.mocklet.helper.MockletEnumerationAdapter;
import org.dbflute.util.DfResourceUtil;

/**
 * @author modified by jflute (originated in Seasar)
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class MockletServletContextImpl implements MockletServletContext, Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = -5626752218858278823L;

    public static final int MAJOR_VERSION = 2;
    public static final int MINOR_VERSION = 4;
    public static final String SERVER_INFO = "utflute";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected String servletContextName; // mutable
    protected String contextPath; // mutable
    protected final Map<String, String> mimeTypes = new HashMap<String, String>();
    protected final Map<String, String> initParameters = new HashMap<String, String>();
    protected final Map<String, Object> attributes = new HashMap<String, Object>();

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MockletServletContextImpl(String path) {
        if (path == null) {
            throw new IllegalArgumentException("The argument 'path' should not be null.");
        }
        this.contextPath = path;
    }

    // ===================================================================================
    //                                                                             Servlet
    //                                                                             =======
    public String getServletContextName() {
        return servletContextName;
    }

    public String getContextPath() {
        return contextPath;
    }

    public ServletContext getContext(String path) {
        throw new UnsupportedOperationException();
    }

    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    public String getMimeType(String file) {
        return mimeTypes.get(file);
    }

    public Set<String> getResourcePaths(String path) {
        path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        File src = getFile(DfResourceUtil.getResourceUrl("."));
        File root = src.getParentFile();
        if (root.getName().equalsIgnoreCase("WEB-INF")) {
            root = root.getParentFile();
        }
        File file = new File(root, adjustPath(path));
        if (!file.exists()) {
            int pos = path.lastIndexOf('/');
            if (pos != -1) {
                path = path.substring(pos + 1);
            }
            do {
                file = new File(root, path);
                root = root.getParentFile();
            } while (!file.exists() && root != null);
            path = "/" + path;
        }
        if (file.isDirectory()) {
            int len = file.getAbsolutePath().length();
            Set<String> paths = new HashSet<String>();
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; ++i) {
                    paths.add(path + files[i].getAbsolutePath().substring(len).replace('\\', '/'));
                }
                return paths;
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public URL getResource(String path) throws MalformedURLException {
        if (path == null) {
            return null;
        }
        path = adjustPath(path);
        File src = getFile(DfResourceUtil.getResourceUrl("."));
        File root = src.getParentFile();
        if (root.getName().equalsIgnoreCase("WEB-INF")) {
            root = root.getParentFile();
        }
        while (root != null) {
            File file = new File(root, path);
            if (file.exists()) {
                return file.toURL();
            }
            root = root.getParentFile();
        }
        if (DfResourceUtil.isExist(path)) {
            return DfResourceUtil.getResourceUrl(path);
        }
        if (path.startsWith("WEB-INF")) {
            path = path.substring("WEB-INF".length());
            return getResource(path);
        }
        return null;
    }

    protected File getFile(URL url) {
        File file = new File(DfResourceUtil.getFileName(url));
        if (file != null && file.exists()) {
            return file;
        }
        return null;
    }

    public InputStream getResourceAsStream(String path) {
        if (path == null) {
            return null;
        }
        path = adjustPath(path);
        if (DfResourceUtil.isExist(path)) {
            return DfResourceUtil.getResourceStream(path);
        }
        if (path.startsWith("WEB-INF")) {
            path = path.substring("WEB-INF".length());
            return getResourceAsStream(path);
        }
        return null;
    }

    protected String adjustPath(String path) {
        if (path != null && path.length() > 0 && path.charAt(0) == '/') {
            return path.substring(1);
        }
        return path;
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return new MockletRequestDispatcherImpl();
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public Servlet getServlet(String name) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("rawtypes")
    @Deprecated
    public Enumeration getServlets() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("rawtypes")
    @Deprecated
    public Enumeration getServletNames() {
        throw new UnsupportedOperationException();
    }

    public void log(String message) {
        System.out.println(message);
    }

    @Deprecated
    public void log(Exception ex, String message) {
        System.out.println(message);
        ex.printStackTrace();
    }

    public void log(String message, Throwable t) {
        System.out.println(message);
        t.printStackTrace();
    }

    public String getRealPath(String path) {
        try {
            return DfResourceUtil.getResourceUrl(adjustPath(path)).getFile();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public String getServerInfo() {
        return SERVER_INFO;
    }

    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    public Enumeration<String> getInitParameterNames() {
        return new MockletEnumerationAdapter<String>(initParameters.keySet().iterator());
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

    // ===================================================================================
    //                                                                       Mock Handling
    //                                                                       =============
    public void addMimeType(String file, String type) {
        mimeTypes.put(file, type);
    }

    public void setInitParameter(String name, String value) {
        initParameters.put(name, value);
    }

    public MockletHttpServletRequest createRequest(String path) {
        String queryString = null;
        int question = path.indexOf('?');
        if (question >= 0) {
            queryString = path.substring(question + 1);
            path = path.substring(0, question);
        }
        MockletHttpServletRequestImpl request = new MockletHttpServletRequestImpl(this, path);
        request.setQueryString(queryString);
        return request;
    }

    public void setServletContextName(String servletContextName) {
        this.servletContextName = servletContextName;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public Map<String, String> getInitParameterMap() {
        return initParameters;
    }
}
