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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.dbflute.helper.beans.DfBeanDesc;
import org.dbflute.helper.beans.DfPropertyDesc;
import org.dbflute.helper.beans.factory.DfBeanDescFactory;
import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.utflute.core.binding.BoundResult.BoundField;
import org.dbflute.utflute.core.binding.BoundResult.BoundProperty;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.Srl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 0.1.0 (2011/07/24 Sunday)
 */
public class ComponentBinder {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger _logger = LoggerFactory.getLogger(ComponentBinder.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ComponentProvider _componentProvider;
    protected final BindingRuleProvider _bindingAnnotationProvider;
    protected final Map<Class<? extends Annotation>, BindingAnnotationRule> _bindingAnnotationRuleMap;
    protected Class<?> _terminalSuperClass;
    protected boolean _annotationOnlyBinding; // e.g. for Guice
    protected boolean _byTypeInterfaceOnly; // e.g. for Seasar
    protected boolean _looseBinding; // for test-case class
    protected boolean _overridingBinding; // for nested binding
    protected final List<Object> _mockInstanceList = DfCollectionUtil.newArrayList();
    protected final List<Class<?>> _nonBindingTypeList = DfCollectionUtil.newArrayList();
    protected final Map<Class<?>, Object> _nestedBindingMap = DfCollectionUtil.newHashMap();

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ComponentBinder(ComponentProvider componentProvider, BindingRuleProvider bindingAnnotationProvider) {
        _componentProvider = componentProvider;
        _bindingAnnotationProvider = bindingAnnotationProvider;
        _bindingAnnotationRuleMap = _bindingAnnotationProvider.provideBindingAnnotationRuleMap(); // cached
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    public void stopBindingAtSuper(Class<?> terminalSuperClass) {
        _terminalSuperClass = terminalSuperClass;
    }

    public void annotationOnlyBinding() {
        _annotationOnlyBinding = true;
    }

    public void cancelAnnotationOnlyBinding() {
        _annotationOnlyBinding = false;
    }

    public void byTypeInterfaceOnly() {
        _byTypeInterfaceOnly = true;
    }

    public void cancelByTypeInterfaceOnly() {
        _byTypeInterfaceOnly = false;
    }

    public void looseBinding() {
        _looseBinding = true;
    }

    public void cancelLooseBinding() {
        _looseBinding = false;
    }

    public void overridingBinding() {
        _overridingBinding = true;
    }

    public void cancelOverridingBinding() {
        _overridingBinding = false;
    }

    public void addMockInstance(Object mockInstance) {
        if (mockInstance == null) {
            String msg = "The argument 'mockInstance' should not be null.";
            throw new IllegalArgumentException(msg);
        }
        _mockInstanceList.add(mockInstance);
    }

    public void addNonBindingType(Class<?> nonBindingType) {
        if (nonBindingType == null) {
            String msg = "The argument 'nonBindingType' should not be null.";
            throw new IllegalArgumentException(msg);
        }
        _nonBindingTypeList.add(nonBindingType);
    }

    // cannot revert injected component reference, so emergency option e.g. HttpServletRequest
    public void addNestedBindingComponent(Class<?> bindingType, Object component) {
        if (bindingType == null) {
            String msg = "The argument 'bindingType' should not be null.";
            throw new IllegalArgumentException(msg);
        }
        _nestedBindingMap.put(bindingType, component);
    }

    protected void inheritParentBinderOption(ComponentBinder binder) {
        binder._terminalSuperClass = _terminalSuperClass;
        binder._annotationOnlyBinding = _annotationOnlyBinding;
        binder._byTypeInterfaceOnly = _byTypeInterfaceOnly;
        binder._looseBinding = _looseBinding;
        binder._overridingBinding = _overridingBinding;
        binder._mockInstanceList.addAll(_mockInstanceList);
        binder._nonBindingTypeList.addAll(_nonBindingTypeList);
        binder._nestedBindingMap.putAll(_nestedBindingMap);
    }

    // ===================================================================================
    //                                                                   Component Binding
    //                                                                   =================
    // -----------------------------------------------------
    //                                                 Entry
    //                                                 -----
    public BoundResult bindComponent(Object bean) {
        final BoundResult boundResult = new BoundResult(bean);
        doBindFieldComponent(bean, boundResult);
        doBindPropertyComponent(bean, boundResult);
        return boundResult;
    }

    // -----------------------------------------------------
    //                                         Field Binding
    //                                         -------------
    protected void doBindFieldComponent(Object bean, BoundResult boundResult) {
        for (Class<?> clazz = bean.getClass(); isBindTargetClass(clazz); clazz = clazz.getSuperclass()) {
            if (clazz == null) {
                break;
            }
            final Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                fireFieldBinding(bean, field, boundResult);
            }
        }
    }

    protected void fireFieldBinding(Object bean, Field field, BoundResult boundResult) {
        if (!isModifiersAutoBindable(field)) {
            return;
        }
        final Annotation bindingAnno = findBindingAnnotation(field); // might be null
        if (bindingAnno != null || _looseBinding) {
            field.setAccessible(true);
            final Class<?> fieldType = field.getType();
            if (isNonBindingType(fieldType)) {
                return;
            }
            if (isNonBindingAnnotation(bindingAnno)) {
                return;
            }
            if (!_overridingBinding && getFieldValue(field, bean) != null) {
                return;
            }
            final Object component = findInjectedComponent(field.getName(), fieldType, bindingAnno, boundResult);
            if (component != null) {
                final Object existing = extractExistingFieldValue(bean, field);
                setFieldValue(field, bean, component);
                boundResult.addBoundField(field, existing);
            }
        }
    }

    protected boolean isModifiersAutoBindable(Field field) {
        final int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers) && !field.getType().isPrimitive();
    }

