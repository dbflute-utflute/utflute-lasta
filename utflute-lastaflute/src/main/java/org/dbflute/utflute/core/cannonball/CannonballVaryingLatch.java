/*
 * Copyright 2014-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.dbflute.utflute.core.cannonball;

import java.util.concurrent.CountDownLatch;

/**
 * @author jflute
 * @since 0.4.2 (2014/03/30 Sunday)
 */
public class CannonballVaryingLatch {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final int _initialCount;
    protected final CountDownLatch _latch;
    protected int _bufferCount;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public CannonballVaryingLatch(int initialCount) {
        _initialCount = initialCount;
        _latch = new CountDownLatch(initialCount);
    }

    // ===================================================================================
    //                                                                      CountDownLatch
    //                                                                      ==============
    public void await() {
        try {
            _latch.await();
        } catch (InterruptedException e) {
            String msg = "Failed to await by your latch: latch=" + _latch;
            throw new CannonballRetireException(msg, e);
        }
    }

    public void countDown() {
        if (countDownBufferCount()) {
            return;
        }
        _latch.countDown();
    }

    public synchronized long getCount() {
        return _latch.getCount() + _bufferCount;
    }

    // ===================================================================================
    //                                                                        Buffer Count
    //                                                                        ============
    public synchronized void incrementBufferCount() {
        ++_bufferCount;
    }

    protected synchronized boolean countDownBufferCount() {
        if (_bufferCount > 0) {
            --_bufferCount;
            return true;
        }
        return false;
    }

    // ===================================================================================
    //                                                                         Easy-to-Use
    //                                                                         ===========
    public boolean isReleasedLatch() {
        return getCount() == 0;
    }

    public boolean isLastCountLatch() {
        return getCount() == 1;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public int getInitialCount() {
        return _initialCount;
    }
}
