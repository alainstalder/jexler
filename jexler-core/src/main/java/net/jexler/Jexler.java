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

import java.nio.file.Path;

import net.jexler.service.Event;
import net.jexler.service.Service;

/**
 * Interface for a jexler, runs a script that handles events.
 *
 * @author $(whois jexler.net)
 */
public interface Jexler extends Service, IssueTracker {
	            
    /**
     * Handle given event.
     * @param event
     */
    public void handle(Event event);
    
    /**
     * Get script file path.
     */
    Path getPath();
    
    /**
     * Get meta info.
     * If the jexler is not running, read from jexler file.
     * If the jexler is running, meta info read from jexler
     * file when the jexler was started.
     */
    MetaInfo getMetaInfo();

}
