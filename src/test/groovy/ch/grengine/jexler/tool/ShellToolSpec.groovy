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

package ch.grengine.jexler.tool

import spock.lang.Specification
import spock.lang.Tag
import spock.lang.TempDir

/**
 * Tests the respective class.
 *
 * @author Alain Stalder
 */
class ShellToolSpec extends Specification {

    @TempDir
    File tempDir;

    def 'TEST default'() {
        given:
        final def tool = new ShellTool()

        expect:
        tool.workingDirectory == null
        tool.env == null
        tool.stdoutLineHandler == null
        tool.stderrLineHandler == null
        final def result = tool.run(cmd)
        result != null
        result.rc == 0
        result.stdout != ''
        result.stderr == ''

        where:
        cmd << [ (windows ? 'cmd /c dir'           : 'ls -l'),
                 (windows ? [ 'cmd', '/c', 'dir' ] : [ 'ls', '-l' ]) ]
    }

    def 'TEST with working directory and stdout line handler'() {
        given:
        final def tool = new ShellTool()

        final def dir = tempDir
        final def file1 = new File(dir, 'file1')
        final def file2 = new File(dir, 'file2')
        file1.createNewFile()
        file2.createNewFile()
        tool.workingDirectory = dir

        def testStdout = ''
        tool.stdoutLineHandler = { testStdout += it }

        expect:
        tool.workingDirectory == dir
        tool.env == null
        tool.stdoutLineHandler != null
        tool.stderrLineHandler == null
        final def result = tool.run(cmd)
        result.rc == 0
        result.stdout != ''
        result.stdout.contains('file1')
        result.stdout.contains('file2')
        result.stderr == ''
        testStdout != ''
        testStdout.contains('file1')
        testStdout.contains('file2')

        where:
        cmd << [ (windows ? 'cmd /c dir'           : 'ls -l'),
                 (windows ? [ 'cmd', '/c', 'dir' ] : [ 'ls', '-l' ]) ]
    }

    def 'TEST with custom environment and stdout line handler'() {
        given:
        final def tool = new ShellTool()
        tool.environment = [ 'MYVAR' : 'there' ]

        def stdout = ''
        tool.stdoutLineHandler = { stdout += it }

        when:
        final def cmd = (windows ? [ 'cmd', '/c', 'echo hello %MyVar%' ] : [ 'sh', '-c', 'echo hello $MYVAR' ])
        final def result = tool.run(cmd)

        then:
        tool.workingDirectory == null
        tool.env.size() == 1
        tool.env.MYVAR == 'there'
        tool.stdoutLineHandler != null
        tool.stderrLineHandler == null
        result.rc == 0
        result.stdout != ''
        result.stdout.contains('hello there')
        result.stderr == ''
        stdout != ''
        stdout.contains('hello there')
    }

    def 'TEST error in command, with stderr line handler'() {
        given:
        final def tool = new ShellTool()

        def stderr = ''
        tool.stderrLineHandler = { stderr += it }

        when:
        def cmd = (windows ? 'cmd /c type there-is-no-such-file' : 'cat there-is-no-such-file')
        def result = tool.run(cmd)

        then:
        tool.workingDirectory == null
        tool.env == null
        tool.stdoutLineHandler == null
        tool.stderrLineHandler != null
        result.rc != 0
        result.stdout == ''
        result.stderr != ''
        !result.stderr.contains('Exception')
        stderr != ''
        !stderr.contains('Exception')
    }

    def 'TEST exception if no such command'() {
        given:
        final def tool = new ShellTool()

        expect:
        final def result = tool.run(cmd)
        result.rc != 0
        result.stdout == ''
        result.stderr != ''
        result.stderr.contains('java.io.IOException')

        where:
        cmd << [ 'there-is-no-such-command', [ 'there-is-no-such-command', 'arg' ] ]
    }

    def 'TEST result to string'() {
        expect:
        result.toString() == string

        where:
        result                                                        | string
        new ShellTool.Result(5, 'file1\nfile2\n', '') | "[rc=5,stdout='file1%nfile2%n',stderr='']"
        new ShellTool.Result(-1, '', 'error')         | "[rc=-1,stdout='',stderr='error']"
    }

    private static boolean isWindows() {
        return System.getProperty('os.name').startsWith('Windows')
    }

}
