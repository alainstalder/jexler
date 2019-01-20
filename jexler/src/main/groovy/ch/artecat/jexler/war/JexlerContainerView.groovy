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

import ch.artecat.jexler.Issue
import ch.artecat.jexler.Jexler
import ch.artecat.jexler.JexlerContainer
import ch.artecat.jexler.JexlerUtil
import ch.artecat.jexler.service.Service

import ch.grengine.except.GrengineException
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.activation.MimetypesFileTypeMap
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.jsp.PageContext
import java.text.SimpleDateFormat

import static ch.artecat.jexler.JexlerUtil.SHUTDOWN_TIMEOUT_MSG

/**
 * Container/jexler view, used in Jexler.jsp.
 *
 * @author Jex Jexler (Alain Stalder)
 */
@CompileStatic
class JexlerContainerView {

    private static final Logger LOG = LoggerFactory.getLogger(Jexler.class)

    // Markers for when processing jexler stop/start/restart
    // Map value is the tooltip to show
    private static Map<String,String> processing = Collections.synchronizedMap([:])

    // JSP/HTTP
    private PageContext pageContext
    private HttpServletRequest request
    private HttpServletResponse response

    // The jexler and jexler ID set after constructing
    // in getJexlers(), but not for "container" in JSP,
    // hence jexler null if container, not null if a jexler,
    // and setting jexlerId to '' for container because
    // need the parameter in forms
    private Jexler jexler
    private String jexlerId = ''

    // The 'jexler' parameter,
    // set when handling request in handleCommands()
    private String targetJexlerId

    // The jexler for the 'jexler' parameter,
    // set when handling request in handleCommands()
    private Jexler targetJexler

    // Constructor
    JexlerContainerView() {
    }

    // Handle commands (based on request parameters)
    String handleCommands(final PageContext pageContext) {

        this.pageContext = pageContext
        request = (HttpServletRequest)pageContext.request
        response = (HttpServletResponse)pageContext.response

        // no caching of jexler status queries etc.
        response.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate')
        response.setHeader('Pragma', 'no-cache')
        response.setHeader('Expires', '0')

        container.refresh()

        // set jexler from request parameter
        targetJexlerId = request.getParameter('jexler')
        if (targetJexlerId == null) {
            targetJexlerId = ''
        }
        if (targetJexlerId != '') {
            targetJexler = container.getJexler(targetJexlerId)
        }

        final String cmd = request.getParameter('cmd')
        if (cmd != null) {
            switch (cmd) {
            case 'start':
                handleStart()
                break
            case 'stop':
                handleStop()
                break
            case 'restart':
                handleRestart()
                break
            case 'zap':
                handleZap()
                break
            case 'save':
                handleSaveAs()
                break
            case 'delete':
                handleDelete()
                break
            case 'forget':
                handleForget()
                break
            case 'forgetall':
                handleForgetAll()
                break
            case 'http':
                handleHttp()
                break
            default:
                // ignore
                break
            }
            // redirect to self to allow reloading page in browser
            if (request.method == 'POST' && cmd != 'http') {
                response.sendRedirect(getAction(targetJexlerId))
            }
        }

        container.refresh()

        return ''
    }

    // Get all jexlers from container
    Map<String,JexlerContainerView> getJexlers() {
        final Map<String,JexlerContainerView> jexlersViews = new LinkedHashMap<>()
        for (final Jexler jexler : container.jexlers) {
            JexlerContainerView view = new JexlerContainerView()
            view.jexler = jexler
            view.jexlerId = jexler.id
            jexlersViews[jexler.id] = view
        }
        return jexlersViews
    }

    // Get jexler ID, needed in JSP
    String getJexlerId() {
        return jexlerId
    }

    // Set jexler ID, needed to set in views for jexlers
    void setJexlerId(final String jexlerId) {
        this.jexlerId = jexlerId
    }

    // Get jexler tooltip
    String getJexlerTooltip() {
        return JexlerContextListener.jexlerTooltip
    }

    // Form action for form in JSP, default is always the current jexler (or the container)
    String getFormAction() {
        return getAction(jexlerId)
    }

    // Get start/stop link with icon for table of jexlers
    String getStartStop() {
        if (jexlerId != '' && !jexler.runnable) {
            return space
        }
        final boolean on
        if (jexlerId == '') {
            on = container.state.on
        } else {
            on = jexler.state.on
        }
        return on ? stop : start
    }

