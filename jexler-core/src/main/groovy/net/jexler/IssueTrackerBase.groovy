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

import net.jexler.service.Service

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Implementation of issue tracker interface.
 *
 * @author $(whois jexler.net)
 */
@CompileStatic
class IssueTrackerBase implements IssueTracker {

    private static final Logger LOG = LoggerFactory.getLogger(IssueTrackerBase.class)

    /** List of issues. */
    private final List<Issue> issues

    /**
     * Default constructor.
     */
    IssueTrackerBase() {
        issues = new LinkedList<>()
    }

    @Override
    void trackIssue(final Issue issue) {
        LOG.error(issue.toString())
        synchronized (issues) {
            issues.add(issue)
        }
    }

    @Override
    void trackIssue(final Service service, final String message, final Throwable cause) {
        trackIssue(new Issue(service, message, cause))
    }

    /**
     * Returns an unmodifiable list.
     */
    @Override
    List<Issue> getIssues() {
        synchronized(issues) {
            Collections.sort(issues)
            return Collections.unmodifiableList(issues)
        }
    }

    @Override
    void forgetIssues() {
        synchronized(issues) {
            issues.clear()
        }
    }

}
