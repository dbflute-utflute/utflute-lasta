package org.dbflute.utflute.lasta.bean;

import javax.transaction.TransactionManager;

import org.dbflute.lasta.di.core.annotation.Binding;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public class FooService {

    @Binding
    protected TransactionManager transactionManager;
}
