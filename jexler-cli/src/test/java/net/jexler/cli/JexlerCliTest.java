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

package net.jexler.cli;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for manual tests of jexler cli.
 *
 * @author $(whois jexler.net)
 */
public final class JexlerCliTest
{
    static final Logger log = LoggerFactory.getLogger(JexlerCli.class);

    /**
     * Calls main() of jexler cli with webapp suite.
     *
     * @throws IOException if an I/O error occurs while trying to read from stdin
     */
    public static void main(final String[] args) throws IOException {
        String[] cliArgs = new String[] { "../jexler-war/src/main/webapp/WEB-INF/suite" };
        JexlerCli.main(cliArgs);
    }

    @Test
    public void versionTest() throws Exception {
        String[] cliArgs = new String[] { "-v" };
        JexlerCli.main(cliArgs);
    }

}
