/*
 * Copyright 2014-2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

/**
 * @author jflute
 * @since 0.3.8 (2014/03/03 Monday)
 */
public class CannonballDragon {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final CannonballCar _car; // for the project A
    protected final CannonballWatchingStatus _watchingStatus; // should be used in synchronized scope
    protected final List<CannonballWatcher> _watcherList = new ArrayList<CannonballWatcher>();
    protected final long _planBeginTime; // to calculate time-span
    protected boolean _expectedNormallyDone;
    protected boolean _expectedOvertime;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public CannonballDragon(CannonballCar car, CannonballWatchingStatus watchingStatus) {
        _car = car;
        _watchingStatus = watchingStatus;
        _planBeginTime = System.currentTimeMillis(); // almost same as created time
    }

    // ===================================================================================
    //                                                                              Expect
    //                                                                              ======
    /**
     * Expect the plan is normally done (no overtime).
     * @return this. (NotNull)
     */
    public CannonballDragon expectNormallyDone() {
        _expectedNormallyDone = true;
        return this;
    }

    /**
     * Expect the plan is overtime (forcedly count down).
     * @return this. (NotNull)
     */
    public CannonballDragon expectOvertime() {
        _expectedOvertime = true;
        return this;
    }

    // ===================================================================================
    //                                                                            Overtime
    //                                                                            ========
    /**
     * Release waiting cars if overtime. <br>
     * The default allowed time for the plan is 3000 milliseconds. <br>
     * you can reset it by this method.
     * @param millis The milliseconds until overtime. (NotMinus)
     */
    public void releaseIfOvertime(final long millis) {
        // create and start new watching thread and close old threads
        synchronized (_watchingStatus) {
            if (isWaiting()) {
                for (CannonballWatcher watcher : _watcherList) {
                    watcher.close();
                }
                final CannonballWatcher watcher = createWatcher(millis);
                _watcherList.add(watcher);
                watcher.watch();
            }
        }
    }

    // ===================================================================================
    //                                                                             Watcher
    //                                                                             =======
    protected CannonballWatcher createWatcher(final long millis) {
        return new CannonballWatcher(millis);
    }

    protected class CannonballWatcher {

        protected final long _millis;
        protected volatile boolean _closed;

        public CannonballWatcher(long millis) {
            _millis = millis;
        }

        public void watch() {
            final Thread watchingThread = new Thread(new Runnable() {
                public void run() {
                    final long alreadyPastTime = System.currentTimeMillis() - _planBeginTime;
                    final int splitCount = 10;
                    final long tokenMillis = (_millis - alreadyPastTime) / splitCount;
                    if (tokenMillis > 0) { // just in case
                        for (int i = 0; i < splitCount; i++) {
                            sleep(tokenMillis);
                            if (_closed) {
                                return;
                            }
                        }
                    }
                    synchronized (_watchingStatus) {
                        if (_closed) {
                            return;
                        }
                        if (isWaiting()) {
                            final int entryNumber = _car.getEntryNumber();
                            final CannonballLatch ourLatch = _car.getOurLatch();
                            ourLatch.leaveProjectAAlone(_watchingStatus.getProjectAKey(), entryNumber); // release waiting cars
                            _watchingStatus.markForecdly(); // to suppress unnecessary restart of forcedly car
                        }
                    }
                }

                protected void sleep(long millis) {
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException e) {
                        String msg = "Failed to have a tea break but I want to...";
                        throw new IllegalStateException(msg, e);
                    }
                }
            });
            watchingThread.start();
        }

        public void close() {
            _closed = true;
        }
    }

    protected boolean isWaiting() { // should be used in synchronized scope
        return _watchingStatus.isWaiting();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * Get the entry number for the dragon.
     * @return The assigned number. e.g. 1, 2, 3... (NotNull)
     */
    public int getEntryNumber() {
        return _car.getEntryNumber();
    }

    public boolean isExpectedNormallyDone() {
        return _expectedNormallyDone;
    }

    public boolean isExpectedOvertime() {
        return _expectedOvertime;
    }
}
