/*
   Copyright 2012-now $(whois jexler.net)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package net.jexler.service

import net.jexler.TestJexler
import net.jexler.test.SlowTests

import org.junit.experimental.categories.Category
import spock.lang.Specification

/**
 * Tests the respective class.
 *
 * @author $(whois jexler.net)
 */
@Category(SlowTests.class)
class CronServiceSlowSpec extends Specification {

    private final static long MS_2_SEC = 2000
    private final static long MS_1_SEC = 1000
    private final static String CRON_EVERY_SEC = '*/1 * * * * *'
    private final static String QUARTZ_CRON_EVERY_SEC = '*/1 * * * * ?'

    def 'TEST SLOW (5 sec) cron every sec'() {
        given:
        def jexler = new TestJexler()

        when:
        def service = new CronService(jexler, 'cronid')
        service.cron = CRON_EVERY_SEC

        then:
        service.id == 'cronid'
        service.cron == QUARTZ_CRON_EVERY_SEC

        when:
        service.start()

        then:
        service.state.on
        ServiceUtil.waitForStartup(service, MS_2_SEC)

        when:
        def event = jexler.takeEvent(MS_2_SEC)

        then:
        event.service.is(service)
        event instanceof CronEvent
        event.cron == QUARTZ_CRON_EVERY_SEC

        when:
        service.stop()

        then:
        ServiceUtil.waitForShutdown(service, MS_2_SEC)
        service.state.off
        jexler.takeEvent(MS_2_SEC) == null

        when:
        service.start()

        then:
        service.state.on
        ServiceUtil.waitForStartup(service, MS_2_SEC)

        when:
        service.start()

        then:
        service.state == ServiceState.IDLE

        when:
        event = jexler.takeEvent(MS_2_SEC)

        then:
        event.service.is(service)
        event instanceof CronEvent
        event.cron == QUARTZ_CRON_EVERY_SEC

        when:
        service.stop()

        then:
        ServiceUtil.waitForShutdown(service, MS_2_SEC)
        service.state.off
        jexler.takeEvent(MS_2_SEC) == null

        when:
        service.stop()

        then:
        service.state.off

        when:
        service.start()

        then:
        service.state == ServiceState.IDLE

        when:
        service.zap()

        then:
        service.state == ServiceState.OFF

        cleanup:
        jexler.container.close()
    }

    def 'TEST SLOW (6 sec) cron now'() {
        given:
        def jexler = new TestJexler()

        when:
        def service = new CronService(jexler, 'cronid').setCron(CronService.CRON_NOW)
        def event = jexler.takeEvent(MS_1_SEC)

        then:
        event == null

        when:
        service.start()

        then:
        service.state.on

        when:
        event = jexler.takeEvent(MS_1_SEC)

        then:
        event.service.is(service)
        event instanceof CronEvent
        event.cron == CronService.CRON_NOW
        jexler.takeEvent(MS_1_SEC) == null

        when:
        service.stop()

        then:
        ServiceUtil.waitForShutdown(service, MS_2_SEC)
        service.state.off
        jexler.takeEvent(MS_1_SEC) == null

        when:
        service.start()

        then:
        service.state.on

        when:
        event = jexler.takeEvent(MS_1_SEC)

        then:
        event.service.is(service)
        event instanceof CronEvent
        event.cron == CronService.CRON_NOW
        jexler.takeEvent(MS_1_SEC) == null

        when:
        service.stop()

        then:
        ServiceUtil.waitForShutdown(service, MS_2_SEC)
        service.state.off
        jexler.takeEvent(MS_1_SEC) == null
    }

    def 'TEST SLOW (4 sec) cron now+stop'() {
        given:
        def jexler = new TestJexler()

        when:
        def service = new CronService(jexler, 'cronid').setCron(CronService.CRON_NOW_AND_STOP)
        def event = jexler.takeEvent(MS_1_SEC)

        then:
        event == null

        when:
        service.start()

        then:
        service.state.off

        when:
        event = jexler.takeEvent(MS_1_SEC)

        then:
        event.service.is(service)
        event instanceof CronEvent
        event.cron == CronService.CRON_NOW_AND_STOP

        when:
        event = jexler.takeEvent(MS_1_SEC)

        then:
        event instanceof StopEvent
        jexler.takeEvent(MS_1_SEC) == null

        when:
        service.start()

        then:
        service.state.off

        when:
        event = jexler.takeEvent(MS_1_SEC)

        then:
        event.service.is(service)
        event instanceof CronEvent
        event.cron == CronService.CRON_NOW_AND_STOP

        when:
        event = jexler.takeEvent(MS_1_SEC)

        then:
        event instanceof StopEvent
        jexler.takeEvent(MS_1_SEC) == null
    }

}
