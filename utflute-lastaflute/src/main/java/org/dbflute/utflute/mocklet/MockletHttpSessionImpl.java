/*
 * Copyright 2014-2024 the original author or authors.
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

import org.dbflute.utflute.mocklet.helper.MockletEnumerationAdapter;

import jakarta.servlet.ServletContext;

/**
 * @author modified by jflute (originated in Seasar)
 * @since 0.4.0 (2014/03/16 Sunday)
 */
public class MockletHttpSessionImpl implements MockletHttpSession, Serializable {

    private static final long serialVersionUID = 2182279632419560836L;

    protected final ServletContext servletContext;
    protected String id; // might be regenerate so not final
    protected final long creationTime = System.currentTimeMillis();
    protected long lastAccessedTime = creationTime;
    protected boolean new_ = true;
    protected boolean valid = true;
    protected int maxInactiveInterval = -1;
    protected final Map<String, Object> attributes = new HashMap<String, Object>();

    public MockletHttpSessionImpl(ServletContext servletContext) {
        this.servletContext = servletContext;
        this.id = prepareSessionId();
    }

    protected String prepareSessionId() { // you can override as you like it
        return String.valueOf(System.currentTimeMillis());
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getId() {
        return id;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void access() {
        new_ = false;
        lastAccessedTime = System.currentTimeMillis();
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Deprecated
    public Object getValue(String name) {
        return getAttribute(name);
    }

    public Enumeration<String> getAttributeNames() {
        return new MockletEnumerationAdapter<String>(attributes.keySet().iterator());
    }

    @Deprecated
    public String[] getValueNames() {
        return (String[]) attributes.keySet().toArray(new String[attributes.size()]);
    }

    @Deprecated
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    @Deprecated
    public void removeValue(String name) {
        removeAttribute(name);
    }

    public void invalidate() {
        if (!valid) {
            return;
        }
        attributes.clear();
        valid = false;
        id = prepareSessionId(); // regenerate
    }

    public boolean isNew() {
        return new_;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}