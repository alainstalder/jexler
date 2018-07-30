/*
   Copyright 2012-now $(whois jexler.net)

   Licensed under the Apache License, Version 2.0 (the "License")
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package net.jexler

import ch.grengine.code.CompilerFactory
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory
import ch.grengine.source.DefaultSourceFactory
import ch.grengine.source.Source
import ch.grengine.source.SourceFactory
import ch.grengine.sources.BaseSources
import ch.grengine.sources.DirBasedSources
import groovy.transform.CompileStatic

/**
 * Implementation of the Grengine Sources interface for all non-runnable
 * jexlers in a directory, i.e. for utility classes and ConfigSlurper
 * config, etc., which is shared by several jexlers.
 *
 * @author $(whois jexler.net)
 */
@CompileStatic
class JexlerContainerSources extends BaseSources {

    private final Builder builder

    private final JexlerContainer container
    private final SourceFactory sourceFactory

    /**
     * constructor from builder.
     */
    protected JexlerContainerSources(final Builder builder) {
        this.builder = builder.commit()

        container = builder.container
        sourceFactory = builder.sourceFactory

        super.init(builder.container.dir.canonicalPath, builder.compilerFactory, builder.latencyMs)
    }

    /**
     * gets the updated source set.
     */
    @Override
    protected Set<Source> getSourceSetNew() {
        final Set<Source> sourceSet = new HashSet<Source>()
        container.refresh()
        final List<Jexler> jexlerList = container.jexlers
        for (final Jexler jexler : jexlerList) {
            if (!jexler.runnable) {
                File file = jexler.file
                if (file.exists()) {
                    sourceSet.add(sourceFactory.fromFile(file))
                }
            }
        }
        return sourceSet
    }

    /**
     * gets the builder.
     */
    Builder getBuilder() {
        return builder
    }


    static class Builder {

        /**
         * the default latency (1000ms = 1 second).
         */
        public static final long DEFAULT_LATENCY_MS = 1000L

        private boolean isCommitted

        private final JexlerContainer container
        private CompilerFactory compilerFactory
        private SourceFactory sourceFactory
        private long latencyMs = -1

        /**
         * constructor from container.
         */
        Builder(final JexlerContainer container) {
            this.container = container
            isCommitted = false
        }

        /**
         * sets the compiler factory for compiling sources, default
         * is a new instance of {@link ch.grengine.code.groovy.DefaultGroovyCompilerFactory}.
         */
        Builder setCompilerFactory(final CompilerFactory compilerFactory) {
            check()
            this.compilerFactory = compilerFactory
            return this
        }

        /**
         * sets the source factory for creating sources from files, default
         * is a new instance of {@link ch.grengine.source.DefaultSourceFactory}.
         */
        Builder setSourceFactory(final SourceFactory sourceFactory) {
            check()
            this.sourceFactory = sourceFactory
            return this
        }

        /**
         * sets the latency in milliseconds for checking if script files
         * in the directory have changed, default is {@link #DEFAULT_LATENCY_MS}.
         */
        Builder setLatencyMs(final long latencyMs) {
            check()
            this.latencyMs = latencyMs
            return this
        }

        /**
         * gets the container.
         */
        JexlerContainer getContainer() {
            return container
        }

        /**
         * gets the compiler factory.
         */
        CompilerFactory getCompilerFactory() {
            return compilerFactory
        }

        /**
         * gets the source factory.
         */
        SourceFactory getSourceFactory() {
            return sourceFactory
        }

        /**
         * gets the latency in milliseconds.
         */
        long getLatencyMs() {
            return latencyMs
        }

        private Builder commit() {
            if (!isCommitted) {
                if (compilerFactory == null) {
                    compilerFactory = new DefaultGroovyCompilerFactory()
                }
                if (sourceFactory == null) {
                    sourceFactory = new DefaultSourceFactory()
                }
                if (latencyMs < 0) {
                    latencyMs = DEFAULT_LATENCY_MS
                }
                isCommitted = true
            }
            return this
        }

        /**
         * builds a new instance of {@link DirBasedSources}.
         */
        JexlerContainerSources build() {
            commit()
            return new JexlerContainerSources(this)
        }

        private void check() {
            if (isCommitted) {
                throw new IllegalStateException("Builder already used.")
            }
        }

    }

}
