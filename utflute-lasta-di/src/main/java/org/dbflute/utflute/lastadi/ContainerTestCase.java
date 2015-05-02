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
package org.dbflute.utflute.lastadi;

import java.lang.reflect.Field;

import javax.sql.DataSource;

import org.dbflute.utflute.lastadi.injection.SmartDeployDependencyChecker;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public abstract class ContainerTestCase extends LastaDiTestCase {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The (main) data source for database. (NotNull: after injection) */
    protected DataSource _xdataSource;

    // ===================================================================================
    //                                                                         JDBC Helper
    //                                                                         ===========
    /** {@inheritDoc} */
    @Override
    protected DataSource getDataSource() { // user method
        return _xdataSource;
    }

    // ===================================================================================
    //                                                                  Dependency Checker
    //                                                                  ==================
    protected void checkDependencyToLogic() {
        doCheckDependencyTo("Logic", "Logic");
    }

    protected void checkDependencyToService() {
        doCheckDependencyTo("Service", "Service");
    }

    protected void checkDependencyToHelper() {
        doCheckDependencyTo("Helper", "Helper");
    }

    protected void checkDependencyToBehavior() {
        doCheckDependencyTo("Behavior", "Bhv");
    }

    protected void doCheckDependencyTo(String title, String suffix) {
        policeStoryOfJavaClassChase(createSmartDeployDependencyChecker(title, suffix));
    }

    protected SmartDeployDependencyChecker createSmartDeployDependencyChecker(String title, String suffix) {
        return new EmbeddedSmartDeployDependencyChecker(title, suffix);
    }

    protected class EmbeddedSmartDeployDependencyChecker extends SmartDeployDependencyChecker {

        public EmbeddedSmartDeployDependencyChecker(String title, String suffix) {
            super(title, suffix);
        }

        @Override
        protected void processTargetClass(Class<?> clazz, Field field, Class<?> injectedType) {
            final String injectedClassName = extractInjectedClassName(injectedType);
            log(clazz.getSimpleName() + "." + field.getName() + " depends on " + injectedClassName);
        }
    }
}
