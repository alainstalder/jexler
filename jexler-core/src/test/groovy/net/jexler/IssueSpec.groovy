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

package net.jexler

import net.jexler.service.MockService
import net.jexler.test.FastTests
import org.junit.experimental.categories.Category
import spock.lang.Specification

/**
 * Tests the respective class.
 *
 * @author $(whois jexler.net)
 */
@Category(FastTests.class)
class IssueSpec extends Specification {

    def 'TEST construct and get without cause'() {
        expect:
        issue.service == service
        issue.message == message
        issue.cause == cause
        issue.toString() == string

        where:
        service | message | cause | string
        null | null | null |
                "Issue: [message=null,service=null,cause=null,stackTrace='']"
        new MockService(null, 'mockid') | 'hi \r a \n b \r\n c \r\n\r\n' | null |
                "Issue: [message='hi %n a %n b %n c %n%n',service='${MockService.class.name}:mockid',cause=null,stackTrace='']"
        issue = new Issue(service, message, cause)
    }

    def 'TEST construct and get with cause'() {
        expect:
        issue.service == service
        issue.message == message
        issue.cause == cause
        issue.toString().startsWith(string)
        !issue.toString().contains('\r')
        !issue.toString().contains('\n')

        where:
        service | message | cause | string
        new MockService(null, 'mockid') | 'hi' | new RuntimeException('run') |
                "Issue: [message='hi',service='${MockService.class.name}:mockid'" +
                ",cause='java.lang.RuntimeException: run',stackTrace='java.lang.RuntimeException: run"
        issue = new Issue(service, message, cause)
    }

    def 'TEST compare'() {
        when:
        def issueEarlier = new Issue(null, null, null)
        Thread.sleep(10)
        def issueLater = new Issue(null, null, null)

        then:
        issueEarlier.compareTo(issueLater) > 0
        issueLater.compareTo(issueEarlier) < 0
    }

}
