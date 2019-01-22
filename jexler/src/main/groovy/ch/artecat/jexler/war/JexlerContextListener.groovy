/*
   Copyright 2012-now  Jex Jexler (Alain Stalder)

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

package ch.artecat.jexler.war

import ch.artecat.jexler.Jexler
import ch.artecat.jexler.JexlerContainer
import ch.artecat.jexler.JexlerUtil

import ch.artecat.grengine.Grengine
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.FileAppender
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.ServletContext
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener

/**
 * Jexler context listener.
 *
 * @author Jex Jexler (Alain Stalder)
 */
@CompileStatic
class JexlerContextListener implements ServletContextListener    {

    private static final Logger LOG = LoggerFactory.getLogger(JexlerContextListener.class)

    public static final String GUI_VERSION = '4.0.1' // IMPORTANT: keep in sync with version in main build.gradle

    // Jexler tooltip with versions
    static String jexlerTooltip

    // Servlet context
    static ServletContext servletContext

    // Slurped settings
    static Map<String,Object> settings

    // The one and only jexler container in this webapp
    static JexlerContainer container

    // The logfile (by default one configured with level trace)
    static File logfile

    // Config items from web.xml
    static long startTimeoutSecs
    static long stopTimeoutSecs
    static boolean scriptAllowEdit
    static boolean scriptConfirmSave
    static boolean scriptConfirmDelete

    @Override
    void contextInitialized(final ServletContextEvent event) {

        // Get and log versions (no versions in unit tests or IDE)
        String coreVersion = Jexler.class.package.implementationVersion
        coreVersion = (coreVersion == null) ? '0.0.0' : coreVersion
        String grengineVersion = Grengine.class.package.implementationVersion
        grengineVersion = (grengineVersion == null) ? '0.0.0' : grengineVersion
        String groovyVersion = GroovyClassLoader.class.package.implementationVersion
        groovyVersion = (groovyVersion == null) ? '0.0.0' : groovyVersion
        LOG.info("Welcome to jexler.")
        LOG.info("Jexler $GUI_VERSION | jexler-core: $coreVersion | Grengine: $grengineVersion | Groovy: $groovyVersion")

        // Assemble jexler tooltip
        jexlerTooltip = """\
            Jexler $GUI_VERSION
            • jexler-core: $coreVersion
            • Grengine: $grengineVersion
            • Groovy: $groovyVersion""".stripIndent()

        // Set servlet context
        servletContext = event.servletContext
        final String webappPath = servletContext.getRealPath('/')

        // Get settings from files

        final File settingsFile = new File(webappPath, 'WEB-INF/settings.groovy')
        settings = new ConfigSlurper('').parse(settingsFile.toURI().toURL()).flatten()
        final File settingsCustomFile = new File(webappPath, 'WEB-INF/settings-custom.groovy')
        settings.putAll(new ConfigSlurper('').parse(settingsCustomFile.toURI().toURL()).flatten())
        LOG.trace("settings: ${JexlerUtil.toSingleLine(settings.toString())}")

        startTimeoutSecs = (Long)settings.'operation.jexler.startTimeoutSecs'
        LOG.trace("jexler start timeout: $startTimeoutSecs secs")
        stopTimeoutSecs = (Long)settings.'operation.jexler.stopTimeoutSecs'
        LOG.trace("jexler stop timeout: $stopTimeoutSecs secs")

        scriptAllowEdit = (Boolean)settings.'security.script.allowEdit'
        LOG.trace("allow to edit jexler scripts: $scriptAllowEdit")

        scriptConfirmSave = (Boolean)settings.'safety.script.confirmSave'
        LOG.trace("confirm jexler script save: $scriptConfirmSave")
        scriptConfirmDelete = (Boolean)settings.'safety.script.confirmDelete'
        LOG.trace("confirm jexler script delete: $scriptConfirmDelete")

        // Determine and set log file
        logfile = null
        final LoggerContext context = (LoggerContext)LoggerFactory.ILoggerFactory
        for (final Logger logger : context.loggerList) {
            if (logger instanceof ch.qos.logback.classic.Logger) {
                ch.qos.logback.classic.Logger classicLogger = (ch.qos.logback.classic.Logger)logger
                classicLogger.iteratorForAppenders().each() { final appender ->
                    if (appender instanceof FileAppender) {
                        logfile = new File(((FileAppender)appender).file)
                    }
                }
            }
        }
        LOG.trace("logfile: '$logfile.absolutePath'")

        // Set and start container
        container = new JexlerContainer(new File(webappPath, 'WEB-INF/jexlers'))
        container.start()
    }

    @Override
    void contextDestroyed(final ServletContextEvent event) {
        // Stop and close container
        container.stop()
        container.close()
        LOG.info('Jexler done.')
    }

}