    // Get restart/zap link with icon for table of jexlers
    String getRestartZap() {
        if (jexlerId != '' && !jexler.runnable) {
            return space
        }
        if (jexlerId != '' && jexler.state.on) {
            for (final Issue issue : jexler.issues) {
                if (issue.message == SHUTDOWN_TIMEOUT_MSG) {
                    return zap
                }
            }
        }
        return restart
    }

    // Get form action
    private static String getAction(final String jexlerId) {
        return "?jexler=$jexlerId"
    }

    // Get link for posting form for start/stop/restart/zap buttons
    private String getLink(final String cmdParam, final String imgName, final String title) {
        final String type = cmdParam == null ? 'button' : 'submit'
        // title in image to keep tooltip when dimmed by Javascript (server not reachable),
        // title also in button, otherwise not visible in Firefox...
        return """\
            <button class="img" type="$type" name="cmd" value="$cmdParam" title="$title" formaction="${getAction(jexlerId)}">\
            <img src="${imgName}.gif" title="$title">\
            </button>""".replace('            ', '')
    }

    String getSpace() {
        return "<img src='space.gif' title=''>"
    }

    // Get start link with icon for table of jexlers
    String getStart() {
        final String title = jexlerId == '' ? 'Start all jexlers with autostart set' : 'Start jexler'
        final String cmd = isProcessing(jexlerId) ? null : 'start'
        return getLink(cmd, 'start', title)
    }

    // Get stop link with icon for table of jexlers
    String getStop() {
        final String title = jexlerId == '' ? 'Stop all jexlers' : 'Stop jexler'
        final String cmd = isProcessing(jexlerId) ? null : 'stop'
        return getLink(cmd, 'stop', title)
     }

    // Get restart link with icon for table of jexlers
    String getRestart() {
        final String title = jexlerId == '' ? 'Stop all jexlers, then start all with autostart set' : 'Restart jexler'
        final String cmd = isProcessing(jexlerId) ? null : 'restart'
        return getLink(cmd, 'restart', title)
    }

    // Get zap link with icon for table of jexlers
    String getZap() {
        final String title = jexlerId == '' ? 'Zap all jexlers (unsafe, some threads/resources may remain)'
                : 'Zap jexler (unsafe, some threads/resources may remain)'
        final String cmd = isProcessing(jexlerId) ? null : 'zap'
        return getLink(cmd, 'zap', title)
    }

    // Get link to jexler for table of jexlers
    String getJexlerIdLink() {
        // italic if busy ("running")
        String id = jexlerId
        if (jexler.state.busy) {
            id = "<em>$id</em>"
        }
        final String title
        if (jexler.runnable) {
            title = jexler.state.info
        } else {
            title = 'Available to all jexlers'
        }
        return "<a href='?cmd=info&jexler=$jexlerId'><span title='$title'>$id</span></a>"
    }

    // Get web link and icon for table of jexlers
    String getWeb() {
        if (jexlerId == '') {
            final String img = "<img src='info.gif' title='Go to User Guide in new tab'>"
            return "<a href='https://www.artecat.ch/jexler/guide/' target='_blank'>$img</a>"
        }
        final Script script = jexler.script
        if (script != null && jexler.state.operational) {
            final MetaClass mc = script.metaClass
            final Object[] args = [PageContext.class]
            final MetaMethod mm = mc.getMetaMethod('handleHttp', args)
            if (mm != null) {
                return "<a href='?cmd=http&jexler=$jexlerId'><img src='web.gif' title='Go to web served by jexler script'></a>"
            }

        }
        return "<img src='space.gif'>"
    }

    // Get link and icon for logfile and/or issues
    String getLog() {
        if (jexlerId != '' && !jexler.runnable) {
            return "<img src='neutral.gif' title=''>"
        }
        if (jexlerId == '') {
            if (container.issues.size() == 0) {
                return "<a href='?cmd=log&jexler=$jexlerId'><img src='log.gif' title='Show jexler log'></a>"
            } else {
                return "<a href='?cmd=log&jexler=$jexlerId'><img src='error.gif' title='Show jexler log'></a>"
            }
        } else {
            final String img
            final String processingTitle = processing[jexlerId]
            if (processingTitle != null) {
                img = "<img src='wheel.gif' title='$processingTitle'>"
            } else if (jexler.issues.empty) {
                img = "<img src='ok.gif' title='No Issues'>"
            } else {
                img = "<img src='error.gif' title='Show issues (${jexler.issues.size()})'>"
            }
            if (jexler.issues.size() == 0) {
                return img
            } else {
                return "<a href='?cmd=log&jexler=$jexlerId'>$img</a>"
            }
        }
    }

