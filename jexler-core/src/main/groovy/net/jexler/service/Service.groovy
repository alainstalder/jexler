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

import groovy.transform.CompileStatic

/**
 * Interface for a service.
 * Implemented by jexler and jexler container and by services used by jexlers.
 *
 * @author $(whois jexler.net)
 */
@CompileStatic
interface Service {

    /**
     * Initiate service start.
     */
    void start()

    /**
     * Initiate service stop.
     */
    void stop()

    /**
     * Get state of the service.
     */
    ServiceState getState()

    /**
     * Forcefully terminate service, as far as possible;
     * should not wait for anything, nor throw anything.
     */
    void zap()

    /**
     * Get service id.
     */
    String getId()

}
