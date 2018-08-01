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

import net.jexler.service.Event
import net.jexler.service.Service
import net.jexler.service.ServiceGroup
import net.jexler.service.ServiceState
import net.jexler.service.StopEvent

import ch.grengine.Grengine
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.LinkedBlockingQueue
import java.util.regex.Matcher
import java.util.regex.Pattern

import static net.jexler.service.ServiceState.BUSY_EVENT
import static net.jexler.service.ServiceState.BUSY_STARTING
import static net.jexler.service.ServiceState.BUSY_STOPPING
import static net.jexler.service.ServiceState.IDLE
import static net.jexler.service.ServiceState.OFF

/**
 * Jexler, runs a Groovy script that handles events.
 *
 * @author $(whois jexler.net)
 */
@CompileStatic
class Jexler implements Service, IssueTracker {

    private static final Logger LOG = LoggerFactory.getLogger(Jexler.class)

    private static final Grengine META_CONFIG_GRENGINE = new Grengine()

    private static final Pattern META_CONFIG_PATTERN = Pattern.compile(
            /^\/\/\s*jexler\s*\{\s*(.*?)\s*}$/, Pattern.CASE_INSENSITIVE)

    /**
     * Blocking queue for events sent to a Jexler
     * ('events' variable in jexler scripts).
     *
     * Typically events are taken with {@link Events#take()}
     * in an event loop in the jexler script.
     *
     * @author $(whois jexler.net)
     */
    @CompileStatic
    class Events extends LinkedBlockingQueue<Event> {
        /**
         * Take event from queue (blocks).
         */
        @Override
        Event take() {
            state = IDLE
            while (true) {
                try {
                    final Event event = (Event)super.take()
                    state = BUSY_EVENT
                    return event
                } catch (InterruptedException e) {
                    trackIssue(Jexler.this, 'Could not take event.', e)
                }
            }
        }

        /**
         * Return true if there is a next event in the queue
         * and it is a stop event.
         */
        boolean nextIsStop() {
            return peek() instanceof StopEvent
        }

        /**
         * Return true if the event queue contains a stop event.
         */
        boolean hasStop() {
            final Object[] events = toArray()
            for (final Object event : events) {
                if (event instanceof StopEvent) {
                    return true
                }
            }
            return false
        }

    }

    private final File file
    private final String id
    private final JexlerContainer container
    private volatile ServiceState state
    private volatile Script script

    private volatile Thread scriptThread

    /** Event queue. */
    protected final Events events

    /**
     * Group of services.
     * Scripts are free to add services to this list or not - if they do,
     * services are automatically stopped by jexler after the script exits
     * (regularly or throws).
     */
    private final ServiceGroup services

    private final IssueTracker issueTracker

    private ConfigObject metaConfigAtStart

    /**
     * Constructor.
     * @param file file with jexler script
     * @param container jexler container that contains this jexler
     */
    Jexler(final File file, final JexlerContainer container) {
        this.file = file
        this.container = container
        id = container.getJexlerId(file)
        state = OFF
        events = new Events()
        services = new ServiceGroup("${id}.services")
        issueTracker = new IssueTrackerBase()
    }

    /**
     * Initiate jexler start.
     * Immediately marks the jexler service as starting up, then tries to
     * start the script.
     * Typically returns before the jexler script has started or completed
     * to initialize all of its services.
     * The jexler remains in the running state until the script exits in
     * any way, after it has been tried to stop all registered services.
     */
    @Override
    void start() {
        LOG.info("*** Jexler start: $id")
        if (state.on) {
            return
        }

        state = BUSY_STARTING
        forgetIssues()
        final Jexler jexler = this

        // compile, load, create and run in a separate thread
        scriptThread = new Thread(
                new Runnable() {
                    void run() {

                        // read meta config
                        metaConfigAtStart = readMetaConfig()
                        final boolean runnable = metaConfigAtStart != null
                        if (!runnable || !issues.empty) {
                            state = OFF
                            return
                        }

                        // define script binding
                        final Binding binding = new Binding([
                                'jexler' : jexler,
                                'container' : container,
                                'events' : events,
                                'services' : services,
                                'log' : LOG,
                        ])

                        // compile and load class
                        final Class clazz
                        try {
                            clazz = container.grengine.load(file)
                        } catch (final Throwable tCompile) {
                            // (may throw almost anything, checked or not)
                            trackIssue(jexler, 'Script compile failed.', tCompile)
                            state = OFF
                            return
                        }

                        // not a runnable script?
                        if (!Script.class.isAssignableFrom(clazz)) {
                            state = OFF
                            return
                        }

                        // create script instance
                        try {
                            script = (Script)clazz.newInstance()
                        } catch (final Throwable tCreate) {
                            // (may throw anything, checked or not)
                            trackIssue(jexler, 'Script create failed.', tCreate)
                            state = OFF
                            return
                        }

                        // run script
                        script.binding = binding
                        try {
                            script.run()
                        } catch (final Throwable tRun) {
                            // (script may throw anything, checked or not)
                            trackIssue(jexler, 'Script run failed.', tRun)
                        }

                        state = BUSY_STOPPING

                        try {
                            services.stop()
                        } catch (final Throwable tStop) {
                            trackIssue(services, 'Could not stop services.', tStop)
                        }
                        events.clear()
                        services.services.clear()

                        script = null
                        state = OFF
                    }
                })
        scriptThread.daemon = true
        scriptThread.name = id
        scriptThread.start()
    }

