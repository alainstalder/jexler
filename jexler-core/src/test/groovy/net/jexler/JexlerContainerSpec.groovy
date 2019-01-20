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


import net.jexler.service.StopEvent
import net.jexler.test.FastTests

import org.junit.Rule
import org.junit.experimental.categories.Category
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static net.jexler.service.ServiceState.BUSY_STARTING
import static net.jexler.service.ServiceState.IDLE
import static net.jexler.service.ServiceState.OFF

/**
 * Tests the respective class.
 *
 * @author $(whois jexler.net)
 */
@Category(FastTests.class)
class JexlerContainerSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    private final static long MS_1_SEC = 1000
    private final static long MS_10_SEC = 10000

    def 'TEST main functionality in detail'() {
        given:
        final def dir = tempFolder.root
        final def jexlerBody = """\
            while (true) {
              event = events.take()
              if (event instanceof StopEvent) {
                return
              }
            }
            """.stripIndent()
        new File(dir, 'Jexler1.groovy').text = "// Jexler { autostart = false }\n$jexlerBody"
        new File(dir, 'Jexler2.groovy').text = "// Jexler { autostart = true }\n$jexlerBody"
        new File(dir, 'Jexler3.groovy').text = "// Jexler { autostart = true }\n$jexlerBody"
        new File(dir, 'Jexler4.script').text = 'foo.bar=xyz'

        when:
        def container = new JexlerContainer(dir)

        then:
        container.state == OFF
        container.state.off
        container.dir == dir
        container.id == dir.name
        container.jexlers.size() == 3
        container.issues.empty

        when:
        final def jexler1 = container.getJexler('Jexler1')
        final def jexler2 = container.getJexler('Jexler2')
        final def jexler3 = container.getJexler('Jexler3')

        then:
        jexler1.id == 'Jexler1'
        jexler2.id == 'Jexler2'
        jexler3.id == 'Jexler3'
        jexler1.state == OFF
        jexler2.state == OFF
        jexler3.state == OFF

        when:
        container.start()
        JexlerUtil.waitForStartup(container, MS_10_SEC)

        then:
        container.state == IDLE
        container.state.on
        jexler1.state == OFF
        jexler2.state == IDLE
        jexler3.state == IDLE
        container.issues.empty

        when:
        container.stop()
        JexlerUtil.waitForShutdown(container, MS_10_SEC)

        then:
        container.state == OFF
        jexler1.state == OFF
        jexler2.state == OFF
        jexler3.state == OFF
        container.issues.empty

        when:
        container.start()
        JexlerUtil.waitForStartup(container, MS_10_SEC)

        then:
        container.state == IDLE
        container.state.on
        jexler1.state == OFF
        jexler2.state == IDLE
        jexler3.state == IDLE
        container.issues.empty

        when:
        jexler3.handle(new StopEvent(jexler3))
        JexlerUtil.waitAtLeast(MS_1_SEC)

        then:
        container.state == IDLE
        container.state.on
        jexler1.state == OFF
        jexler2.state == IDLE
        jexler3.state == OFF
        container.issues.empty

        when:
        // delete file for Jexler2
        assert jexler2.file.delete()

        then:
        container.getJexler('Jexler2') == jexler2
        container.jexlers.size() == 3

        when:
        // don't remove running jexler even if file is gone
        container.refresh()

        then:
        container.getJexler('Jexler2') == jexler2
        container.issues.empty
        container.jexlers.size() == 3

        when:
        container.stop()
        JexlerUtil.waitForShutdown(container, MS_10_SEC)
        // remove it now
        container.refresh()

        then:
        container.state == OFF
        jexler1.state == OFF
        jexler2.state == OFF
        jexler3.state == OFF
        container.issues.empty
        container.jexlers.size() == 2

        when:
        container.start()
        JexlerUtil.waitForStartup(container, MS_10_SEC)

        then:
        container.state == IDLE
        container.issues.empty
        container.jexlers.size() == 2

        when:
        container.stop()
        JexlerUtil.waitForShutdown(container, MS_10_SEC)

        then:
        container.state.off

        when:
        new File(dir, 'Jexler5.groovy').text = "// Jexler { autostart = true }\nwhile(true){}"
        container = new JexlerContainer(dir)
        container.start()
        JexlerUtil.waitForStartup(container, MS_10_SEC)

        then:
        container.state == BUSY_STARTING

        when:
        container.zap()

        then:
        container.state == OFF
    }

    def 'TEST track issue'() {
        given:
        final def dir = tempFolder.root
        final def container = new JexlerContainer(dir)

        when:
        final def e = new RuntimeException()
        container.trackIssue(null, 'some issue', e)

        then:
        container.issues.size() == 1
        container.issues.first().service == null
        container.issues.first().message == 'some issue'
        container.issues.first().cause == e

        when:
        container.forgetIssues()

        then:
        container.issues.empty

        when:
        final def t = new Throwable()
        container.trackIssue(new Issue(container, 'container issue', t))

        then:
        container.issues.size() == 1
        container.issues.first().service == container
        container.issues.first().message == 'container issue'
        container.issues.first().cause == t

        when:
        container.forgetIssues()

        then:
        container.issues.empty
    }

    def 'TEST constructor throws because directory does not exist'() {
        when:
        def dir = new File('does-not-exist')
        new JexlerContainer(dir)

        then:
        final RuntimeException e = thrown()
        e.message == "Directory '$dir.absolutePath' does not exist."
    }

    def 'TEST constructor throws because file is not a directory'() {
        when:
        final def dir = tempFolder.root
        def file = new File(dir, "file.tmp")
        file.createNewFile()
        new JexlerContainer(file)

        then:
        final RuntimeException e = thrown()
        e.message == "File '$file.absolutePath' is not a directory."
    }

    def 'TEST get jexler id'() {
        when:
        final def dir = tempFolder.root
        final def container = new JexlerContainer(dir)

        then:
        container.getJexlerId(new File(dir, 'Foo.groovy')) == 'Foo'
        container.getJexlerId(new File('Foo.groovy'))      == 'Foo'
        container.getJexlerId(new File('Foo.java'))        == null
        container.getJexlerId(new File('Foo.java.groovy')) == 'Foo.java'
        container.getJexlerId(new File('Foo.groovy.java')) == null
    }

    def 'TEST get jexler file'() {
        given:
        final def dir = tempFolder.root
        final def container = new JexlerContainer(dir)

        when:
        final def file = container.getJexlerFile('Foo')

        then:
        file.canonicalPath == new File(dir, 'Foo.groovy').canonicalPath
    }

    def 'TEST shared scheduler and close'() {
        given:
        final def dir = tempFolder.root
        final def container = new JexlerContainer(dir)

        when:
        final def scheduler1 = container.scheduler
        final def scheduler2 = container.scheduler

        then:
        // must be same reference
        scheduler1.is(scheduler2)
        scheduler1.started
        !scheduler1.shutdown

        when:
        container.close()

        then:
        scheduler1.shutdown

        when:
        final def scheduler3 = container.scheduler

        then:
        // must get a new reference
        !scheduler1.is(scheduler3)
        scheduler1.shutdown
        scheduler3.started
        !scheduler3.shutdown

        when:
        container.close()

        then:
        scheduler3.shutdown
    }

    def 'TEST getAsConfig'() {
        given:
        final def dir = tempFolder.root
        final def file = new File(dir, 'Test.groovy')
        file.text = "a { x=1; y=true; z { aa='hello' } }"

        when:
        final def container = new JexlerContainer(dir)

        then:
        container.getAsConfig('Test').a.x == 1
        container.getAsConfig('Test').a.y == true
        container.getAsConfig('Test').a.z.aa == 'hello'
    }

}
