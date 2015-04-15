package org.dbflute.utflute.lasta.bean;

import javax.annotation.Resource;
import javax.transaction.TransactionManager;

import org.dbflute.lasta.di.core.annotation.Binding;
import org.dbflute.lasta.di.core.annotation.BindingType;
import org.dbflute.utflute.lasta.dbflute.exbhv.FooBhv;

/**
 * @author jflute
 * @since 0.1.0 (2011/07/24 Sunday)
 */
public class FooLogic {

    @Resource
    private FooBhv fooBhv; // private field

    @Resource(name = "fooService")
    protected FooService fooHelper; // name wrong but component name is specified

    protected FooService fooService; // annotation for setter but none

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
