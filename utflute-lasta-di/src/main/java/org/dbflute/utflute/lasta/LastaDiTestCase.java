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
package org.dbflute.utflute.lasta;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.annotation.Resource;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.dbflute.lasta.di.core.SingletonLaContainer;
import org.dbflute.lasta.di.core.annotation.Binding;
import org.dbflute.lasta.di.core.annotation.BindingType;
import org.dbflute.lasta.di.core.exception.ComponentNotFoundException;
import org.dbflute.lasta.di.core.exception.ComponentNotFoundRuntimeException;
import org.dbflute.lasta.di.core.exception.TooManyRegistrationRuntimeException;
import org.dbflute.lasta.di.core.factory.SingletonLaContainerFactory;
import org.dbflute.lasta.di.core.smart.Env;
import org.dbflute.lasta.di.naming.NamingConvention;
import org.dbflute.utflute.core.InjectionTestCase;
import org.dbflute.utflute.core.binding.BindingAnnotationRule;
import org.dbflute.utflute.core.binding.ComponentBinder;
import org.dbflute.utflute.core.binding.NonBindingDeterminer;
import org.dbflute.utflute.core.transaction.TransactionFailureException;
import org.dbflute.utflute.core.transaction.TransactionResource;
import org.dbflute.util.Srl;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public abstract class LastaDiTestCase extends InjectionTestCase {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                         Static Cached
    //                                         -------------
    /** The cached configuration file of DI container. (NullAllowed: null means beginning or ending) */
    protected static String _xcachedConfigFile;

    /** The cached determination of suppressing web mock. (NullAllowed: null means beginning or ending) */
    protected static Boolean _xcachedSuppressWebMock;

    // ===================================================================================
    //                                                                            Settings
    //                                                                            ========
    // -----------------------------------------------------
    //                                      Before Container
    //                                      ----------------
    @Override
    protected void xsetupBeforeContainer() {
        super.xsetupBeforeContainer();
        xprepareUnitTestEnv();
    }

    // -----------------------------------------------------
    //                                     Prepare Container
    //                                     -----------------
    @Override
    protected void xprepareTestCaseContainer() {
        final String configFile = xdoPrepareTestCaseContainer();
        xsaveCachedInstance(configFile);
    }

    protected String xdoPrepareTestCaseContainer() {
        if (isUseOneTimeContainer()) {
            xdestroyContainer();
        }
        final String configFile = prepareConfigFile();
        if (xisInitializedContainer()) {
            if (xcanRecycleContainer(configFile)) {
                log("...Recycling seasar: " + configFile);
                xrecycleContainerInstance(configFile);
                return configFile; // no need to initialize
            } else { // changed
                xdestroyContainer(); // to re-initialize
            }
        }
        xinitializeContainer(configFile);
        return configFile;
    }

    protected boolean xcanRecycleContainer(String configFile) {
        return xconfigCanAcceptContainerRecycle(configFile) && xwebMockCanAcceptContainerRecycle();
    }

    protected boolean xconfigCanAcceptContainerRecycle(String configFile) {
        return configFile.equals(_xcachedConfigFile); // no change
    }

    protected void xrecycleContainerInstance(String configFile) {
        // managed as singleton so caching is unneeded here
    }

    protected boolean xwebMockCanAcceptContainerRecycle() {
        // no mark or no change
        return _xcachedSuppressWebMock == null || _xcachedSuppressWebMock.equals(isSuppressWebMock());
    }

    protected void xsaveCachedInstance(String configFile) {
        _xcachedConfigFile = configFile;
        _xcachedSuppressWebMock = isSuppressWebMock();
    }

    /**
     * Does it suppress web mock? e.g. HttpServletRequest, HttpSession
     * @return The determination, true or false.
     */
    protected boolean isSuppressWebMock() {
        return false;
    }

    @Override
    protected boolean isUseOneTimeContainer() {
        return false;
    }

    /**
     * Prepare configuration file as root for Lasta Di.
     * @return The pure file name of root xml. (NotNull)
     */
    protected String prepareConfigFile() { // customize point
        return "app.xml"; // as default
    }

    @Override
    protected void xclearCachedContainer() {
        _xcachedConfigFile = null;
    }

    // ===================================================================================
    //                                                                         Transaction
    //                                                                         ===========
    @Override
    protected TransactionResource beginNewTransaction() { // user method
        final Class<TransactionManager> managerType = TransactionManager.class;
        if (!hasComponent(managerType)) {
            return null;
        }
        final TransactionManager manager = getComponent(managerType);
        final Transaction suspendedTx;
        try {
            if (manager.getStatus() != Status.STATUS_NO_TRANSACTION) {
                suspendedTx = manager.suspend(); // because Seasar's DBCP doesn't support nested transaction
            } else {
                suspendedTx = null;
            }
        } catch (SystemException e) {
            throw new TransactionFailureException("Failed to suspend current", e);
        }
        TransactionResource resource = null;
        try {
            manager.begin();
            resource = new TransactionResource() {
                public void commit() {
                    try {
                        manager.commit();
                    } catch (Exception e) {
                        throw new TransactionFailureException("Failed to commit the transaction.", e);
                    } finally {
                        xresumeSuspendedTxQuietly(manager, suspendedTx);
                    }
                }

                public void rollback() {
                    try {
                        manager.rollback();
                    } catch (Exception e) {
                        throw new TransactionFailureException("Failed to roll-back the transaction.", e);
                    } finally {
                        xresumeSuspendedTxQuietly(manager, suspendedTx);
                    }
                }
            }; // for thread-fire's transaction or manual transaction
        } catch (NotSupportedException e) {
            throw new TransactionFailureException("Failed to begin new transaction.", e);
        } catch (SystemException e) {
            throw new TransactionFailureException("Failed to begin new transaction.", e);
        }
        return resource;
    }

    protected void xresumeSuspendedTxQuietly(TransactionManager manager, Transaction suspendedTx) {
        try {
            if (suspendedTx != null) {
                manager.resume(suspendedTx);
            }
        } catch (Exception e) {
            log(e.getMessage());
        }
    }

    // ===================================================================================
    //                                                                   Component Binding
    //                                                                   =================
    @Override
    protected ComponentBinder createOuterComponentBinder(Object bean) {
        final ComponentBinder binder = super.createOuterComponentBinder(bean);
        binder.byTypeInterfaceOnly();
        return binder;
    }

    @Override
    protected Map<Class<? extends Annotation>, BindingAnnotationRule> xprovideBindingAnnotationRuleMap() {
        final Map<Class<? extends Annotation>, BindingAnnotationRule> ruleMap = newHashMap();
        ruleMap.put(Resource.class, new BindingAnnotationRule());
        ruleMap.put(Binding.class, new BindingAnnotationRule().determineNonBinding(new NonBindingDeterminer() {
            public boolean isNonBinding(Annotation bindingAnno) {
                return BindingType.NONE.equals(((Binding) bindingAnno).bindingType());
            }
        }));
        return ruleMap;
    }

    @Override
    protected String xfilterByBindingNamingRule(String propertyName, Class<?> propertyType) {
        if (propertyType.getSimpleName().contains("_")) { // e.g. (org.dbflute.maihama.) Foo_BarLogic
            return null; // simple name that contains '_' is unsupported
        }
        // e.g. [root].logic.foo.bar.QuxLogic
        final NamingConvention convention = getComponent(NamingConvention.class);
        final String componentName;
        try {
            // e.g. foo_bar_quxLogic -> foo_bar_quxLogic ends with [property name] -> returns foo_bar_quxLogic
            componentName = convention.fromClassNameToComponentName(propertyType.getName());
        } catch (RuntimeException ignored) { // just in case e.g. org.dbflute.maihama.foo
            return null;
        }
        if (xcanUseComponentNameByBindingNamingRule(componentName, propertyName)) {
            return componentName;
        }
        // not smart deploy component or name wrong e.g. (foo_bar_) quxLogic does not equal quxService
        return null;
    }

    protected boolean xcanUseComponentNameByBindingNamingRule(String componentName, String propertyName) {
        if (componentName.contains("_")) { // means smart deploy component
            if (componentName.endsWith(propertyName)) {
                final String front = Srl.substringLastFront(componentName, propertyName); // e.g. foo_bar_
                if (front.equals("") || front.endsWith("_")) {
                    // e.g.
                    //  foo_bar_quxLogic ends with foo_bar_quxLogic
                    //  foo_bar_quxLogic ends with quxLogic
                    //  foo_bar_quxLogic ends with bar_quxLogic
                    return true;
                }
                // e.g. foo_bar_quxLogic ends with ar_quxLogic
            }
        }
        return false;
    }

    // ===================================================================================
    //                                                                     Seasar Handling
    //                                                                     ===============
    protected void xprepareUnitTestEnv() {
        Env.setFilePath("env_ut.txt");
        Env.setValueIfAbsent("ut");
    }

    // -----------------------------------------------------
    //                                            Initialize
    //                                            ----------
    protected boolean xisInitializedContainer() {
        return SingletonLaContainerFactory.hasContainer();
    }

    protected void xinitializeContainer(String configFile) {
        log("...Initializing seasar as library: " + configFile);
        xdoInitializeContainerAsLibrary(configFile);
    }

    protected void xdoInitializeContainerAsLibrary(String configFile) {
        SingletonLaContainerFactory.setConfigPath(configFile);
        SingletonLaContainerFactory.init();
    }

    // -----------------------------------------------------
    //                                               Destroy
    //                                               -------
    protected void xdestroyContainer() {
        SingletonLaContainerFactory.destroy();
        SingletonLaContainerFactory.setExternalContext(null); // destroy() does not contain this
    }

    // -----------------------------------------------------
    //                                             Component
    //                                             ---------
    /** {@inheritDoc} */
    protected <COMPONENT> COMPONENT getComponent(Class<COMPONENT> type) { // user method
        return SingletonLaContainer.getComponent(type);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    protected <COMPONENT> COMPONENT getComponent(String name) { // user method
        return (COMPONENT) SingletonLaContainer.getComponent(name);
    }

    /** {@inheritDoc} */
    protected boolean hasComponent(Class<?> type) { // user method
        try {
            SingletonLaContainer.getComponent(type);
            return true;
        } catch (ComponentNotFoundException | ComponentNotFoundRuntimeException e) {
            return false;
        } catch (TooManyRegistrationRuntimeException e) {
            return false;
        }
    }

    /** {@inheritDoc} */
    protected boolean hasComponent(String name) { // user method
        try {
            SingletonLaContainer.getComponent(name);
            return true;
        } catch (ComponentNotFoundException | ComponentNotFoundRuntimeException ignored) {
            return false;
        }
    }
}
