package org.dbflute.utflute.lastadi.bean;

import jakarta.annotation.Resource;

import org.dbflute.utflute.lastadi.dbflute.exbhv.FooBhv;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public abstract class FooBaseFacade {

    @Resource
    private FooBhv fooBhv; // super's private field

    public FooBhv superBehaviorInstance() {
        return fooBhv;
    }
}
