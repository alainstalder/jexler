/*
   Copyright 2012-now  Jex Jexler (Alain Stalder)

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

package ch.artecat.jexler

import ch.artecat.jexler.service.CronEvent
import ch.artecat.jexler.service.DirWatchEvent
import ch.artecat.jexler.service.MockService
import ch.artecat.jexler.test.FastTests

import org.junit.Rule
import org.junit.experimental.categories.Category
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.StandardWatchEventKinds

import static ch.artecat.jexler.service.ServiceState.IDLE
import static ch.artecat.jexler.service.ServiceState.OFF

/**
 * Tests the respective class.
 *
 * @author Jex Jexler (Alain Stalder)
 */
@Category(FastTests.class)
class JexlerDispatcherSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    private final static long MS_1_SEC = 1000
    private final static long MS_10_SEC = 10000

    static class TestState {
        boolean declareCalled
        boolean startCalled
        boolean handleByClassNameAndServiceIdCalled
        boolean handleByClassNameCalled
        boolean handleCalled
        boolean stopCalled
    }

    static TestState testState

    def setup() {
        testState = new TestState()
    }
    
    def 'TEST minimal methods and no suitable event handler'() {
        given:
        final def dir = tempFolder.root
        final def file = new File(dir, 'Test.groovy')
        file.text = """\
            // Jexler {}
            mockService = new MockService(jexler, 'MockService')
            JexlerDispatcher.dispatch(this)
            void start() {
              services.add(mockService)
              services.start()
            }
            """.stripIndent()
        when:
        final def jexler = new Jexler(file, new JexlerContainer(dir))
        jexler.start()
        JexlerUtil.waitForStartup(jexler, MS_10_SEC)

        then:
        jexler.state == IDLE
        jexler.issues.empty

        when:
        final def mockService = MockService.getInstance('MockService')

        then:
        mockService.nStarted == 1
        mockService.nEventsSent == 0
        mockService.nEventsGotBack == 0
        mockService.nStopped == 0

        when:
        mockService.notifyJexler()
        JexlerUtil.waitAtLeast(MS_1_SEC)

        then:
        jexler.issues.size() == 1
        jexler.issues.first().service == jexler
        jexler.issues.first().message == 'Dispatch: No handler for event MockEvent from service MockService.'
        jexler.issues.first().cause == null
        mockService.nStarted == 1
        mockService.nEventsSent == 1
        mockService.nEventsGotBack == 0
        mockService.nStopped == 0

        when:
        jexler.forgetIssues()

        then:
        jexler.issues.empty

        when:
        jexler.stop()
        JexlerUtil.waitForShutdown(jexler, MS_10_SEC)

        then:
        jexler.state == OFF
        jexler.issues.empty
        mockService.nStarted == 1
        mockService.nEventsSent == 1
        mockService.nEventsGotBack == 0
        mockService.nStopped == 1
    }

    def 'TEST mandatory start method missing'() {
        given:
        final def dir = tempFolder.root
        final def file = new File(dir, 'Test.groovy')
        file.text = """\
            // Jexler {}
            mockService = new MockService(jexler, 'MockService')
            JexlerDispatcher.dispatch(this)
            void noStartMethod() {
              services.add(mockService)
              services.start()
            }
            """.stripIndent()
        when:
        final def jexler = new Jexler(file, new JexlerContainer(dir))
        jexler.start()
        JexlerUtil.waitForStartup(jexler, MS_10_SEC)

        then:
        jexler.state == OFF
        jexler.issues.size() == 1
        jexler.issues.first().service == jexler
        jexler.issues.first().message == 'Dispatch: Mandatory start() method missing.'
        jexler.issues.first().cause == null
    }

    def 'TEST all methods and handlers'() {
        given:
        final def dir = tempFolder.root
        final def file = new File(dir, 'Test.groovy')
        file.text = """\
            // Jexler {}
            JexlerDispatcher.dispatch(this)
            void declare() {
              mockService = new MockService(jexler, 'MockService')
              testState = JexlerDispatcherSpec.testState
              testState.declareCalled = true
            }
            void start() {
              services.add(mockService)
              services.start()
              testState.startCalled = true
            }
            void handleMockEventMockService(def event) {
              mockService.notifyGotEvent()
              testState.handleByClassNameAndServiceIdCalled = true
            }
            void handleCronEvent(def event) {
              mockService.notifyGotEvent()
              testState.handleByClassNameCalled = true
            }
            void handleDirWatchEvent(def event) {
              mockService.notifyGotEvent()
              testState.handleCalled = true
            }
            void stop() {
              testState.stopCalled = true
            }
            """.stripIndent()
        when:
        final def jexler = new Jexler(file, new JexlerContainer(dir))
        jexler.start()
        JexlerUtil.waitForStartup(jexler, MS_10_SEC)

        then:
        jexler.state == IDLE
        jexler.issues.empty

        when:
        final def mockService = MockService.getInstance('MockService')

        then:
        mockService.nStarted == 1
        mockService.nEventsSent == 0
        mockService.nEventsGotBack == 0
        mockService.nStopped == 0
        testState.declareCalled
        testState.startCalled
        !testState.handleByClassNameAndServiceIdCalled
        !testState.handleByClassNameCalled
        !testState.handleCalled
        !testState.stopCalled

        when:
        mockService.notifyJexler()
        JexlerUtil.waitAtLeast(MS_1_SEC)

        then:
        jexler.state == IDLE
        jexler.issues.empty
        mockService.nStarted == 1
        mockService.nEventsSent == 1
        mockService.nEventsGotBack == 1
        mockService.nStopped == 0
        testState.declareCalled
        testState.startCalled
        testState.handleByClassNameAndServiceIdCalled
        !testState.handleByClassNameCalled
        !testState.handleCalled
        !testState.stopCalled

        when:
        mockService.notifyJexler(new CronEvent(jexler, '* * * * *'))
        JexlerUtil.waitAtLeast(MS_1_SEC)

        then:
        jexler.state == IDLE
        jexler.issues.empty
        mockService.nStarted == 1
        mockService.nEventsSent == 2
        mockService.nEventsGotBack == 2
        mockService.nStopped == 0
        testState.declareCalled
        testState.startCalled
        testState.handleByClassNameAndServiceIdCalled
        testState.handleByClassNameCalled
        !testState.handleCalled
        !testState.stopCalled

        when:
        mockService.notifyJexler(new DirWatchEvent(jexler, new File('.'), StandardWatchEventKinds.ENTRY_CREATE))
        JexlerUtil.waitAtLeast(MS_1_SEC)

        then:
        jexler.state == IDLE
        jexler.issues.empty
        mockService.nStarted == 1
        mockService.nEventsSent == 3
        mockService.nEventsGotBack == 3
        mockService.nStopped == 0
        testState.declareCalled
        testState.startCalled
        testState.handleByClassNameAndServiceIdCalled
        testState.handleByClassNameCalled
        testState.handleCalled
        !testState.stopCalled

        when:
        jexler.stop()
        JexlerUtil.waitForShutdown(jexler, MS_10_SEC)

        then:
        jexler.state == OFF
        jexler.issues.empty
        mockService.nStarted == 1
        mockService.nEventsSent == 3
        mockService.nEventsGotBack == 3
        mockService.nStopped == 1
        testState.declareCalled
        testState.startCalled
        testState.handleByClassNameAndServiceIdCalled
        testState.handleByClassNameCalled
        testState.handleCalled
        testState.stopCalled
    }

    def 'TEST handle throws'() {
        given:
        final def dir = tempFolder.root
        final def file = new File(dir, 'Test.groovy')
        file.text = """\
            // Jexler {}
            JexlerDispatcher.dispatch(this)            
            void declare() {            
              mockService = new MockService(jexler, 'MockService')            
              testState = JexlerDispatcherSpec.testState
              testState.declareCalled = true            
            }            
            void start() {            
              services.add(mockService)            
              services.start()            
              testState.startCalled = true            
            }            
            void handleMockEventMockService(def event) {            
              mockService.notifyGotEvent()            
              testState.handleByClassNameAndServiceIdCalled = true            
              throw new RuntimeException('handle failed')            
            }            
            void stop() {            
              testState.stopCalled = true            
            }
            """.stripIndent()
        when:
        final def jexler = new Jexler(file, new JexlerContainer(dir))
        jexler.start()
        JexlerUtil.waitForStartup(jexler, MS_10_SEC)

        then:
        jexler.state == IDLE
        jexler.issues.empty

        when:
        final def mockService = MockService.getInstance('MockService')
        mockService.notifyJexler()
        JexlerUtil.waitAtLeast(MS_1_SEC)

        then:
        jexler.state == IDLE
        jexler.issues.size() == 1
        jexler.issues.first().service == jexler
        jexler.issues.first().message == 'Dispatch: Handler handleMockEventMockService failed.'
        jexler.issues.first().cause instanceof RuntimeException
        jexler.issues.first().cause.message == 'handle failed'

        when:
        jexler.forgetIssues()
        jexler.stop()
        JexlerUtil.waitForShutdown(jexler, MS_10_SEC)

        then:
        jexler.state == OFF
        jexler.issues.empty
    }

}
