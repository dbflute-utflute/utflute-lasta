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
    protected final List<BoundField> _boundFieldList = new ArrayList<BoundField>();
    protected final List<BoundProperty> _boundPropertyList = new ArrayList<BoundProperty>();
    protected final List<BoundResult> _nestedBoundResultList = new ArrayList<BoundResult>();

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public BoundResult(Object targetBean) {
        _targetBean = targetBean;
    }

    public static class BoundField {

        protected final Field field;
        protected final Object existing; // null allowed

        public BoundField(Field field, Object existing) {
            this.field = field;
            this.existing = existing;
        }

        @Override
        public String toString() {
            return "boundField:{" + field + ", " + existing + "}";
        }

        public Field getField() {
            return field;
        }

        public Object getExisting() {
            return existing;
        }
    }

    public static class BoundProperty {

        protected final DfPropertyDesc propertyDesc;
        protected final Object existing; // null allowed

        public BoundProperty(DfPropertyDesc propertyDesc, Object existing) {
            this.propertyDesc = propertyDesc;
            this.existing = existing;
        }

        @Override
        public String toString() {
            return "boundProperty:{" + propertyDesc + ", " + existing + "}";
        }

        public DfPropertyDesc getPropertyDesc() {
            return propertyDesc;
        }

        public Object getExisting() {
            return existing;
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "boundResult:{" + _targetBean + ", " + _boundFieldList + ", " + _boundPropertyList + ", " + _nestedBoundResultList + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public Object getTargetBean() {
        return _targetBean;
    }

    public List<BoundField> getBoundFieldList() {
        return _boundFieldList;
    }

    public void addBoundField(Field field, Object existing) {
        _boundFieldList.add(new BoundField(field, existing));
    }

    public void addBoundFieldAll(List<BoundField> boundFieldList) {
        _boundFieldList.addAll(boundFieldList);
    }

    public List<BoundProperty> getBoundPropertyList() {
        return _boundPropertyList;
    }

    public void addBoundProperty(DfPropertyDesc boundProperty, Object existing) {
        _boundPropertyList.add(new BoundProperty(boundProperty, existing));
    }

    public void addBoundPropertyAll(List<BoundProperty> boundPropertyList) {
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