    // Get issues for jexler or container
    String getIssues() {
        final List<Issue> issues
        if (jexler == null) {
            issues = container.issues
        } else {
            issues = jexler.issues
        }
        if (issues.size() == 0) {
            return''
        }

        final StringBuilder builder = new StringBuilder()
        builder.append("<pre class='issues'>")
        for (final Issue issue : issues) {
            builder.append('\n')
            SimpleDateFormat format = new SimpleDateFormat('EEE dd MMM yyyy HH:mm:ss.SSS')
            builder.append("<strong>Date:      </strong>${format.format(issue.date)}\n")
            builder.append("<strong>Message:   </strong>$issue.message\n")
            final Service service = issue.service
            String s
            if (service == null) {
                s = '-'
            } else {
                s = "${service.class.name}:$service.id"
            }
            builder.append("<strong>Service:   </strong>$s\n")
            final Throwable cause = issue.cause
            s = (cause==null) ? "-" : cause.toString()
            replacements.each { final original, final replacement ->
                s = s.replace(original, replacement)
            }
            builder.append("<strong>Cause: </strong>$s\n")
            s = issue.stackTrace
            if (s != null) {
                replacements.each { final original, final replacement ->
                    s = s.replace(original, replacement)
                }
                builder.append(s.empty ?: "<span class='trace'>$s</span>\n")
            }
        }
        builder.append('</pre>')
        return builder.toString()
    }

    // Get logfile
    String getLogfile() {
        if (jexlerId != '') {
            return ''
        }
        final File logfile = JexlerContextListener.logfile
        final StringBuilder builder = new StringBuilder()
        builder.append("<pre class='log'>\n")
        try {
            String logData = readTextFileReversedLines(logfile)
            logData = logData.replace('<', '&lt')
            builder.append(logData)
        } catch (final IOException e) {
            final String msg = "Could not read logfile '$logfile.absolutePath'."
            container.trackIssue(null, msg, e)
            builder.append(msg)
        }
        builder.append('</pre>\n')
        final String s = builder.toString()
        return s
    }

    // Get script source
    String getSource() {
        if (jexler == null) {
            return ''
        }
        final File file = jexler.file
        if (!file.exists()) {
            return ''
        }
        try {
            return file.text.replace('&', '&amp;')
        } catch (final IOException e) {
            final String msg = "Could not read jexler script file '$file.absolutePath'."
            jexler.trackIssue(null, msg, e)
            return msg
        }
    }

    // Get mime type from file extension
    String getMimeType() {
        if (jexler == null) {
            return 'text/plain'
        }
        final MimetypesFileTypeMap map = new MimetypesFileTypeMap()
        map.addMimeTypes('text/x-groovy groovy')
        final String mimeType = map.getContentType(jexler.file)
        return mimeType
    }

    // Whether editing scripts is allowed by config or not
    String getScriptAllowEdit() {
        return Boolean.toString(JexlerContextListener.scriptAllowEdit)
    }

    // Return disabled attribute if not allowed to edit scripts
    String getDisabledIfReadonly() {
        final boolean allowEdit = JexlerContextListener.scriptAllowEdit
        if (allowEdit) {
            return ''
        } else {
            return " disabled='disabled'"
        }
    }

    // Whether to ask before saving scripts or not according to config
    boolean isConfirmSave() {
        return JexlerContextListener.scriptConfirmSave
    }

    // Whether to ask before deleting scripts or not according to config
    boolean isConfirmDelete() {
        return JexlerContextListener.scriptConfirmDelete
    }

    // Start jexler or container
    private void handleStart() {
        if (targetJexlerId == '') {
            for (final Jexler jexler : container.jexlers) {
                if (jexler.metaConfig?.autostart) {
                    handleStart(jexler)
                }
            }
        } else if (targetJexler != null) {
            handleStart(targetJexler)
        }
    }

    // Stop jexler or container
    private void handleStop() {
        if (targetJexlerId == '') {
            // do this first to get a new grengine instance
            container.stop()
            for (final Jexler jexler : container.jexlers) {
                if (jexler.state.on) {
                    handleStop(jexler)
                }
            }
        } else if (targetJexler != null) {
            handleStop(targetJexler)
        }
    }

    // Restart jexler or container
    private void handleRestart() {
        if (targetJexlerId == '') {
            // do this first to get a new grengine instance
            container.stop()
            for (final Jexler jexler : container.jexlers) {
                if (jexler.metaConfig?.autostart) {
                    handleRestart(jexler)
                } else if (jexler.state.on) {
                    handleStop(jexler)
                }
            }
        } else if (targetJexler != null) {
            handleRestart(targetJexler)
        }
    }

