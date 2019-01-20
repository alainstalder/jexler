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

package ch.artecat.jexler.service

import ch.artecat.jexler.TestJexler
import ch.artecat.jexler.test.FastTests

import org.junit.experimental.categories.Category
import spock.lang.Specification

import java.nio.file.StandardWatchEventKinds

/**
 * Tests the respective class.
 *
 * @author Jex Jexler (Alain Stalder)
 */
@Category(FastTests.class)
class DirWatchServiceSpec extends Specification {

    def 'TEST no watch dir'() {
        given:
        final def dirNotExist = new File('does-not-exist')
        final def jexler = new TestJexler()

        when:
        final def service = new DirWatchService(jexler, 'watchid')
        service.with {
            dir = dirNotExist
            kinds = [ StandardWatchEventKinds.ENTRY_CREATE ]
            cron = '* * * * *'
            scheduler = null
        }
        service.start()

        then:
        service.state.off
        service.dir == dirNotExist
        service.kinds.size() == 1
        service.kinds.first() == StandardWatchEventKinds.ENTRY_CREATE
        service.modifiers.empty
        service.cron == '0 * * * * ?'
        service.scheduler == null
        jexler.issues.size() == 1
        jexler.issues.first().service == service
        jexler.issues.first().message.startsWith('Could not create watch service or key')
        jexler.issues.first().cause instanceof IOException
    }

}
