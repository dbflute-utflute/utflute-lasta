package org.dbflute.utflute.lastaflute.bean;

import javax.annotation.Resource;

/**
 * @author jflute
 */
public class FooAssist {

    @Resource
    private FooLogic fooLogic;

    public String callAssist() {
        return fooLogic.callLogic();
    }
}