    protected Object extractExistingFieldValue(Object bean, Field field) {
        return DfReflectionUtil.getValueForcedly(field, bean);
    }

    // -----------------------------------------------------
    //                                      Property Binding
    //                                      ----------------
    protected void doBindPropertyComponent(Object bean, BoundResult boundResult) {
        final DfBeanDesc beanDesc = DfBeanDescFactory.getBeanDesc(bean.getClass());
        final List<String> proppertyNameList = beanDesc.getProppertyNameList();
        for (String propertyName : proppertyNameList) {
            firePropertyBinding(bean, beanDesc, propertyName, boundResult);
        }
    }

    protected void firePropertyBinding(Object bean, DfBeanDesc beanDesc, String propertyName, BoundResult boundResult) {
        final DfPropertyDesc propertyDesc = beanDesc.getPropertyDesc(propertyName);
        if (!propertyDesc.isWritable()) {
            return;
        }
        final Class<?> propertyType = propertyDesc.getPropertyType();
        if (isNonBindingType(propertyType)) {
            return;
        }
        final Method writeMethod = propertyDesc.getWriteMethod();
        if (writeMethod == null) { // public field
            return; // unsupported fixedly
        }
        final Annotation bindingAnno = findBindingAnnotation(writeMethod); // might be null
        if (_annotationOnlyBinding && bindingAnno == null) {
            return; // e.g. Guice needs annotation to setter
        }
        if (isNonBindingAnnotation(bindingAnno)) {
            return;
        }
        if (!isBindTargetClass(writeMethod.getDeclaringClass())) {
            return;
        }
        if (!_overridingBinding && propertyDesc.isReadable() && propertyDesc.getValue(bean) != null) {
            return;
        }
        final Object component = findInjectedComponent(propertyName, propertyType, bindingAnno, boundResult);
        if (component == null) {
            // binder does not throw injection failure because it cannot check correctly
            // (you can test component building getComponent() easily instead, and also use police-story)
            return;
        }
        final Object existing = extractExistingPropertyValue(bean, propertyDesc);
        propertyDesc.setValue(bean, component);
        boundResult.addBoundProperty(propertyDesc, existing);
    }

    protected Object extractExistingPropertyValue(Object bean, DfPropertyDesc propertyDesc) {
        return propertyDesc.isReadable() ? propertyDesc.getValue(bean) : null;
    }

    // ===================================================================================
    //                                                                      Find Component
    //                                                                      ==============
    protected Object findInjectedComponent(String propertyName, Class<?> propertyType, Annotation bindingAnno, BoundResult boundResult) {
        final InjectedComponentContainer container = doFindInjectedComponent(propertyName, propertyType, bindingAnno);
        bindNestedBinding(container, boundResult);
        bindNestedMock(container, boundResult);
        return container.getInjected(); // null allowed
    }

    protected InjectedComponentContainer doFindInjectedComponent(String propertyName, Class<?> propertyType, Annotation bindingAnno) {
        final Object mock = findMockInstance(propertyType);
        if (mock != null) {
            return InjectedComponentContainer.ofMock(mock);
        }
        if (isFindingByNameOnlyProperty(propertyName, propertyType, bindingAnno)) {
            return InjectedComponentContainer.of(doFindInjectedComponentByName(propertyName, propertyType, bindingAnno));
        } else if (isFindingByTypeOnlyProperty(propertyName, propertyType, bindingAnno)) {
            return InjectedComponentContainer.of(doFindInjectedComponentByType(propertyType));
        }
        final Object byName = doFindInjectedComponentByName(propertyName, propertyType, bindingAnno);
        return InjectedComponentContainer.of(byName != null ? byName : doFindInjectedComponentByType(propertyType));
    }

