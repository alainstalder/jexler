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

import ch.grengine.jexler.test.FastTests
import org.junit.experimental.categories.Category
import spock.lang.Specification

/**
 * Tests the respective class.
 *
 * @author Alain Stalder
 */
@Category(FastTests.class)
class ServiceUtilSpec extends Specification {

    def 'TEST to quartz cron, valid values'() {
        expect:
        quartzCron == ServiceUtil.toQuartzCron(cron)

        where:
        cron               | quartzCron
        '* * * * *'        | '0 * * * * ?'
        '* * */2 * *'      | '0 * * */2 * ?'
        '* * * * 1-5'      | '0 * * ? * 1-5'
        '* * * * * *'      | '* * * * * ?'
        '*/30 * * */2 * *' | '*/30 * * */2 * ?'
        '15 * * * * 1-5'   | '15 * * ? * 1-5'
        '* * * * * ?'      | '* * * * * ?'
        '* * * ? * *'      | '* * * ? * *'
        '* * * * * ? 1970' | '* * * * * ? 1970'
        'now'              | 'now'
        'now+stop'         | 'now+stop'
    }

    def 'TEST to quartz cron, invalid values'() {
        when:
        ServiceUtil.toQuartzCron('invalid')

        then:
        IllegalArgumentException ex = thrown()
        ex.message.startsWith("Could not parse cron 'invalid':")

        when:
        ServiceUtil.toQuartzCron('* * ? * ?')

        then:
        ex = thrown()
        ex.message.startsWith("Could not parse cron '0 * * ? * ?':")

        when:
        ServiceUtil.toQuartzCron('* * 1-31 * 1-7')

        then:
        ex = thrown()
        ex.message.startsWith("Could not parse cron '0 * * 1-31 * 1-7':")
    }

}
