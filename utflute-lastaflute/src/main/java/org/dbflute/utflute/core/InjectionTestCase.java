/*
 * Copyright 2014-2021 the original author or authors.
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
package org.dbflute.utflute.core;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dbflute.utflute.core.binding.BindingAnnotationRule;
import org.dbflute.utflute.core.binding.BindingRuleProvider;
import org.dbflute.utflute.core.binding.BoundResult;
import org.dbflute.utflute.core.binding.ComponentBinder;
import org.dbflute.utflute.core.binding.ComponentProvider;
import org.dbflute.utflute.core.transaction.TransactionFailureException;
import org.dbflute.utflute.core.transaction.TransactionResource;

/**
 * @author jflute
 * @since 0.1.2 (2011/09/16 Friday)
 */
public abstract class InjectionTestCase extends PlainTestCase {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                    Transaction Object
    //                                    ------------------
    /** The object that has transaction resources for test case. */
    private TransactionResource _xtestCaseTransactionResource;

    // -----------------------------------------------------
    //                                     Component Binding
    //                                     -----------------
    /** The binder of component for the test case. (NotNull) */
    private final ComponentBinder _xtestCaseComponentBinder = createTestCaseComponentBinder();

    /** The result of bound component for the test case. (NullAllowed: before binding, after destroy) */
    private BoundResult _xtestCaseBoundResult;

    /** The list of injected bound result. (NullAllowed: before binding, after destroy) */
    private List<BoundResult> _xinjectedBoundResultList; // lazy-loaded

    /** The list of mock instance injected to component. (NullAllowed: when no mock) */
    private List<Object> _xmockInstanceList; // lazy-loaded

    /** The list of non-binding type NOT injected to component. (NullAllowed: when no mock) */
    private List<Class<?>> _xnonBindingTypeList; // lazy-loaded

    // ===================================================================================
    //                                                                            Settings
    //                                                                            ========
    // -----------------------------------------------------
    //                                                Set up
    //                                                ------
    @Override
    public void setUp() throws Exception {
        super.setUp();

        xsetupBeforeContainer();
        xsetupBeforeTestCaseContainer();
        xprepareTestCaseContainer();
        xsetupAfterTestCaseContainer();

        xsetupBeforeTestCaseInjection();
        xprepareTestCaseInjection();
        xsetupAfterTestCaseInjection();

        xsetupBeforeTestCaseTransaction();
        xprepareTestCaseTransaction();
        xsetupAfterTestCaseTransaction();
    }

    // -----------------------------------------------------
    //                                     setUp() Container
    //                                     -----------------
    @Deprecated
    protected void xsetupBeforeContainer() { // use xsetupBeforeTestCaseContainer()
    }

    protected void xsetupBeforeTestCaseContainer() {
    }

    protected abstract void xprepareTestCaseContainer();

    protected void xsetupAfterTestCaseContainer() {
    }

    /**
     * Does it use one-time container? (re-initialize container per one test case?)
     * @return The determination, true or false.
     */
    protected boolean isUseOneTimeContainer() { // you can override
        return false;
    }

    // -----------------------------------------------------
    //                                     setUp() Injection
    //                                     -----------------
    protected void xsetupBeforeTestCaseInjection() {
    }

    protected void xprepareTestCaseInjection() {
        _xtestCaseBoundResult = _xtestCaseComponentBinder.bindComponent(this);
    }

    protected void xsetupAfterTestCaseInjection() {
    }

    // -----------------------------------------------------
    //                                   setUp() Transaction
    //                                   -------------------
    protected void xsetupBeforeTestCaseTransaction() {
    }

    protected void xprepareTestCaseTransaction() {
        if (!isSuppressTestCaseTransaction()) {
            xbeginTestCaseTransaction();
        }
    }

    /**
     * Does it suppress transaction for the test case? (non-transaction as default?)
     * @return The determination, true or false.
     */
    protected boolean isSuppressTestCaseTransaction() { // you can override
        return false; // default is to use the transaction
    }

    protected void xbeginTestCaseTransaction() {
        _xtestCaseTransactionResource = beginNewTransaction();
    }

    protected void xsetupAfterTestCaseTransaction() {
    }

    // -----------------------------------------------------
    //                                             Tear Down
    //                                             ---------
    @Override
    public void tearDown() throws Exception {
        if (!isSuppressTestCaseTransaction()) {
            xrollbackTestCaseTransaction(); // should be tear-down to close transaction when failure 
        }
        xdestroyTestCaseInjection();
        xdestroyTestCaseContainer();
        _xmockInstanceList = null;
        _xnonBindingTypeList = null;
        super.tearDown();
    }

