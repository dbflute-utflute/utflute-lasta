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
package org.dbflute.utflute.lastaflute.mock;

import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.web.path.ActionPathResolver;
import org.lastaflute.web.response.next.ForwardNext;
import org.lastaflute.web.response.next.HtmlNext;
import org.lastaflute.web.response.next.RedirectNext;
import org.lastaflute.web.response.next.RoutingNext;

import junit.framework.Assert;

/**
 * @author jflute
 */
public class TestingHtmlData {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RoutingNext _nextRouting; // not null
    protected final Map<String, Object> _dataMap; // not null
    protected final OptionalThing<Object> _pushedForm; // not null, empty allowed

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TestingHtmlData(RoutingNext nextRouting, Map<String, Object> dataMap, OptionalThing<Object> pushedForm) {
        _nextRouting = nextRouting;
        _dataMap = dataMap;
        _pushedForm = pushedForm;
    }

    // ===================================================================================
    //                                                                        Testing Tool
    //                                                                        ============
    // -----------------------------------------------------
    //                                          Next Routing
    //                                          ------------
    public void assertHtmlForward(HtmlNext htmlNext) {
        assertTrue("Not HTML forward response: " + _nextRouting, isRoutingAsHtmlForward());
        assertEquals(htmlNext.getRoutingPath(), _nextRouting.getRoutingPath());
    }

    public void assertRedirect(Class<?> actionType) {
        // #hope make overload method using UrlChain
        assertTrue("Not redirect response: " + _nextRouting, isRoutingAsRedirect());
        doAssertRedirectOrForward("redirect", actionType);
    }

    public void assertSimpleForward(Class<?> actionType) {
        assertTrue("Not (simple) forward response: " + _nextRouting, isRoutingAsSimpleForward());
        doAssertRedirectOrForward("forward", actionType);
    }

    protected void doAssertRedirectOrForward(String type, Class<?> actionType) {
        final String actionUrl = toActionUrl(actionType);
        final String routingPath = _nextRouting.getRoutingPath();
        final boolean result = routingPath.startsWith(actionUrl);
        assertTrue("Wrong action " + type + ": expected=" + actionUrl + ", actual" + routingPath, result);
    }

    protected String toActionUrl(Class<?> actionType) {
        // #hope get it from requestManager or response saves action type
        return getActionPathResolver().toActionUrl(actionType);
    }

    public boolean isRoutingAsHtmlForward() {
        return _nextRouting instanceof HtmlNext;
    }

    public boolean isRoutingAsRedirect() {
        return _nextRouting instanceof RedirectNext;
    }

    public boolean isRoutingAsSimpleForward() {
        return _nextRouting instanceof ForwardNext;
    }

    // -----------------------------------------------------
    //                                              Data Map
    //                                              --------
    public <VALUE> VALUE required(String key, Class<VALUE> valueType) {
        final Object value = _dataMap.get(key);
        assertTrue("Not found the value: key=" + key + ", dataMap=" + _dataMap.keySet(), value != null);
        final Class<? extends Object> actualType = value.getClass();
        assertTrue("Cannot cast the value: expected=" + valueType + ", actual=" + actualType, valueType.isAssignableFrom(actualType));
        @SuppressWarnings("unchecked")
        final VALUE cast = (VALUE) value;
        return cast;
    }

    public <ELEMENT> List<ELEMENT> requiredList(String key, Class<ELEMENT> elementType) {
        @SuppressWarnings("unchecked")
        final List<ELEMENT> list = (List<ELEMENT>) _dataMap.get(key);
        assertListNotNull(key, list);
        assertListHasAnyElement(key, list);
        assertListHasSpecifiedElementType(elementType, list);
        return list;
    }

    protected <ELEMENT> void assertListNotNull(String key, List<ELEMENT> list) {
        assertTrue("Not found the list: key=" + key + ", dataMap=" + _dataMap.keySet(), list != null);
    }

    protected <ELEMENT> void assertListHasAnyElement(String key, List<ELEMENT> list) {
        assertTrue("Found the empty list: key=" + key, !list.isEmpty());
    }

    protected <ELEMENT> void assertListHasSpecifiedElementType(Class<ELEMENT> elementType, List<ELEMENT> list) {
        final Class<? extends Object> firstType = list.get(0).getClass();
        final boolean result = elementType.isAssignableFrom(firstType);
        assertTrue("Cannot cast the list element: expected=" + elementType + ", actual=" + firstType, result);
    }

    // -----------------------------------------------------
    //                                           Pushed Form
    //                                           -----------
    public <FORM> FORM requiredPushedForm(Class<FORM> formType) {
        assertTrue("Not found the pushed form: formType=" + formType, _pushedForm.isPresent());
        @SuppressWarnings("unchecked")
        final FORM form = (FORM) _pushedForm.get();
        return form;
    }

    // -----------------------------------------------------
    //                                         Assert Helper
    //                                         -------------
    protected void assertEquals(Object expected, Object actual) {
        Assert.assertEquals(expected, actual);
    }

    protected void assertTrue(String msg, boolean condition) {
        Assert.assertTrue(msg, condition);
    }

    // ===================================================================================
    //                                                                          Components
    //                                                                          ==========
    protected ActionPathResolver getActionPathResolver() {
        return ContainerUtil.getComponent(ActionPathResolver.class);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public RoutingNext getNextRouting() {
        return _nextRouting;
    }

    public Map<String, Object> getDataMap() {
        return _dataMap;
    }

    public OptionalThing<Object> getPushedForm() {
        return _pushedForm;
    }
}
