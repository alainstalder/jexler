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

package net.jexler.service

import net.jexler.Jexler

import groovy.transform.CompileStatic
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

import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService

/**
 * Directory watch service, creates an event when a file
 * in a given directory is created, modified oder deleted.
 *
 * @author $(whois jexler.net)
 */
@CompileStatic
class DirWatchService extends ServiceBase {

    private static final Logger log = LoggerFactory.getLogger(DirWatchService.class)

    private final Jexler jexler
    private File dir
    private List<WatchEvent.Kind> kinds
    private List<WatchEvent.Modifier> modifiers
    private String cron
    private Scheduler scheduler

    private TriggerKey triggerKey
    private WatchService watchService
    private WatchKey watchKey

    /**
     * Constructor.
     * @param jexler the jexler to send events to
     * @param id the id of the service
     */
    DirWatchService(Jexler jexler, String id) {
        super(id)
        this.jexler = jexler
        dir = jexler.dir
        kinds = [ StandardWatchEventKinds.ENTRY_CREATE,
                  StandardWatchEventKinds.ENTRY_MODIFY,
                  StandardWatchEventKinds.ENTRY_DELETE ]
        modifiers = []
        cron = '*/5 * * * * ?'
    }

    /**
     * Set directory to watch.
     * Default if not set is the directory that contains the jexler.
     * @param dir directory to watch
     * @return this (for chaining calls)
     */
    DirWatchService setDir(File dir) {
        this.dir = dir
        return this
    }

    /**
     * Get directory to watch.
     */
    File getDir() {
        return dir
    }

    /**
     * Set kinds of events to watch for.
     * Default is standard events for create, modify and delete.
     * @param kinds
     * @return
     */
    DirWatchService setKinds(List<WatchEvent.Kind> kinds) {
        this.kinds = kinds
        return this
    }

    /**
     * Get kinds of events to watch for.
     */
    List<WatchEvent.Kind> getKinds() {
        return kinds
    }

    /**
     * Set modifiers when watching for events.
     * On Mac OS X, by default the file system seems to be polled
     * every 10 seconds; to reduce this to 2 seconds, pass a modifier
     * com.sun.nio.file.SensitivityWatchEventModifier.HIGH.
     * @param modifiers
     * @return
     */
    DirWatchService setModifiers(List<WatchEvent.Modifier> modifiers) {
        this.modifiers = modifiers
        return this
    }

    /**
     * Get modifiers when watching for events.
     */
    List<WatchEvent.Modifier> getModifiers() {
        return modifiers
    }

    /**
     * Set cron pattern for when to check.
     * Default is every 5 seconds.
     * @return this (for chaining calls)
     */
    DirWatchService setCron(String cron) {
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
    DirWatchService setScheduler(Scheduler scheduler) {
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
        final Path path = dir.toPath()
        try {
            watchService = path.fileSystem.newWatchService()
            watchKey = path.register(watchService,
                    kinds as WatchEvent.Kind[],
                    modifiers as WatchEvent.Modifier[])
        } catch (IOException e) {
            jexler.trackIssue(this,
                    "Could not create watch service or key for directory '$dir.absolutePath'.", e)
            return
        }

        final String uuid = UUID.randomUUID()
        final JobDetail job = JobBuilder.newJob(DirWatchJob.class)
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
        scheduler.unscheduleJob(triggerKey)
        watchKey.cancel()
        try {
            watchService.close()
        } catch (IOException e) {
            log.trace('failed to close watch service', e)
        }
        state = ServiceState.OFF
    }

    @Override
    void zap() {
        if (state.off) {
            return
        }
        state = ServiceState.OFF
        new Thread() {
            void run() {
                try {
                    scheduler.unscheduleJob(triggerKey)
                } catch (Throwable t) {
                    log.trace('failed to unschedule cron job', t)
                }
                try {
                    watchKey.cancel()
                    watchService.close()
                } catch (Throwable t) {
                    log.trace('failed stop watching directory', t)
                }
            }
        }
    }

    /**
     * Internal class, only public because otherwise not called by quartz scheduler.
     */
    static class DirWatchJob implements Job {
        void execute(JobExecutionContext ctx) throws JobExecutionException {
            final DirWatchService service = (DirWatchService)ctx.jobDetail.jobDataMap.service
            final String savedName = Thread.currentThread().name
            Thread.currentThread().name = "$service.jexler.id|$service.id"
            for (WatchEvent watchEvent : service.watchKey.pollEvents()) {
                final Path contextPath = ((Path) watchEvent.context())
                final File file = new File(service.dir, contextPath.toFile().name)
                final WatchEvent.Kind kind = watchEvent.kind()
                log.trace("event $kind '$file.absolutePath'")
                service.jexler.handle(new DirWatchEvent(service, file, kind))
            }
            Thread.currentThread().name = savedName
        }
    }

}