    // -----------------------------------------------------
    //                                teatDown() Transaction
    //                                ----------------------
    protected void xrollbackTestCaseTransaction() {
        if (_xtestCaseTransactionResource == null) { // just in case
            return;
        }
        if (isCommitTestCaseTransaction()) {
            commitTransaction(_xtestCaseTransactionResource);
        } else {
            rollbackTransaction(_xtestCaseTransactionResource);
        }
        _xtestCaseTransactionResource = null;
    }

    /**
     * Does it commit transaction for the test case? (commit updated data?)
     * @return The determination, true or false.
     */
    protected boolean isCommitTestCaseTransaction() { // you can override
        return false; // default is to roll-back always
    }

    @Override
    protected void commitTransaction(TransactionResource resource) { // user method
        xassertTransactionResourceNotNull(resource);
        try {
            resource.commit();
        } catch (Exception e) {
            String msg = "Failed to commit the transaction: " + resource;
            throw new TransactionFailureException(msg, e);
        }
    }

    @Override
    protected void rollbackTransaction(TransactionResource resource) { // user method
        xassertTransactionResourceNotNull(resource);
        try {
            resource.rollback();
        } catch (Exception e) {
            String msg = "Failed to roll-back the transaction: " + resource;
            throw new TransactionFailureException(msg, e);
        }
    }

    // -----------------------------------------------------
    //                                  teatDown() Injection
    //                                  --------------------
    protected void xdestroyTestCaseInjection() {
        _xtestCaseComponentBinder.revertBoundComponent(_xtestCaseBoundResult);
        _xtestCaseBoundResult = null;
        if (_xinjectedBoundResultList != null) {
            _xtestCaseComponentBinder.revertBoundComponent(_xinjectedBoundResultList);
        }
        _xinjectedBoundResultList = null;
    }

    // -----------------------------------------------------
    //                                  teatDown() Container
    //                                  --------------------
    protected void xdestroyTestCaseContainer() {
        if (isUseOneTimeContainer() || isDestroyContainerAtTearDown()) {
            xdestroyContainer();
            xclearCachedContainer();
        }
    }

    /**
     * Does it destroy container instance at tear-down? (next test uses new-created container?)
     * @return The determination, true or false.
     */
    protected boolean isDestroyContainerAtTearDown() { // you can override
        return false; // default is to cache the instance
    }

    protected abstract void xclearCachedContainer();

    // ===================================================================================
    //                                                                   Component Binding
    //                                                                   =================
    // -----------------------------------------------------
    //                                                Binder
    //                                                ------
    protected ComponentBinder xcreateBasicComponentBinder() { // customize point
        return new ComponentBinder(xcreateComponentProvider(), createBindingRuleProvider());
    }

    protected ComponentProvider xcreateComponentProvider() {
        return new ComponentProvider() {

            public <COMPONENT> COMPONENT provideComponent(Class<COMPONENT> type) {
                return getComponent(type);
            }

            @SuppressWarnings("unchecked")
            public <COMPONENT> COMPONENT provideComponent(String name) {
                return (COMPONENT) getComponent(name);
            }

            public boolean existsComponent(Class<?> type) {
                return hasComponent(type);
            }

            public boolean existsComponent(String name) {
                return hasComponent(name);
            }
        };
    }

    protected ComponentBinder createTestCaseComponentBinder() { // you can override
        final ComponentBinder binder = xcreateBasicComponentBinder();
        binder.stopBindingAtSuper(InjectionTestCase.class);
        if (isUseTestCaseLooseBinding()) {
            binder.looseBinding();
        }
        return binder;
    }

    protected boolean isUseTestCaseLooseBinding() { // you can override
        return false;
    }

