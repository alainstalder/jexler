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

package ch.grengine.jexler.war

import groovy.transform.CompileStatic
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.Configuration
import org.eclipse.jetty.webapp.WebAppContext
import org.junit.jupiter.api.Test
import spock.lang.Tag

/**
 * Starts embedded jetty with jexler webapp.
 *
 * @author Alain Stalder
 */
@Tag("demo")
@CompileStatic
class JexlerJettyTest {

    @Test
    void demo() throws Exception {

        // Embedded Jetty with JSP support
        // See https://examples.javacodegeeks.com/enterprise-java/jetty/jetty-jsp-example/

        System.setProperty('groovy.grape.report.downloads', 'true')

        // Create server
        final int port = 9080
        final Server server = new Server(port)

        // Create context
        final WebAppContext context = new WebAppContext()
        context.resourceBase = './src/main/webapp'
        context.descriptor = 'WEB-INF/web.xml'
        context.contextPath = '/'
        context.parentLoaderPriority = true

        // "3. Including the JSTL jars for the webapp." in context
        context.setAttribute('org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern','.*/[^/]*jstl.*\\.jar$')

        // "4. Enabling the Annotation based configuration" in context
        final Configuration.ClassList classList = Configuration.ClassList.setServerDefault(server)
        classList.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration",
                "org.eclipse.jetty.plus.webapp.EnvConfiguration",
                "org.eclipse.jetty.plus.webapp.PlusConfiguration")
        classList.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration")

        // Set handler and start server
        server.handler = context
        server.start()

        println()
        println('***************************************************************')
        println("Jexler in embedded jetty running on http://localhost:$port/")
        println('Press ctrl-c to stop.')
        println('***************************************************************')
        
        server.join()
    }

}
