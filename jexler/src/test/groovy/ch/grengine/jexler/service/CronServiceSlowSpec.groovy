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
import ch.grengine.jexler.test.SlowTests
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static CronService.CRON_NOW
import static CronService.CRON_NOW_AND_STOP
import static ServiceState.IDLE
import static ServiceState.OFF

/**
 * Tests the respective class.
 *
 * @author Alain Stalder
 */
@Category(SlowTests.class)
class CronServiceSlowSpec extends Specification {

    private final static long MS_4_SEC = 4000
    private final static long MS_2_SEC = 2000
    private final static String CRON_EVERY_SEC = '*/1 * * * * *'
    private final static String QUARTZ_CRON_EVERY_SEC = '*/1 * * * * ?'

    def 'TEST SLOW (10 sec) cron every sec'() {
        given:
        final def jexler = new TestJexler()

        when:
        final def service = new CronService(jexler, 'cronid')
        service.cron = CRON_EVERY_SEC

        then:
        service.id == 'cronid'
        service.cron == QUARTZ_CRON_EVERY_SEC

        when:
        service.start()

        then:
        service.state.on
        ServiceUtil.waitForStartup(service, MS_4_SEC)

        when:
        def event = jexler.takeEvent(MS_4_SEC)

        then:
        event.service.is(service)
        event instanceof CronEvent
        event.cron == QUARTZ_CRON_EVERY_SEC

        when:
        service.stop()

        then:
        ServiceUtil.waitForShutdown(service, MS_4_SEC)
        service.state.off

        when:
        event = jexler.takeEvent(MS_2_SEC)

        then:
        event == null || event instanceof CronEvent

        when:
        event = jexler.takeEvent(MS_2_SEC)

        then:
        event == null

        when:
        service.start()

        then:
        service.state.on
        ServiceUtil.waitForStartup(service, MS_4_SEC)

        when:
        service.start()

        then:
        service.state == IDLE

        when:
        event = jexler.takeEvent(MS_4_SEC)

        then:
        event.service.is(service)
        event instanceof CronEvent
        event.cron == QUARTZ_CRON_EVERY_SEC

        when:
        service.stop()

        then:
        ServiceUtil.waitForShutdown(service, MS_4_SEC)
        service.state.off

        when:
        event = jexler.takeEvent(MS_2_SEC)

        then:
        event == null || event instanceof CronEvent

        when:
        event = jexler.takeEvent(MS_2_SEC)

        then:
        event == null

        when:
        service.stop()

        then:
        service.state.off

        when:
        service.start()

        then:
        service.state == IDLE

        when:
        service.zap()

        then:
        service.state == OFF

        when:
        service.zap()

        then:
        service.state == OFF

        cleanup:
        jexler.container.close()
    }

    def 'TEST SLOW (10 sec) cron now'() {
        given:
        final def jexler = new TestJexler()

        when:
        final def service = new CronService(jexler, 'cronid').setCron(CRON_NOW)
        def event = jexler.takeEvent(MS_2_SEC)

        then:
        event == null

        when:
        service.start()

        then:
        service.state.on

        when:
        event = jexler.takeEvent(MS_2_SEC)

        then:
        event.service.is(service)
        event instanceof CronEvent
        event.cron == CRON_NOW
        jexler.takeEvent(MS_2_SEC) == null

        when:
        service.stop()

        then:
        ServiceUtil.waitForShutdown(service, MS_4_SEC)
        service.state.off
        jexler.takeEvent(MS_2_SEC) == null

        when:
        service.start()

        then:
        service.state.on

        when:
        event = jexler.takeEvent(MS_2_SEC)

        then:
        event.service.is(service)
        event instanceof CronEvent
        event.cron == CRON_NOW
        jexler.takeEvent(MS_2_SEC) == null

        when:
        service.stop()

        then:
        ServiceUtil.waitForShutdown(service, MS_4_SEC)
        service.state.off
        jexler.takeEvent(MS_2_SEC) == null
    }

    def 'TEST SLOW (6 sec) cron now+stop'() {
        given:
        final def jexler = new TestJexler()

        when:
        final def service = new CronService(jexler, 'cronid').setCron(CRON_NOW_AND_STOP)
        def event = jexler.takeEvent(MS_2_SEC)

        then:
        event == null

        when:
        service.start()

        then:
        service.state.off

        when:
        event = jexler.takeEvent(MS_2_SEC)

        then:
        event.service.is(service)
        event instanceof CronEvent
        event.cron == CRON_NOW_AND_STOP

        when:
        event = jexler.takeEvent(MS_2_SEC)

        then:
        event instanceof StopEvent
        jexler.takeEvent(MS_2_SEC) == null

        when:
        service.start()

        then:
        service.state.off

        when:
        event = jexler.takeEvent(MS_2_SEC)

        then:
        event.service.is(service)
        event instanceof CronEvent
        event.cron == CRON_NOW_AND_STOP

        when:
        event = jexler.takeEvent(MS_2_SEC)

        then:
        event instanceof StopEvent
        jexler.takeEvent(MS_2_SEC) == null
    }

}
