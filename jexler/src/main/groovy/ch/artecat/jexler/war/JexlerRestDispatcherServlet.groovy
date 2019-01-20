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

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Servlet that dispatches HTTP requests to a jexler indicated by a
 * configurable part of the HTTP request, by default a header with
 * name 'jexler; intended rather for handling REST calls than for
 * direct use via web browser.
 *
 * The jexler must be operational and provide a service() method with
 * the same signature as in a HttpServlet. This method is then called
 * outside of the jexler event queue, possibly in several threads.
 *
 * In case of errors, the corresponding status code is returned, with
 * no response body by default (except if a jexler to handler was found,
 * called and then threw while having already committed at least part
 * of the response).
 *
 * @author Jex Jexler (Alain Stalder)
 */
@CompileStatic
class JexlerRestDispatcherServlet extends HttpServlet    {

    private static final Logger LOG = LoggerFactory.getLogger(JexlerRestDispatcherServlet.class)

    // Script class for getting jexler ID from HTTP request.
    private Class<Script> idGetterClass

    // Script class for sending error HTTP response.
    private Class<Script> errorSenderClass

    @Override
    void init() throws ServletException {
        super.init()
        final String getterSource = JexlerContextListener.settings.'rest.idGetter'
        idGetterClass = new GroovyClassLoader().parseClass(getterSource)
        final String errorSenderSource = JexlerContextListener.settings.'rest.errorSender'
        errorSenderClass = new GroovyClassLoader().parseClass(errorSenderSource)
    }

    /**
     * Dispatch to jexler indicated by header 'Jexler'.
     */
    @Override
    protected void service(final HttpServletRequest httpReq, final HttpServletResponse httpResp)
            throws ServletException, IOException {

        // Check for jexler ID in request
        final String jexlerId = getJexlerId(httpReq)
        if (jexlerId == null) {
            // error already logged in script
            sendError(httpReq, httpResp, 400)
            return
        }

        // Any jexler for this ID?
        final Jexler jexler = JexlerContextListener.container.getJexler(jexlerId)
        if (jexler == null) {
            LOG.error("No jexler '$jexlerId'.")
            sendError(httpReq, httpResp, 404)
            return
        }

        // Jexler operational?
        final Script script = jexler.script
        if (script == null || !jexler.state.operational) {
            LOG.error("Jexler '$jexlerId' not operational.")
            sendError(httpReq, httpResp, 404)
            return
        }

        // Jexler has a service() method?
        final MetaClass mc = script.metaClass
        final Object[] args = [ HttpServletRequest.class, HttpServletResponse.class ]
        final MetaMethod mm = mc.getMetaMethod('service', args)
        if (mm == null) {
            jexler.trackIssue(jexler, "No 'service(httpReq,httpResp)' method for handling HTTP request.", null)
            sendError(httpReq, httpResp, 500)
            return
        }

        // Invoke service() method
        try {
            mm.invoke(script, [ httpReq, httpResp ] as Object[])
        } catch (final Throwable t) {
            jexler.trackIssue(jexler, "Service method failed to handle HTTP request.", t)
            sendError(httpReq, httpResp, 500)
        }
    }

    private String getJexlerId(final HttpServletRequest httpReq) {
        final Script idGetterScript = idGetterClass.newInstance()
        idGetterScript.binding = new Binding([ 'httpReq' : httpReq, 'log' : LOG ])
        return (String)idGetterScript.run()

    }

    private void sendError(final HttpServletRequest httpReq, final HttpServletResponse httpResp, final int status) {
        httpResp.status = status
        final Script errorSenderScript = errorSenderClass.newInstance()
        errorSenderScript.binding = new Binding([ 'httpReq' : httpReq, 'httpResp' : httpResp ])
        errorSenderScript.run()
    }

}
