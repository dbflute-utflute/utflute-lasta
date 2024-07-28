package org.dbflute.utflute.lastadi.bean;

import org.dbflute.utflute.lastadi.dbflute.exbhv.FooBhv;

import jakarta.annotation.Resource;
import jakarta.transaction.TransactionManager;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public class FooFacade extends FooBaseFacade {

    @Resource
    private FooBhv fooBhv; // same name as super's

    protected TransactionManager transactionManager; // no annotation, no setter

    protected FooService fooService; // annotation for protected setter

    public FooBhv myBehaviorInstance() {
        return fooBhv;
    }

    public FooService getFooService() {
        return fooService;
    }

    @Resource
    protected void setFooService(FooService fooService) {
        this.fooService = fooService;
    }
}