    // -----------------------------------------------------
    //                                         Register Mock
    //                                         -------------
    /**
     * Register the mock instance for injection. <br>
     * You can use new-created instance as DI component like this:
     * <pre>
     * FooAction <span style="color: #553000">action</span> = <span style="color: #70226C">new</span> FooAction();
     * <span style="color: #FD4747">registerMock</span>(<span style="color: #70226C">new</span> MockFooLogic());
     * inject(<span style="color: #553000">action</span>); <span style="color: #3F7E5E">// the new-created mock logic is injected</span>
     * </pre>
     * You can inject DI components for mock instance like this:
     * <pre>
     * FooAction <span style="color: #553000">action</span> = <span style="color: #70226C">new</span> FooAction();
     * registerMock(<span style="color: #FD4747">inject</span>(<span style="color: #70226C">new</span> MockFooLogic()));
     * inject(<span style="color: #553000">action</span>); <span style="color: #3F7E5E">// the new-created mock logic is injected</span>
     * </pre>
     * The nest limit is 2 basically. But you can resolve it by mock relay.<br>
     * e.g. Action to Assist to Logic to Wizard
     * <pre>
     * <span style="color: #3F7E5E">// Good</span>
     * registerMock(<span style="color: #FD4747">inject</span>(<span style="color: #70226C">new</span> MockFooLogic()));
     * FooAction <span style="color: #553000">action</span> = <span style="color: #70226C">new</span> FooAction();
     * inject(<span style="color: #553000">action</span>); <span style="color: #3F7E5E">// refers real assist refers mock logic</span>
     * 
     * <span style="color: #3F7E5E">// Bad (but...)</span>
     * registerMock(<span style="color: #FD4747">inject</span>(<span style="color: #70226C">new</span> MockFooWizard()));
     * FooAction <span style="color: #553000">action</span> = <span style="color: #70226C">new</span> FooAction();
     * inject(<span style="color: #553000">action</span>); <span style="color: #3F7E5E">// refers real assist refers logic referes real wizard</span>
     * 
     * <span style="color: #3F7E5E">// Good (using mock relay)</span>
     * registerMock(<span style="color: #FD4747">inject</span>(<span style="color: #70226C">new</span> MockFooWizard()));
     * registerMock(<span style="color: #FD4747">inject</span>(<span style="color: #70226C">new</span> MockFooLogic()));
     * FooAction <span style="color: #553000">action</span> = <span style="color: #70226C">new</span> FooAction();
     * inject(<span style="color: #553000">action</span>); <span style="color: #3F7E5E">// refers real assist mock logic referes mock wizard</span>
     * </pre>
     * @param mock The mock instance injected to component. (NotNull)
     */
    public void registerMock(Object mock) { // user method
        assertNotNull(mock);
        if (_xmockInstanceList == null) {
            _xmockInstanceList = new ArrayList<Object>();
        }
        final Object filtered;
        if (mock instanceof BoundResult) {
            filtered = ((BoundResult) mock).getTargetBean(); // for registerMock(inject(bean))
        } else {
            filtered = mock;
        }
        _xmockInstanceList.add(filtered);
    }

    /**
     * <span style="color: #FD4747; font-size: 120%">old method so use registerMock().</span>
     * @param mock The mock instance injected to component. (NotNull)
     */
    public void registerMockInstance(Object mock) { // user method
        assertNotNull(mock);
        registerMock(mock);
    }

    /**
     * <span style="color: #FD4747; font-size: 120%">old method so use registerMock() with inject().</span>
     * @param mock The mock instance injected to component. (NotNull)
     */
    public void registerMockInstanceInjecting(Object mock) { // user method
        assertNotNull(mock);
        inject(mock);
        registerMock(mock);
    }

    /**
     * Suppress the binding of the type for injection.
     * <pre>
     * FooAction <span style="color: #553000">action</span> = <span style="color: #70226C">new</span> FooAction();
     * <span style="color: #FD4747">suppressBindingOf</span>(FooBhv.<span style="color: #70226C">class</span>);
     * inject(<span style="color: #553000">action</span>); <span style="color: #3F7E5E">// not injected about the behavior type</span>
     * </pre>
     * @param nonBindingType The non-binding type NOT injected to component. (NotNull)
     */
    public void suppressBindingOf(Class<?> nonBindingType) { // user method
        assertNotNull(nonBindingType);
        if (_xnonBindingTypeList == null) {
            _xnonBindingTypeList = new ArrayList<Class<?>>();
        }
        _xnonBindingTypeList.add(nonBindingType);
    }

    protected BindingRuleProvider createBindingRuleProvider() {
        return new BindingRuleProvider() {
            public Map<Class<? extends Annotation>, BindingAnnotationRule> provideBindingAnnotationRuleMap() {
                return xprovideBindingAnnotationRuleMap();
            }

            public String filterByBindingNamingRule(String propertyName, Class<?> propertyType) {
                return xfilterByBindingNamingRule(propertyName, propertyType);
            }
        };
    }

    protected abstract Map<Class<? extends Annotation>, BindingAnnotationRule> xprovideBindingAnnotationRuleMap();

    protected String xfilterByBindingNamingRule(String propertyName, Class<?> propertyType) {
        return null; // as default: means no filter
    }

