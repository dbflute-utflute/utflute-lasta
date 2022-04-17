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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.dbflute.utflute.mocklet.helper.MockletEnumerationAdapter;

/**
 * @author modified by jflute (originated in Seasar)
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class MockletServletConfigImpl implements MockletServletConfig, Serializable {

    private static final long serialVersionUID = 5515573574823840162L;

    protected String servletName;
    protected ServletContext servletContext;
    protected final Map<String, String> initParameters = new HashMap<String, String>();

    public MockletServletConfigImpl() {
    }

    public String getServletName() {
        return servletName;
    }

    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public String getInitParameter(String name) {
        return (String) initParameters.get(name);
    }

    public void setInitParameter(String name, String value) {
        initParameters.put(name, value);
    }

    public Enumeration<String> getInitParameterNames() {
        return new MockletEnumerationAdapter<String>(initParameters.keySet().iterator());
    }
}