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

package net.jexler.service

import net.jexler.JexlerUtil

import groovy.transform.CompileStatic
import org.quartz.CronExpression
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.ParseException
import java.text.SimpleDateFormat

import static net.jexler.service.CronService.CRON_NOW
import static net.jexler.service.CronService.CRON_NOW_AND_STOP

/**
 * Service utilities.
 *
 * Includes some static methods that might be useful in Groovy scripts
 * or for writing custom services.
 *
 * @author $(whois jexler.net)
 */
@CompileStatic
class ServiceUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceUtil.class)

    /**
     * Wait until service state is not BUSY_STARTING or timeout.
     * @param service service
     * @param timeout timeout in ms
     * @return true if no timeout, false otherwise
     */
    static boolean waitForStartup(final Service service, final long timeout) {
        final long t0 = System.currentTimeMillis()
        while (true) {
            if (!service.state.busyStarting) {
                return true
            }
            if (System.currentTimeMillis() - t0 >= timeout) {
                return false
            }
            JexlerUtil.waitAtLeast(10)
        }
    }

    /**
     * Wait until service state is not OFF or timeout.
     * @param service service
     * @param timeout timeout in ms
     * @return true if no timeout, false otherwise
     */
    static boolean waitForShutdown(final Service service, final long timeout) {
        final long t0 = System.currentTimeMillis()
        while (true) {
            if (service.state.off) {
                return true
            }
            if (System.currentTimeMillis() - t0 >= timeout) {
                return false
            }
            JexlerUtil.waitAtLeast(10)
        }
    }

    /**
     * Convert to "quartz-style" cron with seconds.
     * <ul>
     * <li>leaves untouched if 'now' or 'now+stop'</li>
     * <li>adds '0' as first item (seconds) if contains 5 items,
     *     i.e. if is an "old-style" cron string with minutes resolution</li>
     * <li>replaces '*' for day-of-month or day-of-week with '?' when needed
     *     by quartz to parse such a cron string...</li>
     * <li>logs the new cron string if was modified above</li>
     * <li>validates the resulting cron string</li>
     * <li>if valid, logs the next date+time when the cron string would fire</li>
     * </ul>
     *
     * @throws IllegalArgumentException if the resulting cron string is not a valid quartz cron string
     */
    static String toQuartzCron(final String cron) throws IllegalArgumentException {
        if (CRON_NOW == cron | CRON_NOW_AND_STOP == cron) {
            return cron
        }
        final List<String> list = cron.trim().split(/\s/) as List<String>
        // add seconds if missing
        if (list.size() == 5) {
            list.add(0, '0') // on every full minute
        }
        // set at least one '?' for day-of-month or day-of-week
        if (list.size() >= 6 && list[5] != '?' && list[3] != '?') {
            if (list[5] == '*') {
                list[5] = '?'
            } else if (list[3] == '*') {
                list[3] = '?'
            }
        }

        final String quartzCron = list.join(' ')
        if (quartzCron != cron) {
            LOG.trace("cron '$cron' => '$quartzCron'")
        }
        CronExpression cronExpression
        try {
            cronExpression = new CronExpression(quartzCron)
        } catch (final ParseException e) {
            throw new IllegalArgumentException("Could not parse cron '$quartzCron': $e.message", e)
        }
        final SimpleDateFormat format = new SimpleDateFormat('EEE dd MMM yyyy HH:mm:ss.SSS')
        final Date nextDate = cronExpression.getNextValidTimeAfter(new Date())
        final String next = nextDate == null ? null : format.format(nextDate)
        LOG.trace("next '$quartzCron' => $next")
        return quartzCron
    }

}
