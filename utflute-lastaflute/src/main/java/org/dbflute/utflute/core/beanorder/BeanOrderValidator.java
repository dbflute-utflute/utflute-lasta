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
package org.dbflute.utflute.core.beanorder;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @param <BEAN> The type of bean.
 * @author jflute
 */
public class BeanOrderValidator<BEAN> {

    protected final Consumer<ExpectedBeanOrderBy<BEAN>> orderBySpecCall;

    public BeanOrderValidator(Consumer<ExpectedBeanOrderBy<BEAN>> orderBySpecCall) {
        this.orderBySpecCall = orderBySpecCall;
    }

    public void validateOrder(List<BEAN> beanList, Consumer<BeanOrderByViolation<BEAN>> violationCall) {
        final ExpectedBeanOrderBy<BEAN> orderBy = new ExpectedBeanOrderBy<BEAN>();
        orderBySpecCall.accept(orderBy);
        final List<BeanOrderBySpec<BEAN>> orderBySpecList = orderBy.getOrderBySpecList();
        final Map<Integer, BeanOrderDiffMark> diffMarkMap = new LinkedHashMap<>();
        BEAN previousBean = null;
        for (BEAN nextBean : beanList) {
            if (previousBean != null) {
                doAssertOrder(previousBean, nextBean, orderBySpecList.iterator(), violationCall, 1, diffMarkMap);
            }
            previousBean = nextBean;
        }
        verifyDiff(orderBySpecList, diffMarkMap);
    }

    protected void verifyDiff(List<BeanOrderBySpec<BEAN>> orderBySpecList, Map<Integer, BeanOrderDiffMark> diffMarkMap) {
        for (BeanOrderBySpec<BEAN> spec : orderBySpecList) {
            final int specNo = spec.getSpecNo();
            final BeanOrderDiffMark diffMark = diffMarkMap.get(specNo);
            if (diffMark == null || !diffMark.isDiff()) {
                throw new IllegalStateException("Not found the difference for the spec number: " + specNo);
            }
        }
    }

    protected void doAssertOrder(BEAN previousBean, BEAN nextBean, Iterator<BeanOrderBySpec<BEAN>> specIterator,
            Consumer<BeanOrderByViolation<BEAN>> violationCall, int specNo, Map<Integer, BeanOrderDiffMark> diffMarkMap) {
        if (!specIterator.hasNext()) {
            return;
        }
        final BeanOrderBySpec<BEAN> spec = specIterator.next();
        final Function<BEAN, Comparable<? extends Object>> provider = spec.getValueProvider();
        @SuppressWarnings("unchecked")
        final Comparable<Object> previousValue = (Comparable<Object>) provider.apply(previousBean);
        @SuppressWarnings("unchecked")
        final Comparable<Object> nextValue = (Comparable<Object>) provider.apply(nextBean);
        final int compareTo = compareTo(spec, previousValue, nextValue);
        if (spec.isAsc() ? compareTo > 0 : compareTo < 0) {
            violationCall.accept(createOrderByViolation(specNo, previousBean, nextBean, previousValue, nextValue));
        } else if (compareTo == 0) {
            doAssertOrder(previousBean, nextBean, specIterator, violationCall, specNo + 1, diffMarkMap);
        }
        if (compareTo != 0) {
            if (!diffMarkMap.containsKey(specNo)) {
                diffMarkMap.put(specNo, new BeanOrderDiffMark(specNo).markDiff());
            }
        }
    }

    protected int compareTo(BeanOrderBySpec<BEAN> spec, Comparable<Object> previousValue, Comparable<Object> nextValue) {
        if (previousValue == null && nextValue == null) {
            return 0;
        } else if (previousValue == null) {
            return (spec.isAsc() ? 1 : -1) * (spec.isNullsFirst() ? -1 : 1); // nulls last as default
        } else if (nextValue == null) {
            return (spec.isAsc() ? 1 : -1) * (spec.isNullsFirst() ? 1 : -1);
        } else {
            return previousValue.compareTo(nextValue);
        }
    }

    protected BeanOrderByViolation<BEAN> createOrderByViolation(int specNo, BEAN previousBean, BEAN nextBean,
            Comparable<Object> previousValue, Comparable<Object> nextValue) {
        return new BeanOrderByViolation<>(specNo, previousBean, nextBean, previousValue, nextValue);
    }
}
