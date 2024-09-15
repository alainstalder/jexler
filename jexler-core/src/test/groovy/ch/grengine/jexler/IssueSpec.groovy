/*
   Copyright 2012-now by Alain Stalder. Made in Switzerland.

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

package ch.grengine.jexler


import ch.grengine.jexler.service.MockService
import ch.grengine.jexler.test.FastTests
import org.junit.experimental.categories.Category
import spock.lang.Specification

/**
 * Tests the respective class.
 *
 * @author Alain Stalder
 */
@Category(FastTests.class)
class IssueSpec extends Specification {

    def 'TEST construct and get null values'() {
        when:
        final def issue = new Issue(null, null, null)

        then:
        issue.service == null
        issue.message == null
        issue.cause == null
        issue.stackTrace == ''
        issue.toString() == "Issue: [message=null,service=null,cause=null,stackTrace='']"
    }

    def 'TEST construct and get without cause'() {
        when:
        final def service = new MockService(null, 'mockid')
        final def message = 'hi \r a \n b \r\n c \r\n\r\n'
        final def issue = new Issue(service, message, null)

        then:
        issue.service == service
        issue.message == message
        issue.cause == null
        issue.stackTrace == ''
        issue.toString() == "Issue: [message='hi %n a %n b %n c %n%n',service='${MockService.class.name}:mockid'," +
                "cause=null,stackTrace='']"
    }

    def 'TEST construct and get with cause'() {
        when:
        final def service = new MockService(null, 'mockid')
        final def message = 'hi'
        final def cause = new RuntimeException('run')
        final def issue = new Issue(service, message, cause)

        then:
        issue.service == service
        issue.message == message
        issue.cause == cause
        issue.stackTrace != ''
        issue.toString().startsWith("Issue: [message='hi',service='${MockService.class.name}:mockid'" +
                ",cause='java.lang.RuntimeException: run',stackTrace='java.lang.RuntimeException: run")
        !issue.toString().contains('\r')
        !issue.toString().contains('\n')
    }

    def 'TEST compare'() {
        when:
        final def issueEarlier = new Issue(null, null, null)
        JexlerUtil.waitAtLeast(10)
        final def issueLater = new Issue(null, null, null)

        then:
        issueEarlier.compareTo(issueLater) > 0
        issueLater.compareTo(issueEarlier) < 0
    }

}
