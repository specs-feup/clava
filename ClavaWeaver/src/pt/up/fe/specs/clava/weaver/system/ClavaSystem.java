/**
 * Copyright 2019 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver.system;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.system.OutputType;
import pt.up.fe.specs.util.system.ProcessOutput;
import pt.up.fe.specs.util.system.ProcessOutputAsString;
import pt.up.fe.specs.util.system.StreamToString;
import pt.up.fe.specs.util.utilities.ProgressCounter;

/**
 * 
 * @author JoaoBispo
 *
 */
public class ClavaSystem {

    public static ProcessOutputAsString runProcess(List<String> command, File workingDir,
            boolean storeOutput, boolean printOutput, Long timeoutNanos) {

        SpecsLogs.msgInfo("[Clava] Running custom version of runProcess");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDir);
        Function<InputStream, String> stdout = new StreamToString(printOutput, storeOutput, OutputType.StdOut);
        Function<InputStream, String> stderr = new StreamToString(printOutput, storeOutput, OutputType.StdErr);

        ProcessOutput<String, String> output = runProcess(builder, stdout, stderr, timeoutNanos);

        return new ProcessOutputAsString(output.getReturnValue(), output.getStdOut(), output.getStdErr());
    }

    public static <O, E> ProcessOutput<O, E> runProcess(ProcessBuilder builder,
            Function<InputStream, O> outputProcessor, Function<InputStream, E> errorProcessor, Long timeoutNanos) {

        String commandString = SpecsSystem.getCommandString(builder.command());
        SpecsLogs.debug("Launching Process: " + commandString);

        Process process = null;
        try {
            // Experiment: Calling Garbage Collector before starting process in order to reduce memory required to fork
            // VM
            // http://www.bryanmarty.com/2012/01/14/forking-jvm/
            long totalMemBefore = Runtime.getRuntime().totalMemory();
            System.gc();
            long totalMemAfter = Runtime.getRuntime().totalMemory();
            SpecsLogs.msgLib("Preparing to run process, memory before -> after GC: "
                    + SpecsStrings.parseSize(totalMemBefore) + " -> " + SpecsStrings.parseSize(totalMemAfter));
            process = builder.start();
        } catch (IOException e) {
            throw new RuntimeException("Could not start process", e);
        }

        InputStream errorStream = process.getErrorStream();
        InputStream inputStream = process.getInputStream();

        // try {

        // FunctionContainer<InputStream, O> outputContainer = new FunctionContainer<>(outputProcessor);
        // FunctionContainer<InputStream, E> errorContainer = new FunctionContainer<>(errorProcessor);

        ExecutorService stdoutThread = Executors.newSingleThreadExecutor();
        Future<O> outputFuture = stdoutThread.submit(() -> outputProcessor.apply(inputStream));

        ExecutorService stderrThread = Executors.newSingleThreadExecutor();
        Future<E> errorFuture = stderrThread.submit(() -> errorProcessor.apply(errorStream));

        // The ExecutorService objects are shutdown, as they will not
        // receive more tasks.
        stdoutThread.shutdown();
        stderrThread.shutdown();

        return executeProcess(process, timeoutNanos, outputFuture, errorFuture);

    }

    private static <O, E> ProcessOutput<O, E> executeProcess(Process process, Long timeoutNanos, Future<O> outputFuture,
            Future<E> errorFuture) {

        try {
            boolean timedOut = false;
            if (timeoutNanos == null) {
                process.waitFor();
            } else {
                timedOut = !process.waitFor(timeoutNanos, TimeUnit.NANOSECONDS);
                // Destroy all descendants, if available
                destroyDescendants(process.descendants().collect(Collectors.toList()));
            }

            // Read streams before the process ends
            O output = null;
            E error = null;

            try {
                output = outputFuture.get(10, TimeUnit.SECONDS);
                error = errorFuture.get(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while waiting for output/error streams");
            } catch (Exception e) {
                throw new RuntimeException("Exception while waiting for output/error streams", e);
            }

            int returnValue = timedOut ? -1 : process.exitValue();

            if (timedOut) {
                // Get descendants of the process
                List<ProcessHandle> processDescendants = process.descendants().collect(Collectors.toList());

                SpecsLogs.debug(() -> "SpecsSystem.executeProcess: Killing process...");
                process.destroyForcibly();
                SpecsLogs.debug(() -> "SpecsSystem.executeProcess: Waiting killing...");
                boolean processDestroyed = process.waitFor(1, TimeUnit.SECONDS);
                if (processDestroyed) {
                    SpecsLogs.debug(() -> "SpecsSystem.executeProcess: Destroyed");
                } else {
                    SpecsLogs.debug(() -> "SpecsSystem.executeProcess: Could not destroy process!");
                }

                destroyDescendants(processDescendants);

            }

            return new ProcessOutput<>(returnValue, output, error);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProcessOutput<>(-1, null, null);
        }
    }

    private static void destroyDescendants(List<ProcessHandle> processDescendants) {
        // Destroy descendants
        ProgressCounter counter = new ProgressCounter(processDescendants.size());
        SpecsLogs.debug("Found " + processDescendants.size() + " descendants processes");
        for (ProcessHandle handle : processDescendants) {
            SpecsLogs
                    .debug(() -> "SpecsSystem.executeProcess: Killing descendant process... " + counter.next());

            handle.destroyForcibly();
            SpecsLogs.debug(() -> "SpecsSystem.executeProcess: Waiting killing...");
            try {
                handle.onExit().get(1, TimeUnit.SECONDS);
                SpecsLogs.debug(() -> "SpecsSystem.executeProcess: Destroyed");
            } catch (TimeoutException t) {
                SpecsLogs.debug(
                        () -> "SpecsSystem.executeProcess: Timeout while destroying descendant process!");
            } catch (Exception e) {
                SpecsLogs.debug(() -> "SpecsSystem.executeProcess: Could not destroy descendant process!");
            }
        }
    }
}
