package org.dbflute.utflute.lastaflute.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class FooBhv {

    private static final Logger logger = LoggerFactory.getLogger(FooBhv.class);
    
    public String callBhv() {
        logger.debug("callBhv() here");
        return "maihama";
    }
}