    // -----------------------------------------------------
    //                                                Inject
    //                                                ------
    /**
     * Inject dependencies for the bean. <br>
     * You can use DI component in self-new instance like this:
     * <pre>
     * FooAction <span style="color: #553000">action</span> = <span style="color: #70226C">new</span> FooAction();
     * <span style="color: #FD4747">inject</span>(<span style="color: #553000">action</span>);
     * 
     * <span style="color: #553000">action</span>.index(); <span style="color: #3F7E5E">// can use DI component</span>
     * </pre>
     * Also you can inject components to mock instance like this:
     * <pre>
     * registerMock(<span style="color: #FD4747">inject</span>(new MockFooLogic()));
     * </pre>
     * @param bean The instance of bean. (NotNull)
     * @return The information of bound result. (NotNull)
     */
    protected BoundResult inject(Object bean) { // user method
        final ComponentBinder binder = createOuterComponentBinder(bean);
        final BoundResult boundResult = xdoInject(bean, binder);
        if (_xinjectedBoundResultList == null) {
            _xinjectedBoundResultList = new ArrayList<BoundResult>(2);
        }
        _xinjectedBoundResultList.add(boundResult);
        return boundResult;
    }

    protected ComponentBinder createOuterComponentBinder(Object bean) { // you can override
        final ComponentBinder binder = xcreateBasicComponentBinder();
        xadjustOuterComponentBinder(bean, binder);
        return binder;
    }

    protected void xadjustOuterComponentBinder(Object bean, ComponentBinder binder) {
        // adjust mock components
        final List<Object> mockInstanceList = newArrayList();
        if (_xmockInstanceList != null) {
            mockInstanceList.addAll(_xmockInstanceList);
        }
        for (Object mockInstance : mockInstanceList) {
            if (mockInstance == bean) { // check instance so uses '=='
                continue; // suppress infinity loop just in case
            }
            binder.addMockInstance(mockInstance);
        }

        // adjust no binding components
        final List<Class<?>> nonBindingTypeList = newArrayList();
        if (_xnonBindingTypeList != null) {
            nonBindingTypeList.addAll(_xnonBindingTypeList);
        }
        for (Class<?> nonBindingType : nonBindingTypeList) {
            binder.addNonBindingType(nonBindingType);
        }
    }

    protected BoundResult xdoInject(Object bean, ComponentBinder binder) {
        return binder.bindComponent(bean);
    }

    // ===================================================================================
    //                                                                  Container Handling
    //                                                                  ==================
    protected abstract void xdestroyContainer();

    /**
     * Get component from DI container for the type.
     * @param <COMPONENT> The type of component.
     * @param type The type of component to find. (NotNull)
     * @return The instance of the component. (NotNull: if not found, throws exception)
     */
    protected abstract <COMPONENT> COMPONENT getComponent(Class<COMPONENT> type); // user method

    /**
     * Get component from DI container for the name.
     * @param <COMPONENT> The type of component.
     * @param name The name of component to find. (NotNull)
     * @return The instance of the component. (NotNull: if not found, throws exception)
     */
    protected abstract <COMPONENT> COMPONENT getComponent(String name); // user method

    /**
     * Does it have the component on the DI container for the type.
     * @param type The type of component to find. (NotNull)
     * @return The determination, true or false.
     */
    protected abstract boolean hasComponent(Class<?> type); // user method

    /**
     * Does it have the component on the DI container for the name.
     * @param name The name of component to find. (NotNull)
     * @return The determination, true or false.
     */
    protected abstract boolean hasComponent(String name); // user method

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    protected TransactionResource xgetTestCaseTransactionResource() {
        return _xtestCaseTransactionResource;
    }

    protected void xsetTestCaseTransactionResource(TransactionResource testCaseTransactionResource) {
        _xtestCaseTransactionResource = testCaseTransactionResource;
    }

    protected ComponentBinder xgetTestCaseComponentBinder() {
        return _xtestCaseComponentBinder;
    }

    protected BoundResult xgetTestCaseBoundResult() {
        return _xtestCaseBoundResult;
    }

    protected void xsetTestCaseBoundResult(BoundResult testCaseBoundResult) {
        _xtestCaseBoundResult = testCaseBoundResult;
    }

    protected List<BoundResult> xgetInjectedBoundResultList() {
        return _xinjectedBoundResultList;
    }

    protected void xsetInjectedBoundResultList(List<BoundResult> injectedBoundResultList) {
        _xinjectedBoundResultList = injectedBoundResultList;
    }

    protected List<Object> xgetMockInstanceList() {
        return _xmockInstanceList;
    }

    protected void xsetMockInstanceList(List<Object> mockInstanceList) {
        _xmockInstanceList = mockInstanceList;
    }

    protected List<Class<?>> xgetNonBindingTypeList() {
        return _xnonBindingTypeList;
    }

    protected void xsetNonBindingTypeList(List<Class<?>> nonBindingTypeList) {
        _xnonBindingTypeList = nonBindingTypeList;
    }
}
