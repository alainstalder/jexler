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

package net.jexler.war

import net.jexler.test.DemoTests

import groovy.transform.CompileStatic
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * Starts embedded jetty with jexler webapp.
 *
 * @author $(whois jexler.net)
 */
@Category(DemoTests.class)
@CompileStatic
class JexlerJettyTest {

    @Test
    void demo() throws Exception {

        // Embedded Jetty with JSP support
        // See https://examples.javacodegeeks.com/enterprise-java/jetty/jetty-jsp-example/

        System.setProperty('groovy.grape.report.downloads', 'true')

        // creating server
        final int port = 9080
        final Server server = new Server(port)

        // creating context
        final WebAppContext context = new WebAppContext()
        context.resourceBase = './src/main/webapp'
        context.descriptor = 'WEB-INF/web.xml'
        context.contextPath = '/'
        context.parentLoaderPriority = true

        // "3. Including the JSTL jars for the webapp." in context
        context.setAttribute('org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern','.*/[^/]*jstl.*\\.jar$')

        // "4. Enabling the Annotation based configuration" in context
        org.eclipse.jetty.webapp.Configuration.ClassList classlist =
                org.eclipse.jetty.webapp.Configuration.ClassList.setServerDefault(server)
        classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration",
                "org.eclipse.jetty.plus.webapp.EnvConfiguration",
                "org.eclipse.jetty.plus.webapp.PlusConfiguration")
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration")

        // setting handler and starting server
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
