package org.dbflute.utflute.lasta.dbflute.exbhv;

import javax.transaction.TransactionManager;

/**
 * @author jflute
 * @since 0.1.0 (2011/07/24 Sunday)
 */
public class FooBhv {

    protected TransactionManager transactionManager;

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
