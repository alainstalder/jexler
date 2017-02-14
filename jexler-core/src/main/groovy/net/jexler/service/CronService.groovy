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

import net.jexler.Jexler

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.Scheduler
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A cron service, creates events at configurable times.
 * Implemented using the quartz library.
 *
 * @author $(whois jexler.net)
 */
@CompileStatic
class CronService extends ServiceBase {

    private static final Logger log = LoggerFactory.getLogger(CronService.class)

    /**
     * Pseudo cron string for a single cron event immediately.
     */
    public static final String CRON_NOW = 'now'

    /**
     * Pseudo cron string for a single cron event immediately,
     * followed by a single stop event.
     */
    public static final String CRON_NOW_AND_STOP = "$CRON_NOW+stop"

    private final Jexler jexler
    private String cron
    private Scheduler scheduler
    private TriggerKey triggerKey

    /**
     * Constructor.
     * @param jexler the jexler to send events to
     * @param id the id of the service
     */
    CronService(Jexler jexler, String id) {
        super(id)
        this.jexler = jexler
    }

    /**
     * Set cron pattern, e.g. "* * * * *" or with seconds "0 * * * * *".
     * Use "now" for now, i.e. for a single event immediately,
     * or "now+stop" for a single event immediately, followed
     * by a StopEvent, which can both be useful for testing.
     * @return this (for chaining calls)
     */
    CronService setCron(String cron) {
        this.cron = ServiceUtil.toQuartzCron(cron)
        return this
    }

    /**
     * Get cron pattern.
     */
    String getCron() {
        return cron
    }

    /**
     * Set quartz scheduler.
     * Default is a scheduler shared by all jexlers in the same jexler container.
     * @return this (for chaining calls)
     */
    CronService setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler
        return this
    }

    /**
     * Get quartz scheduler.
     */
    Scheduler getScheduler() {
        return scheduler
    }

    /**
     * Get jexler.
     */
    Jexler getJexler() {
        return jexler
    }

    @Override
    void start() {
        if (state.on) {
            return
        }
        if (cron.startsWith(CRON_NOW)) {
            log.trace("new cron event: $cron")
            jexler.handle(new CronEvent(this, cron))
            state = ServiceState.IDLE
            if (cron.equals(CRON_NOW_AND_STOP)) {
                jexler.handle(new StopEvent(this))
                state = ServiceState.OFF
            }
            return
        }

        final String uuid = UUID.randomUUID()
        final JobDetail job = JobBuilder.newJob(CronJob.class)
                .withIdentity("job-$id-$uuid", jexler.id)
                .usingJobData(['service':this] as JobDataMap)
                .build()
        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-$id-$uuid", jexler.id)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .startNow()
                .build()
        triggerKey = trigger.key

        if (scheduler == null) {
            scheduler = jexler.container.scheduler
        }
        scheduler.scheduleJob(job, trigger)
        state = ServiceState.IDLE
    }

    @Override
    void stop() {
        if (state.off) {
            return
        }
        if (scheduler != null) {
            scheduler.unscheduleJob(triggerKey)
        }
        state = ServiceState.OFF
    }

    @Override
    void zap() {
        if (state.off) {
            return
        }
        state = ServiceState.OFF
        if (scheduler != null) {
            new Thread() {
                void run() {
                    try {
                        scheduler.unscheduleJob(triggerKey)
                    } catch (Throwable t) {
                        log.trace('failed to unschedule cron job', t)
                    }
                }
            }.start()
        }
    }

    /**
     * Internal class, only public because otherwise not called by quartz scheduler.
     */
    static class CronJob implements Job {
        void execute(JobExecutionContext ctx) throws JobExecutionException {
            final CronService service = (CronService)ctx.jobDetail.jobDataMap.service
            final String savedName = Thread.currentThread().name
            Thread.currentThread().name = "$service.jexler.id|$service.id"
            log.trace("new cron event: $service.cron")
            service.jexler.handle(new CronEvent(service, service.cron))
            Thread.currentThread().name = savedName
        }
    }

}
