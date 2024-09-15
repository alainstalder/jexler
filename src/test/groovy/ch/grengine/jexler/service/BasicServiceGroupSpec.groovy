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

import ch.grengine.jexler.TestJexler
import ch.grengine.jexler.test.FastTests
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static ServiceState.BUSY_EVENT
import static ServiceState.BUSY_STARTING
import static ServiceState.BUSY_STOPPING
import static ServiceState.IDLE
import static ServiceState.OFF

/**
 * Tests the respective class.
 *
 * @author Alain Stalder
 */
@Category(FastTests.class)
class BasicServiceGroupSpec extends Specification {

    def 'TEST basics including group service state'() {
        given:
        final def jexler = new TestJexler()
        final def service1 = new MockService(jexler, 'service1')
        final def service2 = new MockService(jexler, 'service2')
        final def service3 = new MockService(jexler, 'service3')

        when:
        final def group = new ServiceGroup('group')

        then:
        group.id == 'group'
        group.services.empty

        when:
        group.add(service1)
        group.add(service2)
        group.add(service3)

        then:
        group.services.size() == 3
        group.state == OFF

        when:
        service2.state = BUSY_STARTING

        then:
        group.state == BUSY_STARTING

        when:
        service1.state = IDLE
        service3.state = BUSY_EVENT

        then:
        group.state == BUSY_STARTING

        when:
        service2.state = IDLE

        then:
        group.state == BUSY_EVENT

        when:
        service1.state = BUSY_STOPPING
        service2.state = BUSY_STOPPING
        service3.state = BUSY_STOPPING

        then:
        group.state == BUSY_STOPPING
    }

    def 'TEST start and stop'() {
        given:
        final def jexler = new TestJexler()
        final def service1 = new MockService(jexler, 'service1')
        final def service2 = new MockService(jexler, 'service2')
        final def service3 = new MockService(jexler, 'service3')

        when:
        final def group = new ServiceGroup('group')
        group.add(service1)
        group.add(service2)
        group.add(service3)

        then:
        service1.state == OFF
        service2.state == OFF
        service3.state == OFF
        group.state == OFF
        !group.state.on
        group.state.off

        when:
        service1.start()

        then:
        service1.state == IDLE
        service2.state == OFF
        service3.state == OFF
        group.state == IDLE
        group.state.on
        !group.state.off

        when:
        group.start()

        then:
        service1.state == IDLE
        service2.state == IDLE
        service3.state == IDLE
        group.state == IDLE
        group.state.on
        !group.state.off

        when:
        service3.stop()

        then:
        service1.state == IDLE
        service2.state == IDLE
        service3.state == OFF
        group.state == IDLE
        group.state.on
        !group.state.off

        when:
        group.stop()

        then:
        service1.state == OFF
        service2.state == OFF
        service3.state == OFF
        group.state == OFF
        !group.state.on
        group.state.off
    }

    def 'TEST runtime exceptions when stopping services'() {
        given:
        final def jexler = new TestJexler()
        final def service1 = new MockService(jexler, 'service1')
        final def service2 = new MockService(jexler, 'service2')
        final def service3 = new MockService(jexler, 'service3')
        final def group = new ServiceGroup('group')
        group.add(service1)
        group.add(service2)
        group.add(service3)

        when:
        group.start()

        then:
        service1.state == IDLE
        service2.state == IDLE
        service3.state == IDLE
        group.state == IDLE

        when:
        RuntimeException ex1 = new RuntimeException()
        RuntimeException ex2 = new RuntimeException()
        service1.stopRuntimeException = ex1
        service2.stopRuntimeException = ex2
        group.stop()

        then:
        final RuntimeException e = thrown()
        e.is(ex1)
        service1.state == IDLE
        service2.state == IDLE
        service3.state == OFF
        group.state == IDLE
    }

    def 'TEST empty service group'() {
        when:
        final def group = new ServiceGroup('group')

        then:
        group.state == OFF

        when:
        group.start()

        then:
        group.state == OFF

        when:
        group.stop()

        then:
        group.state == OFF
    }

}
