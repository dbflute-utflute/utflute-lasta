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
package org.dbflute.utflute.lastadi;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * @author jflute
 * @since 0.5.0-sp7 (2015/03/22 Sunday)
 */
public abstract class ContainerTestCase extends LastaDiTestCase {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The (main) data source for database. (NotNull: after injection) */
    @Resource
    private DataSource _xdataSource;

    // ===================================================================================
    //                                                                         JDBC Helper
    //                                                                         ===========
    /** {@inheritDoc} */
    @Override
    protected DataSource getDataSource() { // user method
        return _xdataSource;
    }
}
