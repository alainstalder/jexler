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

import net.jexler.service.ServiceState
import net.jexler.service.ServiceUtil

import groovy.transform.CompileStatic

/**
 * Jexler utilities.
 * Includes some static methods that might be useful in Groovy scripts
 * or in Java (for writing custom services or tools).
 *
 * @author $(whois jexler.net)
 */
@CompileStatic
class JexlerUtil {

    /**
     * TODO document
     * @param timeout
     * @return
     */
    static boolean waitForStartup(Jexler jexler, long timeout) {
        boolean ok = ServiceUtil.waitForStartup(jexler, timeout)
        if (!ok) {
            jexler.trackIssue(jexler, 'Timeout waiting for jexler startup.', null)
        }
        return ok
    }


    /**
     * TODO document
     * @param timeout
     * @return
     */
    static boolean waitForShutdown(Jexler jexler, long timeout) {
        boolean ok = ServiceUtil.waitForShutdown(jexler, timeout)
        if (!ok) {
            jexler.trackIssue(jexler, 'Timeout waiting for jexler shutdown.', null)
        }
        return ok
    }

    /**
     * TODO document
     * @param timeout
     * @return
     */
    static boolean waitForStartup(JexlerContainer container, long timeout) {
        boolean ok = ServiceUtil.waitForStartup(container, timeout)
        if (!ok) {
            for (Jexler jexler : container.jexlers) {
                if (jexler.state == ServiceState.BUSY_STARTING) {
                    container.trackIssue(jexler, 'Timeout waiting for jexler startup.', null)
                }
            }
        }
        return ok
    }

    /**
     * TODO document
     * @param timeout
     * @return
     */
    static boolean waitForShutdown(JexlerContainer container, long timeout) {
        boolean ok = ServiceUtil.waitForShutdown(container, timeout)
        if (!ok) {
            for (Jexler jexler : container.jexlers) {
                if (jexler.state != ServiceState.OFF) {
                    container.trackIssue(jexler, 'Timeout waiting for jexler shutdown.', null)
                }
            }
        }
        return ok
    }

    /**
     * Get stack trace for given throwable as a string.
     * @return stack trace, never null, empty if throwable is null or could not obtain
     */
    static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return ''
        }
        try {
            Writer result = new StringWriter()
            throwable.printStackTrace(new PrintWriter(result))
            return result
        } catch (RuntimeException ignore) {
            return ''
        }
    }
    
    /**
     * Replace line breaks in string with '%n'.
     * Replaces CRLF, CR, LF with '%n', in that order.
     * return string with replacements, null if given string is null
     */
    static String toSingleLine(String multi) {
        return multi?.replace('\r\n', '%n')?.replace('\r', '%n')?.replace('\n', '%n')
    }

    /**
     * Wait at least for the indicated time in milliseconds.
     * @param ms time to wait in ms
     */
    static void waitAtLeast(long ms) {
        long t0 = System.currentTimeMillis()
        while (true) {
            long t1 = System.currentTimeMillis()
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
