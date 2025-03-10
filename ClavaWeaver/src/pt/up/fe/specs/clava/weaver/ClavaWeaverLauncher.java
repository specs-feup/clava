/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.weaver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.runner.Result;

import eu.antarex.clang.parser.tests.CTest;
import eu.antarex.clang.parser.tests.CxxTest;
import eu.antarex.clang.parser.tests.CBenchTest;
import eu.antarex.clang.parser.tests.CxxBenchTest;
import larai.LaraI;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.lara.WeaverLauncher;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

public class ClavaWeaverLauncher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        boolean success = execute(args);

        // Only exit if not in server mode
        if (!LaraI.isServerMode()) {
            int exitValue = success ? 0 : 1;
            ClavaLog.debug("Calling System.exit() on ClavaWeaverLauncher, no running GUI detected");
            System.exit(exitValue);
        }

    }

    public static boolean execute(String[] args) {

        // If junit file present, run junit
        if (new File("junit-parser").isFile()) {
            ClavaLog.info("Found file 'junit-parser', running parser unit tets");
            Result result = org.junit.runner.JUnitCore.runClasses(CBenchTest.class,
                    CTest.class, CxxBenchTest.class, CxxTest.class);
            System.out.println("RESULT:\n" + result);
            return result.getFailures().isEmpty();
        }

        return new WeaverLauncher(new CxxWeaver()).launch(args);
    }

    public static boolean execute(List<String> args) {
        return execute(args.toArray(new String[0]));
    }

    public static String[] executeParallel(String[][] args, int threads, List<String> clavaCommand) {
        return executeParallel(args, threads, clavaCommand, SpecsIo.getWorkingDir().getAbsolutePath());
    }

    /**
     * 
     * @param args
     * @param threads
     * @param clavaCommand
     * @param workingDir
     * @return an array with the same size as the number if args, with strings representing JSON objects that represent
     *         the outputs of the execution. The order of the results is the same as the args
     */
    public static String[] executeParallel(String[][] args, int threads, List<String> clavaCommand, String workingDir) {

        var workingFolder = SpecsIo.sanitizeWorkingDir(workingDir);

        var customThreadPool = threads > 0 ? new ForkJoinPool(threads) : new ForkJoinPool();

        // Choose executor
        Function<String[], Boolean> clavaExecutor = clavaCommand.isEmpty() ? ClavaWeaverLauncher::executeSafe
                : weaverArgs -> ClavaWeaverLauncher.executeOtherJvm(weaverArgs, clavaCommand, workingFolder);

        ClavaLog.info(
                () -> "Launching " + args.length + " instances of Clava in parallel, using a parallelism level of "
                        + threads);

        if (!clavaCommand.isEmpty()) {
            ClavaLog.info(
                    () -> "Each Clava instance will run on a separate process, using the command " + clavaCommand);
        }

        // Create paths for the results
        List<File> resultFiles = new ArrayList<>();
        var resultsFolder = SpecsIo.getTempFolder("clava_parallel_results_" + UUID.randomUUID());
        ClavaLog.debug(() -> "Create temporary folder for storing results of Clava parallel execution: "
                + resultsFolder.getAbsolutePath());

        for (int i = 0; i < args.length; i++) {
            resultFiles.add(new File(resultsFolder, "clava_parallel_result_" + i + ".json"));
        }

        try {

            // Adapt the args so that each execution produces a result file
            String[][] adaptedArgs = new String[args.length][];
            for (int i = 0; i < args.length; i++) {
                var newArgs = Arrays.copyOf(args[i], args[i].length + 2);
                newArgs[newArgs.length - 2] = "-r";
                newArgs[newArgs.length - 1] = resultFiles.get(i).getAbsolutePath();
                adaptedArgs[i] = newArgs;
            }

            customThreadPool.submit(() -> Arrays.asList(adaptedArgs).parallelStream()
                    .map(clavaExecutor)
                    .collect(Collectors.toList())).get();

            // Find the file for each execution
            return collectResults(resultFiles);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return collectResults(resultFiles);
        } catch (ExecutionException e) {
            ClavaLog.info("Unrecoverable exception while executing parallel instances of Clava: " + e);
            return collectResults(resultFiles);
        } finally {
            SpecsIo.deleteFolder(resultsFolder);
        }

    }

    private static String[] collectResults(List<File> resultFiles) {
        List<String> results = new ArrayList<>();
        for (var resultFile : resultFiles) {
            // If file does not exist, create empty object
            if (!resultFile.isFile()) {
                results.add("{}");
                continue;
            }

            results.add(SpecsIo.read(resultFile));
        }

        return results.toArray(size -> new String[size]);
    }

    private static boolean executeSafe(String[] args) {
        try {
            return execute(args);
        } catch (Exception e) {
            ClavaLog.info("Exception during Clava execution: " + e);
            return false;
        }
    }

    private static boolean executeOtherJvm(String[] args, List<String> clavaCommand, File workingDir) {
        try {
            List<String> newArgs = new ArrayList<>();
            newArgs.addAll(clavaCommand);
            newArgs.addAll(Arrays.asList(args));

            var result = SpecsSystem.run(newArgs, workingDir);

            return result == 0;
        } catch (Exception e) {
            ClavaLog.info("Exception during Clava execution: " + e);
            return false;
        }
    }
}
