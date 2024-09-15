/*
   Copyright 2013-now by Alain Stalder. Made in Switzerland.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package ch.grengine.jexler.service

import ch.grengine.jexler.Jexler

import groovy.transform.CompileStatic

import static ServiceState.IDLE
import static ServiceState.OFF

/**
 * Mock service implementation for unit tests.
 *
 * @author Alain Stalder
 */
@CompileStatic
class MockService extends ServiceBase {

    private static final Map<String,MockService> INSTANCES = [:]

    private Jexler jexler
    volatile int nStarted = 0
    volatile int nStopped = 0
    volatile int nZapped = 0
    volatile int nEventsSent = 0
    volatile int nEventsGotBack = 0
    volatile RuntimeException stopRuntimeException = null

    static MockService getInstance(final String id) {
        synchronized(INSTANCES) {
            return INSTANCES[id]
        }
    }

    MockService(final Jexler jexler, final String id) {
        super(id)
        this.jexler = jexler
        synchronized(INSTANCES) {
            INSTANCES[id] = this
        }
    }

    @Override
    void start() {
        nStarted++
        state = IDLE
    }

    @Override
    void stop() {
        nStopped++
        if (stopRuntimeException != null) {
            throw stopRuntimeException
        }
        state = OFF
    }

    @Override
    void zap() {
        nZapped++
        nStopped++
        state = OFF
    }

    void notifyGotEvent() {
        nEventsGotBack++
    }

    void notifyJexler() {
        jexler.handle(new MockEvent(this))
        nEventsSent++
    }

    void notifyJexler(final Event event) {
        jexler.handle(event)
        nEventsSent++
    }

}
