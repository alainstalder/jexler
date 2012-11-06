/*
   Copyright 2012 $(whois jexler.net)

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

package net.jexler.core;

import java.util.List;


/**
 * Interface for a jexler.
 *
 * @author $(whois jexler.net)
 */
public interface Jexler {

    /**
     * Start jexler.
     */
    void start();

    /**
     * Tell if jexler is running.
     * @return true if running
     */
    boolean isRunning();

    /**
     * Get list of handlers (empty if not running).
     * @return handlers
     */
    List<JexlerHandler> getHandlers();

    /**
     * Stop jexler.
     */
    void stop();

    /**
     * Get id.
     * @return id, never null
     */
    String getId();

    /**
     * Get human readable description of jexler.
     * @return description, never null
     */
    String getDescription();

}
