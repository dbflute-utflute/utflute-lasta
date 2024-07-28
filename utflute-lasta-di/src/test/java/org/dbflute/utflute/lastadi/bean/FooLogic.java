package org.dbflute.utflute.lastadi.bean;

import org.dbflute.utflute.lastadi.dbflute.exbhv.FooBhv;

import jakarta.annotation.Resource;
import jakarta.transaction.TransactionManager;

/**
 * @author jflute
 * @since 0.1.0 (2011/07/24 Sunday)
 */
public class FooLogic {

    @Resource
    private FooBhv fooBhv; // private field

    @Resource(name = "fooService")
    protected FooService fooHelper; // name wrong but component name is specified

    protected FooService fooService; // no annotation at both field and setter

    public String behaviorToString() {
        return fooBhv != null ? fooBhv.toString() : null;
    }

    public FooService getFooService() {
        return fooService;
    }

    public void setFooService(FooService fooService) {
        this.fooService = fooService;
    }

    public TransactionManager getTransactionManager() {
        return fooBhv != null ? fooBhv.getTransactionManager() : null;
    }
}