    // Zap jexler or container
    private void handleZap() {
        if (targetJexlerId == '') {
            for (final Jexler jexler : container.jexlers) {
                if (jexler.state.on) {
                    handleZap(jexler)
                }
            }
        } else if (targetJexler != null) {
            handleZap(targetJexler)
        }
    }

    private void setTargetJexlerIdFromJexlerNameParameter() {

        final String newJexlerId = request.getParameter('jexlername')

        // strangely the input field with the jexler name is not always posted
        // (apparently only at save and only for some already existing files)
        if (newJexlerId == null) {
            return
        }

        targetJexlerId = newJexlerId
        targetJexler = container.getJexler(targetJexlerId)

    }

    // Save script
    private void handleSaveAs() {
        if (!JexlerContextListener.scriptAllowEdit) {
            return
        }
        String source = request.getParameter('source')
        setTargetJexlerIdFromJexlerNameParameter()
        if (source != null && targetJexlerId != '') {
            source = source.replace('\r\n', '\n')
            final File file = container.getJexlerFile(targetJexlerId)
            try {
                file.text = source
            } catch (final IOException e) {
                final String msg = "Could not save script file '${file.absolutePath}'"
                if (targetJexler != null) {
                    targetJexler.trackIssue(null, msg, e)
                } else {
                    container.trackIssue(null, msg, e)
                }
                return
            }
            if (targetJexler != null) {
                if (targetJexler.runnable) {
                    // check if can compile jexler
                    targetJexler.forgetIssues()
                    try {
                        container.grengine.load(targetJexler.file)
                    } catch (final Exception e) {
                        targetJexler.trackIssue(targetJexler,
                                'Compiling/loading of updated jexler failed.', e)
                    }
                } else {
                    // check if can update container Grengine
                    container.forgetIssues()
                    final GrengineException lastUpdateException = container.grengine.lastUpdateException
                    if (lastUpdateException != null) {
                        container.trackIssue(container, 'Compiling of updated container sources failed' +
                                ' - previous state of utility classes remains active.', lastUpdateException)
                    }
                }
            }
        }
    }

    // Delete script
    private void handleDelete() {
        if (!JexlerContextListener.scriptAllowEdit) {
            return
        }
        setTargetJexlerIdFromJexlerNameParameter()
        if (targetJexlerId != '') {
            final File file = container.getJexlerFile(targetJexlerId)
            if (!file.delete()) {
                final String msg = "Could not delete script file '${file.absolutePath}'"
                if (targetJexler != null) {
                    targetJexler.trackIssue(null, msg, null)
                } else {
                    container.trackIssue(null, msg, null)
                }
                return
            }
            if (targetJexler != null) {
                if (targetJexler.runnable) {
                    // nothing to compile
                } else {
                    // check if can update container Grengine
                    container.forgetIssues()
                    final GrengineException lastUpdateException = container.grengine.lastUpdateException
                    if (lastUpdateException != null) {
                        container.trackIssue(container, 'Compiling of updated container sources failed' +
                                ' - previous state of utility classes remains active.', lastUpdateException)
                    }
                }
            }
        }
    }

    // Forget issues of jexler or container
    private void handleForget() {
        if (targetJexler != null) {
            targetJexler.forgetIssues()
        } else {
            container.forgetIssues()
        }
    }

    // Forget issues of container and of all jexlers
    private void handleForgetAll() {
        container.forgetIssues()
        for (final Jexler jexler : container.jexlers) {
            jexler.forgetIssues()
        }
    }

    // Handle HTTP event, dispatch to jexler or reply directly in case of errors
    private void handleHttp() {
        if (targetJexlerId == '') {
            sendError(response, 404, 'No jexler indicated via request parameter.', null)
            return
        }
        if (targetJexler == null) {
            sendError(response, 404, "Jexler '$targetJexlerId' not found.", null)
            return
        }

        final Script script = targetJexler.script
        if (script == null || !targetJexler.state.operational) {
            sendError(response, 404, "Jexler '$targetJexlerId' is not operational.", null)
            return
        }

        final MetaClass mc = script.metaClass
        final Object[] args = [ PageContext.class ]

        final MetaMethod mm = mc.getMetaMethod('handleHttp', args)
        if (mm == null) {
            sendError(response, 404, "Jexler '$targetJexlerId' does not handle HTTP requests.", null)
            return
        }

        try {
            mm.invoke(script, [ pageContext ] as Object[])
        } catch (final Throwable t) {
            targetJexler.trackIssue(jexler, "Handler 'handleHttp' failed.", t)
            sendError(response, 500, "Jexler '$targetJexlerId': Handler 'handleHttp' failed.", t)
        }
    }

