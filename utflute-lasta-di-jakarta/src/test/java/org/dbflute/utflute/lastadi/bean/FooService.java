package org.dbflute.utflute.lastadi.bean;

import jakarta.annotation.Resource;
import jakarta.transaction.TransactionManager;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public class FooService {

    @Resource
    protected TransactionManager transactionManager;
}
