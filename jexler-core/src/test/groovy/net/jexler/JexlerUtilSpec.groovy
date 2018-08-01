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

import net.jexler.test.FastTests

import org.junit.experimental.categories.Category
import spock.lang.Specification

/**
 * Tests the respective class.
 *
 * @author $(whois jexler.net)
 */
@Category(FastTests.class)
class JexlerUtilSpec extends Specification {

    private static class NoStackTraceException extends Exception {
        @Override
        void printStackTrace(final PrintWriter writer) {
            throw new RuntimeException()
        }
    }

    def 'TEST getStackTrace: empty stack trace'() {
        expect:
        JexlerUtil.getStackTrace(cause).empty

        where:
        cause << [ null, new NoStackTraceException() ]
    }

    def 'TEST getStackTrace: nonempty stack trace'() {
        when:
        final def stackTrace = JexlerUtil.getStackTrace(new Exception())

        then:
        stackTrace.startsWith('java.lang.Exception')
        stackTrace.contains("${this.class.simpleName}.groovy")
    }

    def 'TEST toSingleLine: general'() {
        expect:
        JexlerUtil.toSingleLine(input) == output

        where:
        input                                     | output
        null                                      | null
        ''                                        | ''
        '%n 55'                                   | '%n 55'
        'got \r this \n and \r\n that \r\n\n\r .' | 'got %n this %n and %n that %n%n%n .'

    }

    def 'TEST waitAtLeast: general'() {
        expect:
        final long t0 = System.currentTimeMillis()
        JexlerUtil.waitAtLeast(ms)
        final long t1 = System.currentTimeMillis()
        t1-t0 >= ms
        t1-t0 < 2000 // (might be false if a lot of load on test system)

        where:
        ms << (0..50).step(5)
    }

}
