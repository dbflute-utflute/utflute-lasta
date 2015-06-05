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
package org.dbflute.utflute.core.binding;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.dbflute.helper.beans.DfPropertyDesc;

/**
 * @author jflute
 * @since 0.1.0 (2011/07/24 Sunday)
 */
public class BoundResult {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Object _targetBean;
    protected final List<Field> _boundFieldList = new ArrayList<Field>();
    protected final List<DfPropertyDesc> _boundPropertyList = new ArrayList<DfPropertyDesc>();
    protected final List<BoundResult> _nestedBoundResultList = new ArrayList<BoundResult>();

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public BoundResult(Object targetBean) {
        _targetBean = targetBean;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public Object getTargetBean() {
        return _targetBean;
    }

    public List<Field> getBoundFieldList() {
        return _boundFieldList;
    }

    public void addBoundField(Field boundField) {
        _boundFieldList.add(boundField);
    }

    public void addBoundFieldAll(List<Field> boundFieldList) {
        _boundFieldList.addAll(boundFieldList);
    }

    public List<DfPropertyDesc> getBoundPropertyList() {
        return _boundPropertyList;
    }

    public void addBoundProperty(DfPropertyDesc boundProperty) {
        _boundPropertyList.add(boundProperty);
    }

    public void addBoundPropertyAll(List<DfPropertyDesc> boundPropertyList) {
        _boundPropertyList.addAll(boundPropertyList);
    }

    public List<BoundResult> getNestedBoundResultList() {
        return _nestedBoundResultList;
    }

    public void addNestedBoundResult(BoundResult nestedBoundResult) {
        _nestedBoundResultList.add(nestedBoundResult);
    }

    public void addNestedBoundResultAll(List<BoundResult> nestedBoundResultList) {
        _nestedBoundResultList.addAll(nestedBoundResultList);
    }
}