    // Send error page
    private void sendError(final HttpServletResponse response, final int status, final String msg, final Throwable t) {
        response.status = status
        String stacktrace = ''
        if (t != null) {
            stacktrace = JexlerUtil.getStackTrace(t)
            replacements.each { final original, final replacement ->
                stacktrace = stacktrace.replace(original, replacement)
            }
            stacktrace = "<hr><pre class='log'>$stacktrace</pre><hr>"

        }
        response.writer.println("""\
<html>
  <head>
    <title>Error $status</title>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>
    <link rel="shortcut icon" href="favicon.ico"/>
    <link rel="icon" href="favicon.ico"/>
    <link rel="stylesheet" href="jexler.css"/>
  </head>
  <body>
    <a href="."><img class="jexler" src="jexler.jpg" title="Back to jexler main view"></a>
    <h1 style='color:darkred'>Error $status</h1>
    <p>$msg</p>
    $stacktrace
  </body>
</html>
""")
    }

    // Get one and only container in this webapp
    private static JexlerContainer getContainer() {
        return JexlerContextListener.container
    }

    /**
     * Read log file into string while reversing the order of lines.
     * @param file
     * @return file contents (platform default charset and line separator)
     * @throws IOException if reading failed
     */
    private static String readTextFileReversedLines(final File file) throws IOException {
        final StringBuilder builder = new StringBuilder()
        file.eachLine { final line ->
            builder.insert(0, line + System.lineSeparator())
        }
        return builder.toString()
    }

    // Get map of passages to highlight in stack trace
    private Map<String,String> getReplacements() {
        final Map<String,String> replacements = new LinkedHashMap<String,String>()
        replacements['<'] =  '&lt'
        // start jexler script
        replacements['Jexler.start'] = '<strong>Jexler.start</strong>'
        // jexler script thread
        replacements['Jexler$1.run'] = '<strong>Jexler$1.run</strong>'
        // thread that zaps jexler script thread
        replacements['Jexler$2.run'] = '<strong>Jexler$2.run</strong>'
        // individual jexlers
        for (final Jexler jexler : container.jexlers) {
            final String original = "${jexler.id}.groovy"
            final String replacement = "<strong>$original</strong>"
            replacements[original] = replacement
        }
        return replacements
    }

    // Run given closure in a new thread
    private void runInNewThread(final Closure closure) {
        new Thread() {
            void run() {
                closure.call()
            }
        }.start()
    }

    // Whether is processing given jexler / container
    private static boolean isProcessing(final String jexlerId) {
        if (jexlerId == '') {
            // always allow to stop/start all, must be robust
            return false
        } else {
            return processing[jexlerId] != null
        }
    }

    // Start given jexler
    private void handleStart(final Jexler jexler) {
        if (isProcessing(jexler.id)) {
            return
        }
        processing[jexler.id] = "Starting..."
        jexler.start()
        runInNewThread {
            JexlerUtil.waitForStartup(jexler, JexlerContextListener.startTimeoutSecs * 1000L)
            processing.remove(jexler.id)
        }
    }

    // Stop given jexler
    private void handleStop(final Jexler jexler) {
        if (isProcessing(jexler.id)) {
            return
        }
        processing[jexler.id] = "Stopping..."
        jexler.stop()
        runInNewThread {
            JexlerUtil.waitForShutdown(jexler, JexlerContextListener.stopTimeoutSecs * 1000L)
            processing.remove(jexler.id)
        }
    }

    // Restart given jexler
    private void handleRestart(final Jexler jexler) {
        if (isProcessing(jexler.id)) {
            return
        }
        processing[jexler.id] = "Stopping..."
        jexler.stop()
        runInNewThread {
            if (JexlerUtil.waitForShutdown(jexler, JexlerContextListener.stopTimeoutSecs * 1000L)) {
                processing.remove(jexler.id)
                handleStart(jexler)
            } else {
                processing.remove(jexler.id)
            }
        }
    }

    // Zap given jexler
    private void handleZap(final Jexler jexler) {
        // ignore other things in progress
        processing[jexler.id] = "Zapping..."
        jexler.zap()
        processing.remove(jexler.id)
    }

}