    protected static class InjectedComponentContainer {

        protected final Object injected; // null allowed
        protected final boolean mocked;

        protected InjectedComponentContainer(Object injected, boolean mocked) {
            this.injected = injected;
            this.mocked = mocked;
        }

        public static InjectedComponentContainer of(Object injected) {
            return new InjectedComponentContainer(injected, false);
        }

        public static InjectedComponentContainer ofMock(Object mock) {
            return new InjectedComponentContainer(mock, true);
        }

        public Object getInjected() {
            return injected;
        }

        public boolean isMocked() {
            return mocked;
        }
    }

    protected Object findMockInstance(Class<?> type) {
        final List<Object> mockInstanceList = _mockInstanceList;
        for (Object mockInstance : mockInstanceList) {
            if (type.isInstance(mockInstance)) {
                return mockInstance;
            }
        }
        return null;
    }

    protected boolean isFindingByNameOnlyProperty(String propertyName, Class<?> propertyType, Annotation bindingAnno) {
        if (_looseBinding) {
            return false;
        }
        if (isByNameOnlyAnnotation(bindingAnno)) {
            return true;
        }
        if (extractSpecifiedName(bindingAnno) != null) {
            return true;
        }
        if (isLimitedPropertyAsByTypeInterfaceOnly(propertyName, propertyType)) {
            return true;
        }
        return false;
    }

    protected boolean isLimitedPropertyAsByTypeInterfaceOnly(String propertyName, Class<?> propertyType) {
        return _byTypeInterfaceOnly && !propertyType.isInterface();
    }

    protected boolean isFindingByTypeOnlyProperty(String propertyName, Class<?> propertyType, Annotation bindingAnno) {
        return isByTypeOnlyAnnotation(bindingAnno);
    }

    protected Object doFindInjectedComponentByName(String propertyName, Class<?> propertyType, Annotation bindingAnno) {
        final String specifiedName = extractSpecifiedName(bindingAnno);
        final String realName;
        if (specifiedName != null) {
            realName = specifiedName;
        } else {
            final String normalized = normalizeName(propertyName);
            final String filtered = _bindingAnnotationProvider.filterByBindingNamingRule(normalized, propertyType);
            realName = filtered != null ? filtered : normalized;
        }
        return actuallyFindInjectedComponentByName(realName);
    }

    protected Object actuallyFindInjectedComponentByName(String name) {
        return hasComponent(name) ? getComponent(name) : null;
    }

    protected Object doFindInjectedComponentByType(Class<?> propertyType) {
        return hasComponent(propertyType) ? getComponent(propertyType) : null;
    }

    protected String normalizeName(String name) {
        if (_looseBinding) {
            return name.startsWith("_") ? name.substring("_".length()) : name;
        }
        return name;
    }

    // ===================================================================================
    //                                                                Injection Annotation
    //                                                                ====================
    protected Annotation findBindingAnnotation(Field field) {
        return doFindBindingAnnotation(field.getAnnotations());
    }

    protected Annotation findBindingAnnotation(Method method) {
        return doFindBindingAnnotation(method.getAnnotations());
    }

    protected Annotation doFindBindingAnnotation(Annotation[] annotations) {
        if (annotations == null || _bindingAnnotationRuleMap == null) { // just in case
            return null;
        }
        for (Annotation annotation : annotations) {
            if (_bindingAnnotationRuleMap.containsKey(annotation.annotationType())) {
                return annotation;
            }
        }
        return null;
    }

    protected boolean isNonBindingAnnotation(Annotation bindingAnno) {
        final BindingAnnotationRule rule = findBindingAnnotationRule(bindingAnno);
        if (rule == null) {
            return false;
        }
        final NonBindingDeterminer determiner = rule.getNonBindingDeterminer();
        return determiner != null && determiner.isNonBinding(bindingAnno);
    }

    protected boolean isByNameOnlyAnnotation(Annotation bindingAnno) {
        final BindingAnnotationRule rule = findBindingAnnotationRule(bindingAnno);
        return rule != null && rule.isByNameOnly();
    }

    protected boolean isByTypeOnlyAnnotation(Annotation bindingAnno) {
        final BindingAnnotationRule rule = findBindingAnnotationRule(bindingAnno);
        return rule != null && rule.isByTypeOnly();
    }

