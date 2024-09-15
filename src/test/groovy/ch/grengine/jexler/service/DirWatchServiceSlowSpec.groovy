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
import ch.grengine.jexler.TestJexler
import com.sun.nio.file.SensitivityWatchEventModifier
import spock.lang.Specification
import spock.lang.Tag
import spock.lang.TempDir

import java.nio.file.StandardWatchEventKinds

import static ServiceState.IDLE
import static ServiceState.OFF

/**
 * Tests the respective class.
 *
 * @author Alain Stalder
 */
@Tag("slow")
class DirWatchServiceSlowSpec extends Specification {

    @TempDir
    File tempDir;

    private final static long MS_3_SEC = 3000
    private final static String CRON_EVERY_SEC = '*/1 * * * * *'

    def 'TEST SLOW (40 sec) create/modify/remove files in watch dir'() {
        given:
        def watchDir = new File(tempDir, "watch1")
        watchDir.mkdir()
        final def jexler = new TestJexler()

        when:
        final def service = new DirWatchService(jexler, 'watchid')
        service.dir = watchDir
        service.cron = CRON_EVERY_SEC
        service.modifiers = [ SensitivityWatchEventModifier.HIGH ]

        then:
        service.id == 'watchid'

        when:
        service.start()

        then:
        service.state.on
        ServiceUtil.waitForStartup(service, MS_3_SEC)
        jexler.takeEvent(MS_3_SEC) == null

        when:
        checkCreateModifyDeleteEventsTriggered(jexler, service, watchDir)

        service.stop()

        then:
        service.state.off
        ServiceUtil.waitForShutdown(service, MS_3_SEC)

        when:
        // create file after service stop
        new File(watchDir, 'temp2').text = 'hello too'

        then:
        jexler.takeEvent(MS_3_SEC) == null

        when:
        // different watch directory
        watchDir = new File(tempDir, "watch2")
        watchDir.mkdir()
        service.dir = watchDir
        service.start()

        then:
        service.state.on
        ServiceUtil.waitForStartup(service, MS_3_SEC)
        jexler.takeEvent(MS_3_SEC) == null

        when:
        service.start()

        then:
        service.state == IDLE

        when:
        checkCreateModifyDeleteEventsTriggered(jexler, service, watchDir)

        then:
        // delete watch directory
        watchDir.delete()
        jexler.takeEvent(MS_3_SEC) == null

        when:
        service.stop()

        then:
        ServiceUtil.waitForShutdown(service, MS_3_SEC)

        when:
        service.stop()

        then:
        service.state.off

        when:
        // different watch directory
        watchDir = new File(tempDir, "watch3")
        watchDir.mkdir()
        service.dir = watchDir
        service.start()

        then:
        service.state.on

        when:
        service.zap()

        then:
        service.state == OFF

        when:
        service.zap()

        then:
        service.state == OFF
    }

    private static void checkCreateModifyDeleteEventsTriggered(
            final Jexler jexler, final Service service, final File watchDir) {

        // create file
        final def tempFile = new File(watchDir, 'temp')
        tempFile.createNewFile()

        def event = jexler.takeEvent(MS_3_SEC)
        assert event instanceof DirWatchEvent
        assert event.service == service
        assert event.file.canonicalPath == tempFile.canonicalPath
        assert event.kind == StandardWatchEventKinds.ENTRY_CREATE
        assert jexler.takeEvent(MS_3_SEC) == null

        // modify file
        tempFile.text = 'hello there'

        event = jexler.takeEvent(MS_3_SEC)
        assert event instanceof DirWatchEvent
        assert event.service == service
        assert event.file.canonicalPath == tempFile.canonicalPath
        assert event.kind == StandardWatchEventKinds.ENTRY_MODIFY
        assert jexler.takeEvent(MS_3_SEC) == null

        // delete file
        assert tempFile.delete()

        event = jexler.takeEvent(MS_3_SEC)
        assert event instanceof DirWatchEvent
        assert event.service == service
        assert event.file.canonicalPath == tempFile.canonicalPath
        assert event.kind == StandardWatchEventKinds.ENTRY_DELETE
        assert jexler.takeEvent(MS_3_SEC) == null
    }

}