    /**
     * Handle given event.
     */
    void handle(final Event event) {
        events.add(event)
    }

    /**
     * Initiate jexler stop by sending it a stop event to handle.
     */
    @Override
    void stop() {
        LOG.info("*** Jexler stop: $id")
        if (state.off) {
            return
        }
        handle(new StopEvent(this))
    }

    @Override
    ServiceState getState() {
        return state
    }

    @Override
    void zap() {
        LOG.info("*** Jexler zap: $id")
        if (state.off) {
            return
        }
        state = OFF
        final ServiceGroup services = this.services
        final Thread scriptThread = this.scriptThread
        final Jexler jexler = this
        new Thread() {
            void run() {
                if (services != null) {
                    services.zap()
                }
                if (scriptThread != null) {
                    try {
                        scriptThread.stop()
                    } catch (final Throwable tZap) {
                        trackIssue(jexler, 'Failed to stop jexler thread.', tZap)
                    }
                }
            }
        }.start()
    }

    @Override
    String getId() {
        return id
    }

    @Override
    void trackIssue(final Issue issue) {
        issueTracker.trackIssue(issue)
    }

    @Override
    void trackIssue(final Service service, final String message, final Throwable cause) {
        issueTracker.trackIssue(service, message, cause)
    }

    @Override
    List<Issue> getIssues() {
        return issueTracker.issues
    }

    @Override
    void forgetIssues() {
        issueTracker.forgetIssues()
    }

    /**
     * Get jexler script file.
     */
    File getFile() {
        return file
    }

    /**
     * Get directory that contains script file.
     */
    File getDir() {
        return file.parentFile
    }

    /**
     * Get container that contains this jexler.
     */
    JexlerContainer getContainer() {
        return container
    }

    /**
     * Get jexler script instance, null if script is not running.
     */
    Script getScript() {
        return script
    }

    /**
     * Convenience method for getting ConfigSlurper config from parsing
     * this jexler; uses the class already compiled by Grengine.
     */
    ConfigObject getAsConfig() {
        return new ConfigSlurper().parse(container.grengine.load(file))
    }

    /**
     * Return true if jexler is indicated as a jexler in the first
     * line of its script text (meta config); more precisely returns
     * the state at startup if already running, else reads from file.
     */
    boolean isRunnable() {
        return metaConfig != null
    }

    /**
     * Get meta config.
     *
     * Read from the jexler file at each call except if the jexler
     * is already running, in that case returns meta config read at
     * the time the jexler was started.
     *
     * The meta config of a jexler is stored in the first line of
     * a jexler script file.
     *
     * Example:
     * <pre>
     * // Jexler { autostart = true; some = 'thing' }
     * </pre>
     *
     * Returns null if there is no meta config in the jexler or the
     * file could not be read; returns an empty config object if
     * config is present but could not be parsed.
     */
    ConfigObject getMetaConfig() {
        if (state.on) {
            return metaConfigAtStart
        } else {
            return readMetaConfig()
        }
    }

    private ConfigObject readMetaConfig() {
        if (!file.exists()) {
            return null
        }

        final List<String> lines
        try {
            lines = file.readLines()
        } catch (final IOException eRead) {
            final String msg = "Could not read meta config from jexler file '$file.absolutePath'."
            trackIssue(this.container, msg, eRead)
            return null
        }
        if (lines.empty) {
            return null
        }

        final String line = lines.first().trim()
        final Matcher matcher = META_CONFIG_PATTERN.matcher(line)
        if (!matcher.matches()) {
            return null
        }
        final String metaConfigText = matcher.group(1)

        // Using Grengine to automatically compile only once per unique
        // meta config text
        try {
            final Script script = META_CONFIG_GRENGINE.create(metaConfigText)
            return new ConfigSlurper().parse(script)
        } catch (final Throwable tParse) {
            // (script may throw anything, checked or not)
            final String msg = "Could not parse meta config of jexler '$id'."
            trackIssue(this, msg, tParse)
            return new ConfigObject()
        }
    }

}
