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
package org.dbflute.utflute.lastaflute.mock;

import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
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
    protected final RoutingNext nextRouting; // not null
    protected final Map<String, Object> dataMap; // not null
    protected final OptionalThing<Object> pushedForm; // not null, empty allowed

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TestingHtmlData(RoutingNext nextRouting, Map<String, Object> dataMap, OptionalThing<Object> pushedForm) {
        this.nextRouting = nextRouting;
        this.dataMap = dataMap;
        this.pushedForm = pushedForm;
    }

    // ===================================================================================
    //                                                                        Testing Tool
    //                                                                        ============
    // -----------------------------------------------------
    //                                          Next Routing
    //                                          ------------
    // #thinking needed? (and how do I resolve redirect problem?)
    //public void assertRoutingToHtmlForward(HtmlNext htmlNext) {
    //    assertTrue(isRoutingAsHtmlForward());
    //    assertEquals(htmlNext.getRoutingPath(), nextRouting.getRoutingPath());
    //    assertEquals(htmlNext.isAsIs(), nextRouting.isAsIs());
    //}
    // needs ActionPathResolver or RedirectNext should have action type
    //public void assertRoutingToRedirect(Class<?> actionType) {
    //    assertTrue(nextRouting instanceof RedirectNext);
    //}

    public boolean isRoutingAsForward() {
        return nextRouting instanceof ForwardNext;
    }

    public boolean isRoutingAsHtmlForward() {
        return nextRouting instanceof HtmlNext;
    }

    public boolean isRoutingAsRedirect() {
        return nextRouting instanceof RedirectNext;
    }

    // -----------------------------------------------------
    //                                              Data Map
    //                                              --------
    public <VALUE> VALUE required(String key, Class<VALUE> valueType) {
        final Object value = dataMap.get(key);
        assertTrue(value != null);
        assertTrue(valueType.isAssignableFrom(value.getClass()));
        @SuppressWarnings("unchecked")
        final VALUE cast = (VALUE) value;
        return cast;
    }

    public <ELEMENT> List<ELEMENT> requiredList(String key, Class<ELEMENT> elementType) {
        @SuppressWarnings("unchecked")
        final List<ELEMENT> list = (List<ELEMENT>) dataMap.get(key);
        assertTrue(list != null);
        assertTrue(!list.isEmpty());
        assertTrue(elementType.isAssignableFrom(list.get(0).getClass()));
        return list;
    }

    // -----------------------------------------------------
    //                                           Pushed Form
    //                                           -----------
    public <FORM> FORM requiredPushedForm(Class<FORM> formType) {
        assertTrue(pushedForm.isPresent());
        @SuppressWarnings("unchecked")
        final FORM form = (FORM) pushedForm.get();
        return form;
    }

    // -----------------------------------------------------
    //                                         Assert Helper
    //                                         -------------
    protected void assertTrue(boolean condition) {
        Assert.assertTrue(condition);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public RoutingNext getNextRouting() {
        return nextRouting;
    }

    public Map<String, Object> getDataMap() {
        return dataMap;
    }

    public OptionalThing<Object> getPushedForm() {
        return pushedForm;
    }
}
