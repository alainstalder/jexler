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
class IssueTrackerBaseSpec extends Specification {

    def 'TEST track and forget'() {
        when:
        final def tracker = new IssueTrackerBase()

        then:
        tracker.issues.empty

        when:
        tracker.trackIssue(new Issue(null, 'issue1', null))

        then:
        tracker.issues.size() == 1
        tracker.issues.first().message == 'issue1'

        when:
        JexlerUtil.waitAtLeast(10)
        tracker.trackIssue(null, 'issue2', null)

        then:
        tracker.issues.size() == 2
        tracker.issues.first().message == 'issue2'
        tracker.issues.get(1).message == 'issue1'

        when:
        tracker.forgetIssues()

        then:
        tracker.issues.empty
    }

}
