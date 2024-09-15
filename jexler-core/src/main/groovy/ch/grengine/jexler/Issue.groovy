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

package ch.grengine.jexler


import ch.grengine.jexler.service.Service
import groovy.transform.CompileStatic

/**
 * Issue.
 *
 * Issues are typically created and attached to a jexler
 * or jexler container if something could not be done,
 * often because some exception occurred.
 *
 * @author Alain Stalder
 */
@CompileStatic
class Issue implements Comparable<Issue> {

    private final Date date
    private final Service service
    private final String message
    private final Throwable cause
    private final String stackTrace

    /**
     * Constructor from service, message and exception.
     */
    Issue(final Service service, final String message, final Throwable cause) {
        date = new Date()
        this.service = service
        this.message = message
        this.cause = cause
        stackTrace = JexlerUtil.getStackTrace(cause)
    }

    /**
     * Get date and time when the issue occurred.
     */
    Date getDate() {
        return date
    }

    /**
     * Get service where the issue occurred, may be null.
     */
    Service getService() {
        return service
    }

    /**
     * Get message that explains the issue, may be null.
     */
    String getMessage() {
        return message
    }

    /**
     * Get throwable that caused the issue, null if none.
     */
    Throwable getCause() {
        return cause
    }

    /**
     * Get exception stack trace as a multi-line string,
     * empty if could not get it or no causing throwable.
     */
    String getStackTrace() {
        return stackTrace
    }

    /**
     * Comparator, newer date is smaller (first).
     */
    @Override
    int compareTo(final Issue issue) {
        return -date.compareTo(issue.date)
    }

    /**
     * Create a single line string of all members, suitable for logging.
     */
    @Override
    String toString() {
        return """\
            Issue: [message=${message==null ? 'null' : "'${JexlerUtil.toSingleLine(message)}'"}
            ,service=${service == null ? 'null' : "'${service.class.name}:$service.id'"}
            ,cause=${cause == null ? 'null' : "'${JexlerUtil.toSingleLine(cause.toString())}'"}
            ,stackTrace='${JexlerUtil.toSingleLine(stackTrace)}']
        """.stripIndent().replace('\r','').replace('\n','')
    }

}

