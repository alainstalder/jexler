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

package ch.grengine.jexler.tool

import ch.grengine.jexler.JexlerUtil

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

/**
 * Tool for running shell commands, just a thin wrapper around
 * the java runtime exec calls.
 * 
 * Note that there are already at least two standard ways of doing this
 * with Groovy APIs, which may or may not be more convenient depending
 * on your use case.
 *
 * @author Alain Stalder
 */
@CompileStatic
class ShellTool {

    /**
     * Simple bean for the result of executing a shell command.
     *
     * @author Alain Stalder
     */
    @CompileStatic
    static class Result {
        final int rc
        final String stdout
        final String stderr
        Result(final int rc, final String stdout, final String stderr) {
            this.rc = rc
            this.stdout = stdout
            this.stderr = stderr
        }
        @Override
        String toString() {
            return "[rc=$rc,stdout='${JexlerUtil.toSingleLine(stdout)}'," +
                    "stderr='${JexlerUtil.toSingleLine(stderr)}']"
        }
    }
    
    /**
     * Helper class for collecting stdout and stderr.
     */
    @CompileStatic
    @PackageScope
    static class OutputCollector extends Thread {

        private final InputStream is
        private final Closure lineHandler
        private final String threadName

        /** Collected output. */
        String output

        OutputCollector(final InputStream is, final Closure lineHandler, final String threadName) {
            this.is = is
            this.lineHandler = lineHandler
            this.threadName = threadName
        }
        @Override
        void run() {
            currentThread().name = threadName
            final StringBuilder out = new StringBuilder()
            // (assume default platform character encoding)
            final Scanner scanner = new Scanner(is)
            while (scanner.hasNext()) {
                String line = scanner.nextLine()
                out.append(line)
                out.append(System.lineSeparator())
                if (lineHandler != null) {
                    lineHandler.call(line)
                }
            }
            scanner.close()
            output = out.toString()
        }
    }

    private File workingDirectory
    private Map<String,String> env
    private Closure stdoutLineHandler
    private Closure stderrLineHandler

    /**
     * Constructor.
     */
    ShellTool() {
    }

    /**
     * Set working directory for the command.
     * If not set or set to null, inherit from parent process.
     * @return this (for chaining calls)
     */
    ShellTool setWorkingDirectory(final File workingDirectory) {
        this.workingDirectory = workingDirectory
        return this
    }

    /**
     * Get working directory for command.
     */
    File getWorkingDirectory() {
        return workingDirectory
    }

    /**
     * Set environment variables for the command.
     * Key is variable name, value is variable value.
     * If not set or set to null, inherit from parent process.
     * @return this (for chaining calls)
     */
    ShellTool setEnvironment(final Map<String,String> env) {
        this.env = env
        return this
    }

    /**
     * Get environment variables for the command.
     */
    Map<String, String> getEnv() {
        return env
    }

    /**
     * Set a closure that will be called to handle each line of stdout.
     * If not set or set to null, do nothing.
     * @return this (for chaining calls)
     */
    ShellTool setStdoutLineHandler(final Closure handler) {
        stdoutLineHandler = handler
        return this
    }

    /**
     * Get closure for handling stdout lines.
     */
    Closure getStdoutLineHandler() {
        return stdoutLineHandler
    }

    /**
     * Set a closure that will be called to handle each line of stderr.
     * If not set or set to null, do nothing.
     * @return this (for chaining calls)
     */
    ShellTool setStderrLineHandler(final Closure handler) {
        stderrLineHandler = handler
        return this
    }

    /**
     * Get closure for handling stderr lines.
     */
    Closure getStderrLineHandler() {
        return stderrLineHandler
    }

    /**
     * Run the given shell command and return the result.
     * If an exception occurs, the return code of the result is set to -1,
     * stderr of the result is set to the stack trace of the exception and
     * stdout of the result is set to an empty string.
     * @param command command to run
     * @return result, never null
     */
    Result run(final String command) {
        try {
            final Process proc = Runtime.runtime.exec(command, toEnvArray(env), workingDirectory)
            return getResult(proc)
        } catch (final Exception e) {
            return getExceptionResult(JexlerUtil.getStackTrace(e))
        }
    }

    /**
     * Run the given shell command and return the result.
     * If an exception occurs, the return code of the result is set to -1,
     * stderr of the result is set to the stack trace of the exception and
     * stdout of the result is set to an empty string.
     * @param cmdList list containing the command and its arguments
     * @return result, never null
     */
    Result run(final List<String> cmdList) {
        final String[] cmdArray = new String[cmdList.size()]
        cmdList.toArray(cmdArray)
        try {
            final Process proc = Runtime.runtime.exec(cmdArray, toEnvArray(env), workingDirectory)
            return getResult(proc)
        } catch (final Exception e) {
            return getExceptionResult(JexlerUtil.getStackTrace(e))
        }
    }
    
    /**
     * Get result of given process.
     */
    private Result getResult(final Process proc) throws Exception {
        final OutputCollector outCollector = new OutputCollector(proc.inputStream, stdoutLineHandler, 'stdout collector')
        final OutputCollector errCollector = new OutputCollector(proc.errorStream, stderrLineHandler, 'stderr collector')
        outCollector.start()
        errCollector.start()
        final int rc = proc.waitFor()
        outCollector.join()
        errCollector.join()
        return new Result(rc, outCollector.output, errCollector.output)
    }

    /**
     * Get result in case where an exception occurred.
     */
    private static Result getExceptionResult(final String stackTrace) {
        return new Result(-1, '', stackTrace)
    }

    /**
     * Convert map of name and value to array of name=value.
     */
    private static String[] toEnvArray(final Map<String,String> env) {
        final List envList = []
        env?.each { final key, final value ->
            envList.add("$key=$value")
        }
        return envList as String[]
    }

}
