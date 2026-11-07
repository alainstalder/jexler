/*
   Copyright 2010-now by Alain Stalder ("Jex Jexler").

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

package ch.grengine.jexler.war

import org.eclipse.jetty.ee11.annotations.AnnotationConfiguration
import org.eclipse.jetty.ee11.webapp.JspConfiguration
import org.eclipse.jetty.ee11.webapp.WebAppContext
import org.eclipse.jetty.server.Server
import spock.lang.Specification
import spock.lang.Tag

/**
 * Starts embedded jetty with jexler webapp.
 *
 * @author Alain Stalder
 */
@Tag("demo")
//@CompileStatic
class JexlerJettyTest extends Specification {

    def 'DEMO Jexler in Jetty'() {

        given:

        System.setProperty('groovy.grape.report.downloads', 'true')

        // Create server
        final int port = 9090
        final Server server = new Server(port)

        // Create context
        final WebAppContext context = new WebAppContext()
        context.baseResourceAsString = './src/main/webapp'
        context.contextPath = '/'
        context.parentLoaderPriority = true

        // Scan JSTL jar on the classpath for TLDs
        context.setAttribute('org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern','.*/[^/]*jstl.*\\.jar$')

        // Enable annotations (needed for JSP initializer) and JSP
        context.addConfiguration(new AnnotationConfiguration(), new JspConfiguration())

        when:

        // Set handler and start server
        server.handler = context
        server.start()

        println()
        println('***************************************************************')
        println("Jexler in embedded jetty running on http://localhost:$port/")
        println('Press ctrl-c to stop.')
        println('***************************************************************')
        
        server.join()

        then:

        true
    }

}
