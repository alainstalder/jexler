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

import ch.grengine.jexler.JexlerUtil
import ch.grengine.jexler.test.SlowTests

import org.junit.experimental.categories.Category
import spock.lang.Specification

import static ServiceState.BUSY_STARTING
import static ServiceState.IDLE
import static ServiceState.OFF

/**
 * Tests the respective class.
 *
 * @author Alain Stalder
 */
@Category(SlowTests.class)
class ServiceUtilSlowSpec extends Specification {

    private final static long MS_1_SEC = 1000
    private final static long MS_2_SEC = 2000
    private final static long MS_3_SEC = 3000

    static class InterruptingThread extends Thread {
        Thread threadToInterrupt
        long interruptAfterMs
        boolean hasInterrupted
        InterruptingThread(final Thread threadToInterrupt, final long interruptAfterMs) {
            this.threadToInterrupt = threadToInterrupt
            this.interruptAfterMs = interruptAfterMs
        }
        @Override
        void run() {
            JexlerUtil.waitAtLeast(interruptAfterMs)
            threadToInterrupt.interrupt()
            hasInterrupted = true
        }
    }

    static class ServiceStateSettingThread extends Thread {
        Service service
        ServiceState stateToSet
        long setAfterMs
        ServiceStateSettingThread(final Service service, final ServiceState stateToSet, final long setAfterMs) {
            this.service = service
            this.stateToSet = stateToSet
            this.setAfterMs = setAfterMs
        }
        @Override
        void run() {
            JexlerUtil.waitAtLeast(setAfterMs)
            service.state = stateToSet
        }
    }

    def 'TEST SLOW (3 sec) wait for startup'() {
        given:
        final def service = new MockService(null, 'mock')

        when:
        service.state = BUSY_STARTING

        then:
        !ServiceUtil.waitForStartup(service, 0)

        when:
        final def interruptingThread = new InterruptingThread(Thread.currentThread(), MS_1_SEC)
        interruptingThread.start()
        new ServiceStateSettingThread(service, IDLE, MS_2_SEC).start()

        then:
        ServiceUtil.waitForStartup(service, MS_3_SEC)
        interruptingThread.hasInterrupted
        service.state == IDLE
    }

    def 'TEST SLOW (3 sec) wait for shutdown'() {
        given:
        final def service = new MockService(null, 'mock')

        when:
        service.state = IDLE

        then:
        !ServiceUtil.waitForShutdown(service, 0)

        when:
        final def interruptingThread = new InterruptingThread(Thread.currentThread(), MS_1_SEC)
        interruptingThread.start()
        new ServiceStateSettingThread(service, OFF, MS_2_SEC).start()

        then:
        ServiceUtil.waitForShutdown(service, MS_3_SEC)
        interruptingThread.hasInterrupted
        service.state == OFF
    }

}