    protected BindingAnnotationRule findBindingAnnotationRule(Annotation bindingAnno) {
        return bindingAnno != null ? _bindingAnnotationRuleMap.get(bindingAnno.annotationType()) : null;
    }

    // ===================================================================================
    //                                                                      Nested Binding
    //                                                                      ==============
    protected void bindNestedBinding(InjectedComponentContainer container, BoundResult boundResult) {
        final Object injected = container.getInjected();
        if (injected == null || _nestedBindingMap.isEmpty()) {
            return;
        }
        final ComponentBinder binder = new ComponentBinder(new ComponentProvider() {
            public <COMPONENT> COMPONENT provideComponent(String name) {
                return null;
            }

            @SuppressWarnings("unchecked")
            public <COMPONENT> COMPONENT provideComponent(Class<COMPONENT> type) {
                final COMPONENT specified = (COMPONENT) _nestedBindingMap.get(type);
                return specified != null ? specified : null;
            }

            public boolean existsComponent(String name) {
                return false;
            }

            public boolean existsComponent(Class<?> type) {
                return provideComponent(type) != null;
            }
        }, _bindingAnnotationProvider);
        inheritParentBinderOption(binder);
        binder._looseBinding = false; // because of container-managed component
        binder._overridingBinding = true; // because cannot remove reference
        final BoundResult nestedResult = binder.bindComponent(injected); // e.g. HttpServletRequest
        boundResult.addNestedBoundResult(nestedResult);
    }

    // ===================================================================================
    //                                                                         Nested Mock
    //                                                                         ===========
    protected void bindNestedMock(InjectedComponentContainer container, BoundResult boundResult) {
        final Object injected = container.getInjected();
        if (injected == null || _mockInstanceList.isEmpty()) {
            return;
        }
        final ComponentBinder binder = new ComponentBinder(new ComponentProvider() {
            public <COMPONENT> COMPONENT provideComponent(String name) {
                return null;
            }

            @SuppressWarnings("unchecked")
            public <COMPONENT> COMPONENT provideComponent(Class<COMPONENT> type) {
                return (COMPONENT) findMockInstance(type); // for nested mock
            }

            public boolean existsComponent(String name) {
                return false;
            }

            public boolean existsComponent(Class<?> type) {
                return provideComponent(type) != null;
            }
        }, _bindingAnnotationProvider);
        inheritParentBinderOption(binder);
        binder._looseBinding = false; // because may be container-managed component
        binder._overridingBinding = true; // for container-managed component's field
        final BoundResult nestedResult = binder.bindComponent(injected);
        boundResult.addNestedBoundResult(nestedResult);
    }

    // ===================================================================================
    //                                                                      Revert Binding
    //                                                                      ==============
    public void revertBoundComponent(BoundResult boundResult) {
        doRevertBoundComponent(boundResult);
    }

    public void revertBoundComponent(List<BoundResult> boundResultList) {
        for (BoundResult boundResult : orderRevertedList(boundResultList)) {
            doRevertBoundComponent(boundResult);
        }
    }

    protected void doRevertBoundComponent(BoundResult boundResult) {
        // needs to revert because it may be container-managed bean
        final Object bean = boundResult.getTargetBean();
        final List<BoundField> boundFieldList = boundResult.getBoundFieldList();
        for (BoundField boundField : orderRevertedList(boundFieldList)) {
            try {
                boundField.getField().set(bean, boundField.getExisting());
            } catch (Exception continued) { // because of not important but may need to debug so logging
                final String fileExp = buildRevertContinuedExp(continued);
                _logger.debug("*Cannot release bound field: target=" + bean + ", field=" + boundField + fileExp);
            }
        }
        boundFieldList.clear();
        final List<BoundProperty> boundPropertyList = boundResult.getBoundPropertyList();
        for (BoundProperty boundProperty : orderRevertedList(boundPropertyList)) {
            try {
                boundProperty.getPropertyDesc().setValue(bean, boundProperty.getExisting());
            } catch (Exception continued) { // because of not important but may need to debug so logging
                final String fileExp = buildRevertContinuedExp(continued);
                _logger.debug("*Cannot release bound property: target=" + bean + ", property=" + boundProperty + fileExp);
            }
        }
        boundPropertyList.clear();
        final List<BoundResult> nestedBoundResultList = boundResult.getNestedBoundResultList();
        for (BoundResult nestedBoundResult : orderRevertedList(nestedBoundResultList)) {
            doRevertBoundComponent(nestedBoundResult);
        }
    }

