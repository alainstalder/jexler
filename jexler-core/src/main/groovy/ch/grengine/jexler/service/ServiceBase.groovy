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

package ch.grengine.jexler.service

import groovy.transform.CompileStatic

import static ServiceState.OFF

/**
 * Abstract service base implementation.
 *
 * @author Alain Stalder
 */
@CompileStatic
abstract class ServiceBase implements Service {

    private final String id
    private volatile ServiceState state

    /**
     * Constructor.
     * @param jexler the jexler to send events to
     * @param id the id of the service
     */
    ServiceBase(final String id) {
        this.id = id
        state = OFF
    }

    /**
     * Set state to given value.
     */
    void setState(final ServiceState state) {
        this.state = state
    }
        
    @Override
    ServiceState getState() {
        return state
    }

    @Override
    String getId() {
        return id
    }

}
