package org.dbflute.utflute.lastaflute.bean;

import javax.annotation.Resource;

/**
 * @author jflute
 */
public class FooLogic {

    @Resource
    private FooBhv fooBhv;

    public String callLogic() {
        return fooBhv.callBhv();
    }
}
