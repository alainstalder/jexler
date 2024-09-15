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

package ch.grengine.jexler.service

import groovy.transform.CompileStatic

/**
 * Enum for the state of a service.
 *
 * @author Alain Stalder
 */
@CompileStatic
enum ServiceState {

    /** Service is off. */
    OFF ('off'),
    /** Service is busy starting. */
    BUSY_STARTING('busy (starting)'),
    /** Service is running and idle. */
    IDLE('idle'),
    /** Service is running and busy processing an event. */
    BUSY_EVENT('busy (event)'),
    /** Service is busy stopping. */
    BUSY_STOPPING('busy (stopping)')

    /** Human readable description of the state. */
    final String info

    /**
     * Constructor from info.
     */
    ServiceState(final String info) {
        this.info = info
    }

    /**
     * Convenience method, returns true if OFF.
     */
    boolean isOff() {
        return this == OFF
    }

    /**
     * Convenience method, returns true if BUSY_STARTING.
     */
    boolean isBusyStarting() {
        return this == BUSY_STARTING
    }

    /**
     * Convenience method, returns true if IDLE.
     */
    boolean isIdle() {
        return this == IDLE
    }

    /**
     * Convenience method, returns true if BUSY_EVENT.
     */
    boolean isBusyEvent() {
        return this == BUSY_EVENT
    }

    /**
     * Convenience method, returns true if BUSY_STOPPING.
     */
    boolean isBusyStopping() {
        return this == BUSY_STOPPING
    }

    /**
     * Convenience method, returns true if not OFF.
     */
    boolean isOn() {
        return this != OFF
    }

    /**
     * Convenience method, returns true if IDLE or BUSY_EVENT,
     * in other words, if the service is up and processing events.
     */
    boolean isOperational() {
        return this == IDLE || this == BUSY_EVENT
    }

    /**
     * Convenience method, returns true if busy.
     */
    boolean isBusy() {
        return this == BUSY_STARTING || this == BUSY_EVENT || this == BUSY_STOPPING
    }

}