    protected <ELEMENT> List<ELEMENT> orderRevertedList(List<ELEMENT> originalList) {
        final List<ELEMENT> reversedList = new ArrayList<ELEMENT>(originalList);
        Collections.reverse(reversedList); // to avoid real component loss
        return reversedList;
    }

    protected String buildRevertContinuedExp(Exception continued) {
        final StringBuilder sb = new StringBuilder();
        final StackTraceElement[] stackTrace = continued.getStackTrace();
        if (stackTrace.length >= 1) {
            final StackTraceElement el = stackTrace[0];
            sb.append(", exception=");
            sb.append(continued.getClass().getSimpleName()).append("::").append(continued.getMessage());
            sb.append(" at ").append(el.getClassName()).append("@").append(el.getMethodName());
            sb.append("(").append(el.getFileName()).append(":").append(el.getLineNumber()).append(")");
        }
        return sb.toString();
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected boolean isBindTargetClass(Class<?> clazz) {
        return _terminalSuperClass == null || !clazz.isAssignableFrom(_terminalSuperClass);
    }

    protected boolean isNonBindingType(Class<?> bindingType) {
        if (determineFixedNonBindingType(bindingType)) {
            return true;
        }
        final List<Class<?>> nonBindingTypeList = _nonBindingTypeList;
        for (Class<?> nonBindingType : nonBindingTypeList) {
            if (nonBindingType.isAssignableFrom(bindingType)) {
                return true;
            }
        }
        return false;
    }

    protected boolean determineFixedNonBindingType(Class<?> bindingType) {
        // to avoid e.g. SessionManager@setAttribute(Object)
        return Object.class.equals(bindingType); // too abstract
    }

    protected String extractSpecifiedName(Annotation bindingAnnotation) {
        String specifiedName = null;
        if (bindingAnnotation instanceof Resource) { // only standard annotation here for now
            specifiedName = ((Resource) bindingAnnotation).name(); // might be empty string
        }
        return Srl.is_NotNull_and_NotTrimmedEmpty(specifiedName) ? specifiedName : null;
    }

    // ===================================================================================
    //                                                                        Field Helper
    //                                                                        ============
    protected Object getFieldValue(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalArgumentException e) {
            throwIllegalArgumentFieldGet(field, target, e);
            return null; // unreachable
        } catch (IllegalAccessException e) {
            throwIllegalAccessFieldGet(field, target, e);
            return null; // unreachable
        }
    }

    protected void throwIllegalArgumentFieldGet(Field field, Object target, IllegalArgumentException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Illegal argument to get the field.");
        br.addItem("Field");
        br.addElement(field);
        br.addItem("Target");
        br.addElement(target);
        final String msg = br.buildExceptionMessage();
        throw new IllegalArgumentException(msg, e);
    }

    protected void throwIllegalAccessFieldGet(Field field, Object target, IllegalAccessException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Illegal access to get the field.");
        br.addItem("Field");
        br.addElement(field);
        br.addItem("Target");
        br.addElement(target);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg, e);
    }

    protected void setFieldValue(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalArgumentException e) {
            throwIllegalArgumentFieldSet(field, target, value, e);
        } catch (IllegalAccessException e) {
            throwIllegalAccessFieldSet(field, target, value, e);
        }
    }

    protected void throwIllegalArgumentFieldSet(Field field, Object target, Object value, IllegalArgumentException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Illegal argument to set the field.");
        br.addItem("Field");
        br.addElement(field);
        br.addItem("Target");
        br.addElement(target);
        br.addItem("Value");
        br.addElement(value != null ? value.getClass() : null);
        br.addElement(value);
        final String msg = br.buildExceptionMessage();
        throw new IllegalArgumentException(msg, e);
    }

    protected void throwIllegalAccessFieldSet(Field field, Object target, Object value, IllegalAccessException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Illegal access to set the field.");
        br.addItem("Field");
        br.addElement(field);
        br.addItem("Target");
        br.addElement(target);
        br.addItem("Value");
        br.addElement(value != null ? value.getClass() : null);
        br.addElement(value);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg, e);
    }

    // ===================================================================================
    //                                                                       Bean Handling
    //                                                                       =============
    protected <COMPONENT> COMPONENT getComponent(Class<COMPONENT> type) {
        return _componentProvider.provideComponent(type);
    }

    @SuppressWarnings("unchecked")
    protected <COMPONENT> COMPONENT getComponent(String name) {
        return (COMPONENT) _componentProvider.provideComponent(name);
    }

    protected boolean hasComponent(Class<?> type) {
        return _componentProvider.existsComponent(type);
    }

    protected boolean hasComponent(String name) {
        return _componentProvider.existsComponent(name);
    }
}
