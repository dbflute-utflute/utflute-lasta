package org.dbflute.utflute.lasta.bean;

import javax.annotation.Resource;

/**
 * @author jflute
 * @since 0.5.1 (2015/03/22 Sunday)
 */
public class FooControllerImpl implements FooController {

    @Resource
    protected FooFacade fooFacade;

    public FooFacade facadeInstance() {
        return fooFacade;
    }
}
