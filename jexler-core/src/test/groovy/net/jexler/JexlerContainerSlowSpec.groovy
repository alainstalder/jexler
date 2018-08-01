/*
   Copyright 2012-now $(whois jexler.net)

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

package net.jexler


import net.jexler.test.SlowTests

import org.junit.Rule
import org.junit.experimental.categories.Category
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static net.jexler.JexlerUtil.SHUTDOWN_TIMEOUT_MSG
import static net.jexler.JexlerUtil.STARTUP_TIMEOUT_MSG
import static net.jexler.service.ServiceState.BUSY_STARTING
import static net.jexler.service.ServiceState.BUSY_STOPPING
import static net.jexler.service.ServiceState.IDLE
import static net.jexler.service.ServiceState.OFF

/**
 * Tests the respective class.
 *
 * @author $(whois jexler.net)
 */
@Category(SlowTests.class)
class JexlerContainerSlowSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    private final static long MS_1_SEC = 1000
    private final static long MS_3_SEC = 3000
    private final static long MS_6_SEC = 6000
    
    def 'TEST SLOW (12 sec) startup and shutdown too slower than time waited'() {
        given:
        final def dir = tempFolder.root
        final def jexlerBodyFast = """\
            while (true) {
              event = events.take()
              if (event instanceof StopEvent) {
                return
              }
            }
            """.stripIndent()
        final def jexlerBodySlow = """\
            log.info('before startup wait ' + jexler.id)
            JexlerUtil.waitAtLeast($MS_3_SEC)
            log.info('after startup wait ' + jexler.id)
            while (true) {
              event = events.take()
              if (event instanceof StopEvent) {
                // set explicitly because cannot easily wait after returns
                jexler.state = ServiceState.BUSY_STOPPING
                JexlerUtil.waitAtLeast($MS_3_SEC)
                return
              }
            }
            """.stripIndent()
        new File(dir, 'Jexler1.groovy').text = "// Jexler { autostart = true }\n$jexlerBodySlow"
        new File(dir, 'Jexler2.groovy').text = "// Jexler { autostart = true }\n$jexlerBodyFast"
        new File(dir, 'Jexler3.groovy').text = "// Jexler { autostart = true }\n$jexlerBodySlow"

        when:
        final def container = new JexlerContainer(dir)
        final def jexler1 = container.getJexler('Jexler1')
        final def jexler2 = container.getJexler('Jexler2')
        final def jexler3 = container.getJexler('Jexler3')

        then:
        container.jexlers.size() == 3
        container.state.off
        jexler1.state.off
        jexler2.state.off
        jexler3.state.off
        container.issues.empty
        jexler1.issues.empty
        jexler2.issues.empty
        jexler3.issues.empty

        when:
        container.start()
        JexlerUtil.waitForStartup(container, MS_1_SEC)

        then:
        container.state == BUSY_STARTING
        jexler1.state == BUSY_STARTING
        jexler2.state == IDLE
        jexler3.state == BUSY_STARTING
        container.issues.empty
        jexler1.issues.size() == 1
        jexler1.issues.first().message == STARTUP_TIMEOUT_MSG
        jexler2.issues.empty
        jexler3.issues.size() == 1
        jexler3.issues.first().message == STARTUP_TIMEOUT_MSG

        when:
        container.forgetIssues()
        jexler1.forgetIssues()
        jexler2.forgetIssues()
        jexler3.forgetIssues()
        JexlerUtil.waitForStartup(container, MS_6_SEC)

        then:
        container.state == IDLE
        jexler1.state == IDLE
        jexler2.state == IDLE
        jexler3.state == IDLE
        container.issues.empty
        jexler1.issues.empty
        jexler2.issues.empty
        jexler3.issues.empty

        when:
        container.stop()
        JexlerUtil.waitForShutdown(container, MS_1_SEC)

        then:
        container.state == BUSY_STOPPING
        jexler1.state == BUSY_STOPPING
        jexler2.state == OFF
        jexler3.state == BUSY_STOPPING
        container.issues.empty
        jexler1.issues.size() == 1
        jexler1.issues.first().message == SHUTDOWN_TIMEOUT_MSG
        jexler2.issues.empty
        jexler3.issues.size() == 1
        jexler3.issues.first().message == SHUTDOWN_TIMEOUT_MSG

        when:
        container.forgetIssues()
        jexler1.forgetIssues()
        jexler2.forgetIssues()
        jexler3.forgetIssues()
        JexlerUtil.waitForShutdown(container, MS_6_SEC)

        then:
        container.state == OFF
        jexler1.state == OFF
        jexler2.state == OFF
        jexler3.state == OFF
        container.issues.empty
        jexler1.issues.empty
        jexler2.issues.empty
        jexler3.issues.empty
   }

}
