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

package net.jexler;

/**
 * Interface for a sensor ("eyes" of a jexler).
 *
 * Expectation is typically that start() and stop() return
 * only after the service has really started or stopped,
 * and that these methods are idempotent.
 *
 * The id is intended to be unique per jexler.
 *
 * @author $(whois jexler.net)
 */
public interface Sensor extends Service<Sensor> {

    /**
     * Get event handler that this sensor sends events to.
     * @return event handler
     */
    EventHandler getEventHandler();
}
