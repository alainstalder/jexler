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


import ch.grengine.Grengine
import ch.grengine.code.CompilerFactory
import ch.grengine.code.groovy.DefaultGroovyCompiler
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory
import ch.grengine.engine.LayeredEngine
import ch.grengine.except.GrengineException
import ch.grengine.load.DefaultTopCodeCacheFactory
import ch.grengine.load.LoadMode
import ch.grengine.source.DefaultSourceFactory
import ch.grengine.sources.Sources
import ch.grengine.jexler.service.Service
import ch.grengine.jexler.service.ServiceGroup
import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.quartz.Scheduler
import org.quartz.impl.DirectSchedulerFactory
import org.quartz.simpl.RAMJobStore
import org.quartz.simpl.SimpleThreadPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Container of all jexlers in a directory.
 *
 * @author Alain Stalder
 */
@CompileStatic
class JexlerContainer extends ServiceGroup implements Service, IssueTracker, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(JexlerContainer.class)

    private static final String EXT = '.groovy'

    private final File dir

    /** Map of jexler ID to jexler. */
    private final Map<String,Jexler> jexlerMap

    private final IssueTracker issueTracker

    private Scheduler scheduler
    private final Object schedulerLock

    private Grengine grengine

    /**
     * Constructor from jexler script directory.
     * @param dir directory which contains jexler scripts
     * @throws RuntimeException if given dir is not a directory or does not exist
     */
    JexlerContainer(final File dir) {
        // service ID is directory name
        super(dir.exists() ? dir.name : null)
        if (!dir.exists()) {
            throw new RuntimeException("Directory '$dir.absolutePath' does not exist.")
        } else  if (!dir.isDirectory()) {
            throw new RuntimeException("File '$dir.absolutePath' is not a directory.")
        }
        this.dir = dir
        jexlerMap = new TreeMap<>()
        issueTracker = new IssueTrackerBase()
        schedulerLock = new Object()
        // calls also refresh() via JexlerContainerSources
        grengine = createGrengine()
    }

    /**
     * Refresh list of jexlers.
     * Add new jexlers for new script files;
     * remove old jexlers if their script file is gone and they are stopped.
     */
    void refresh() {
        synchronized (jexlerMap) {
            // list directory and create jexlers in map for new script files in directory
            dir.listFiles()?.each { final File file ->
                if (file.isFile() && !file.isHidden()) {
                    final String id = getJexlerId(file)
                    if (id != null && !jexlerMap.containsKey(id)) {
                        final Jexler jexler = new Jexler(file, this)
                        jexlerMap[jexler.id] = jexler
                    }
                }
            }

            // recreate list while omitting jexlers without script file that are stopped
            services.clear()
            jexlerMap.each { final id, final jexler ->
                if (jexler.file.exists() || jexler.state.on) {
                    services.add(jexler)
                }
            }

            // recreate map with list entries
            jexlerMap.clear()
            for (final Jexler jexler : jexlers) {
                jexlerMap[jexler.id] = jexler
            }
        }
    }

    /**
     * Start jexlers that are marked as autostart.
     */
    @Override
    void start() {
        for (final Jexler jexler : jexlers) {
            if (jexler.metaConfig?.autostart) {
                jexler.start()
            }
        }
    }

    @Override
    void stop() {
        super.stop()
        // replace grengine to clean up possibly accumulated grapes
        grengine = createGrengine()
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
     * Get the list of all jexlers, first runnable jexlers,
     * then non-runnable ones, each group sorted by id.
     *
     * This is a copy, iterating over it can be freely done
     * and trying to add or remove list elements throws
     * an UnsupportedOperationException.
     */
    List<Jexler> getJexlers() {
        final List<Jexler> jexlers = new LinkedList<>()
        final List<Jexler> nonRunnables = new LinkedList<>()
        synchronized(jexlerMap) {
            for (final Service service : services) {
                Jexler jexler = (Jexler)service
                if (jexler.runnable) {
                    jexlers.add(jexler)
                } else {
                    nonRunnables.add(jexler)
                }
            }
            jexlers.addAll(nonRunnables)
        }
        return Collections.unmodifiableList(jexlers)
    }

    /**
     * Get the jexler for the given id.
     * @return jexler for given id or null if none
     */
    Jexler getJexler(final String id) {
        synchronized(jexlerMap) {
            return jexlerMap[id]
        }
    }

    /**
     * Get container directory.
     */
    File getDir() {
        return dir
    }

    /**
     * Get the file for the given jexler id,
     * even if no such file exists (yet).
     */
    File getJexlerFile(final String id) {
        return new File(dir, "$id$EXT")
    }

    /**
     * Get the jexler id for the given file,
     * even if the file does not exist (any more),
     * or null if not a jexler script.
     */
    String getJexlerId(final File jexlerFile) {
        final String name = jexlerFile.name
        if (name.endsWith(EXT)) {
            return name.substring(0, name.length() - EXT.length())
        } else {
            return null
        }
    }

    /**
     * Get shared quartz scheduler, already started.
     */
    Scheduler getScheduler() {
        synchronized (schedulerLock) {
            if (scheduler == null) {
                final String uuid = UUID.randomUUID()
                final String name = "JexlerContainerScheduler-$id-$uuid"
                final String instanceId = name
                DirectSchedulerFactory.instance.createScheduler(name, instanceId,
                        new SimpleThreadPool(5, Thread.currentThread().priority), new RAMJobStore())
                scheduler = DirectSchedulerFactory.instance.getScheduler(name)
                scheduler.start()
            }
            return scheduler
        }
    }

    /**
     * Stop the shared quartz scheduler, plus close maybe other things.
     */
    void close() {
        synchronized (schedulerLock) {
            if (scheduler != null) {
                scheduler.shutdown()
                scheduler = null
            }
        }
    }

    /**
     * Get logger for container.
     */
    static Logger getLogger() {
        return LOG
    }

    /**
     * Convenience method for getting ConfigSlurper config from parsing
     * the given jexler; uses the class already compiled by Grengine.
     */
    ConfigObject getAsConfig(final String jexlerId) {
        final Class clazz = grengine.load(new File(dir, "${jexlerId}.groovy"))
        return new ConfigSlurper().parse(clazz)
    }

    /**
     * Get container Grengine instance.
     */
    Grengine getGrengine() {
        return grengine
    }

    private Grengine createGrengine() {

        // setting most things explicitly even if would be default value anyway

        // for Grape to work, a GroovyClassLoader must be a parent loader
        final GroovyClassLoader runtimeLoader = new GroovyClassLoader()
        Grengine.Grape.activate()
        //System.setProperty('groovy.grape.report.downloads', 'true')
        //System.setProperty('ivy.message.logger.level', '4')

        final CompilerConfiguration config = new CompilerConfiguration().with {
            optimizationOptions.put(INVOKEDYNAMIC, true)
            targetBytecode = JDK8
            addCompilationCustomizers(new ImportCustomizer().with {
                addStarImports('ch.grengine.jexler', 'ch.grengine.jexler.service', 'ch.grengine.jexler.tool')
            })
        }
        DefaultGroovyCompiler.withGrape(config, runtimeLoader)

        final CompilerFactory theCompilerFactory = new DefaultGroovyCompilerFactory(config)

        final Grengine gren = new Grengine.Builder().with {
            sourcesLayers = [(Sources)new JexlerContainerSources.Builder(this).with {
                        compilerFactory = theCompilerFactory
                        sourceFactory = new DefaultSourceFactory()
                        latencyMs = 800
                        build()
                    }]
            latencyMs = 800
            engine = new LayeredEngine.Builder().with {
                parent = runtimeLoader
                allowSameClassNamesInMultipleCodeLayers = false
                allowSameClassNamesInParentAndCodeLayers = true
                withTopCodeCache = true
                topLoadMode = LoadMode.PARENT_FIRST
                topCodeCacheFactory = new DefaultTopCodeCacheFactory.Builder().with {
                    compilerFactory = theCompilerFactory
                    build()
                }
                build()
            }
            build()
        }

        final GrengineException lastUpdateException = gren.lastUpdateException
        if (lastUpdateException != null) {
            trackIssue(this, 'Compiling container sources failed at startup' +
                    ' - utility classes are not available to jexlers.', lastUpdateException)
        }

        return gren
    }

}
