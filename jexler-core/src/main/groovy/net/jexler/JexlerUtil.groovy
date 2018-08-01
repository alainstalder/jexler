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

import net.jexler.service.ServiceUtil

import groovy.transform.CompileStatic

import static net.jexler.service.ServiceState.BUSY_STARTING
import static net.jexler.service.ServiceState.OFF

/**
 * Jexler utilities.
 * Includes some static methods that could be useful in Groovy scripts
 * or in Java (for writing custom services or tools).
 *
 * @author $(whois jexler.net)
 */
@CompileStatic
class JexlerUtil {

    public static final String STARTUP_TIMEOUT_MSG = 'Timeout waiting for jexler startup.'
    public static final String SHUTDOWN_TIMEOUT_MSG = 'Timeout waiting for jexler shutdown.'

    /**
     * Wait for jexler startup and report issue if did not start in time.
     * @return true if started within timeout
     */
    static boolean waitForStartup(final Jexler jexler, final long timeout) {
        if (ServiceUtil.waitForStartup(jexler, timeout)) {
            return true
        }
        jexler.trackIssue(jexler, STARTUP_TIMEOUT_MSG, null)
        return false
    }


    /**
     * Wait for jexler shutdown and report issue if did not shut down in time.
     * @return true if shut down within timeout
     */
    static boolean waitForShutdown(final Jexler jexler, final long timeout) {
        if (ServiceUtil.waitForShutdown(jexler, timeout)) {
            return true
        }
        jexler.trackIssue(jexler, SHUTDOWN_TIMEOUT_MSG, null)
        return false
    }

    /**
     * Wait for container startup and report an issue for each
     * jexler that did not start in time.
     * @return true if all jexlers started within timeout
     */
    static boolean waitForStartup(final JexlerContainer container, final long timeout) {
        if (ServiceUtil.waitForStartup(container, timeout)) {
            return true
        }
        for (final Jexler jexler : container.jexlers) {
            if (jexler.state == BUSY_STARTING) {
                jexler.trackIssue(jexler, STARTUP_TIMEOUT_MSG, null)
            }
        }
        return false
    }

    /**
     * Wait for container shutdown and report an issue for each
     * jexler that did not shut down in time.
     * @return true if all jexlers shut down within timeout
     */
    static boolean waitForShutdown(final JexlerContainer container, final long timeout) {
        if (ServiceUtil.waitForShutdown(container, timeout)) {
            return true
        }
        for (final Jexler jexler : container.jexlers) {
            if (jexler.state != OFF) {
                jexler.trackIssue(jexler, SHUTDOWN_TIMEOUT_MSG, null)
            }
        }
        return false
    }

    /**
     * Get stack trace for given throwable as a string.
     * @return stack trace, never null, empty if throwable is null or could not obtain
     */
    static String getStackTrace(final Throwable throwable) {
        if (throwable == null) {
            return ''
        }
        try {
            final Writer result = new StringWriter()
            throwable.printStackTrace(new PrintWriter(result))
            return result
        } catch (final RuntimeException ignore) {
            return ''
        }
    }
    
    /**
     * Replace line breaks in string with '%n'.
     * Replaces CRLF, CR, LF with '%n', in that order.
     * return string with replacements, null if given string is null
     */
    static String toSingleLine(final String multi) {
        return multi?.replace('\r\n', '%n')?.replace('\r', '%n')?.replace('\n', '%n')
    }

    /**
     * Wait at least for the indicated time in milliseconds.
     * @param ms time to wait in ms
     */
    static void waitAtLeast(final long ms) {
        final long t0 = System.currentTimeMillis()
        while (true) {
            final long t1 = System.currentTimeMillis()
            if (t1-t0 >= ms) {
                return
            }
            try {
                Thread.sleep(ms - (t1-t0))
            } catch (InterruptedException ignored) {
            }
        }
    }

}
