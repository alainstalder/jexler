/*
   Copyright 2012-now by Alain Stalder. Made in Switzerland.

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

package ch.artecat.jexler

import ch.artecat.jexler.Jexler.Events
import ch.artecat.jexler.service.Event
import ch.artecat.jexler.service.StopEvent

import groovy.transform.CompileStatic

/**
 * Jexler dispatcher for use in jexler scripts.
 *
 * Allows to handle lifecycle and events a bit more structured
 * by implementing specific methods like start() or handleCronEvent()
 * in the jexler script.
 *
 * @author Alain Stalder
 */
@CompileStatic
class JexlerDispatcher {

    static void dispatch(final Script script) {

        final Jexler jexler = (Jexler)script.binding.variables.jexler
        final Events events = (Events)script.binding.variables.events

        final MetaClass mc = script.metaClass
        final Object[] noArgs = []

        MetaMethod mm = mc.getMetaMethod('declare', noArgs)
        if (mm != null) {
            mm.invoke(script, noArgs)
        }

        mm = mc.getMetaMethod('start', noArgs)
        if (mm == null) {
            jexler.trackIssue(jexler, 'Dispatch: Mandatory start() method missing.', null)
            return
        } else {
            mm.invoke(script, noArgs)
        }

        while (true) {
            final Event event = events.take()

            if (event instanceof StopEvent) {
                mm = mc.getMetaMethod('stop', noArgs)
                if (mm != null) {
                    mm.invoke(script, noArgs)
                }
                return
            }

            mm = mc.getMetaMethod("handle${event.class.simpleName}$event.service.id", [ Event.class ])
            if (mm == null) {
                mm = mc.getMetaMethod("handle${event.class.simpleName}", [ Event.class ])
                if (mm == null) {
                    mm = mc.getMetaMethod('handle', [ Event.class ])
                }
            }
            if (mm == null) {
                jexler.trackIssue(jexler, "Dispatch: No handler for event ${event.class.simpleName}" +
                        " from service $event.service.id.", null)
            } else {
                try {
                    mm.invoke(script, [ event ])
                } catch (Throwable t) {
                    jexler.trackIssue(jexler, "Dispatch: Handler $mm.name failed.", t)
                }
            }
        }
    }

}
