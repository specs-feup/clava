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

import eu.antarex.clang.parser.tests.CBenchTest;
import eu.antarex.clang.parser.tests.CxxBenchTest;
import larai.LaraI;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.cxxweaver.tests.CApiTest;
import pt.up.fe.specs.cxxweaver.tests.CTest;
import pt.up.fe.specs.cxxweaver.tests.CxxApiTest;
import pt.up.fe.specs.cxxweaver.tests.CxxTest;
import pt.up.fe.specs.lara.WeaverLauncher;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

public class ClavaWeaverLauncher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        // System.out.println("Press any key to proceed");
        // SpecsIo.read();

        boolean success = execute(args);

        // Only exit if GUI is not running
        if (!LaraI.isRunningGui()) {
            int exitValue = success ? 0 : 1;
            ClavaLog.debug("Calling System.exit() on ClavaWeaverLauncher, no running GUI detected");
            System.exit(exitValue);
        }

    }

    public static boolean execute(String[] args) {
        // To profile using VisualVM
        // try {
        // System.out.println("PRESS ENTER");
        // System.in.read();
        // } catch (IOException e) {
        // LoggingUtils.msgWarn("Error message:\n", e);
        // }

        // If junit file present, run junit
        if (new File("junit").isFile()) {
            ClavaLog.info("Found file 'junit', running unit tets");
            Result result = org.junit.runner.JUnitCore.runClasses(CApiTest.class, CTest.class, CxxApiTest.class,
                    CxxTest.class);
            System.out.println("RESULT:\n" + result);
            return result.getFailures().isEmpty();
        }

        if (new File("junit-parser").isFile()) {
            ClavaLog.info("Found file 'junit-parser', running parser unit tets");
            Result result = org.junit.runner.JUnitCore.runClasses(CBenchTest.class,
                    eu.antarex.clang.parser.tests.CTest.class, CxxBenchTest.class,
                    eu.antarex.clang.parser.tests.CxxTest.class);
            System.out.println("RESULT:\n" + result);
            return result.getFailures().isEmpty();
        }

        return new WeaverLauncher(new CxxWeaver()).launch(args);

        // // If unit testing flag is present, run unit tester
        // Optional<Boolean> unitTesterResult = runUnitTester(args);
        // if (unitTesterResult.isPresent()) {
        // return unitTesterResult.get();
        // }
        //
        // // If doc generator flag is present, run doc generator
        // Optional<Boolean> docGeneratorResult = runDocGenerator(args);
        // if (docGeneratorResult.isPresent()) {
        // return docGeneratorResult.get();
        // }
        //
        // return LaraLauncher.launch(args, new CxxWeaver());
        // // return LaraI.exec(args, new CxxWeaver());
    }

    public static boolean execute(List<String> args) {
        return execute(args.toArray(new String[0]));
    }

    // private static Optional<Boolean> runUnitTester(String[] args) {
    // // Look for flag
    // String unitTestingFlag = "-" + LaraiKeys.getUnitTestFlag();
    //
    // int flagIndex = IntStream.range(0, args.length)
    // .filter(index -> unitTestingFlag.equals(args[index]))
    // .findFirst()
    // .orElse(-1);
    //
    // if (flagIndex == -1) {
    // return Optional.empty();
    // }
    //
    // List<String> laraUnitArgs = new ArrayList<>();
    // // laraUnitArgs.add("lara-unit-weaver=" + CxxWeaver.class.getName());
    // laraUnitArgs.add("--weaver");
    // laraUnitArgs.add(CxxWeaver.class.getName());
    //
    // // laraUnitArgs.add("lara-unit-weaver=" + CxxWeaver.class.getName());
    // for (int i = flagIndex + 1; i < args.length; i++) {
    // laraUnitArgs.add(args[i]);
    // }
    //
    // SpecsLogs.debug("Launching lara-unit with flags '" + laraUnitArgs + "'");
    //
    // int unitResults = LaraUnitLauncher.execute(laraUnitArgs.toArray(new String[0]));
    //
    // return Optional.of(unitResults == 0);
    // }
    //
    // private static Optional<Boolean> runDocGenerator(String[] args) {
    // // Look for flag
    // String docGeneratorFlag = "-" + LaraiKeys.getDocGeneratorFlag();
    //
    // int flagIndex = IntStream.range(0, args.length)
    // .filter(index -> docGeneratorFlag.equals(args[index]))
    // .findFirst()
    // .orElse(-1);
    //
    // if (flagIndex == -1) {
    // return Optional.empty();
    // }
    //
    // List<String> laraDocArgs = new ArrayList<>();
    // laraDocArgs.add("--weaver");
    // laraDocArgs.add(CxxWeaver.class.getName());
    //
    // for (int i = flagIndex + 1; i < args.length; i++) {
    // laraDocArgs.add(args[i]);
    // }
    //
    // SpecsLogs.debug("Launching lara-doc with flags '" + laraDocArgs + "'");
    //
    // int docResults = LaraDocLauncher.execute(laraDocArgs.toArray(new String[0]));
    //
    // return Optional.of(docResults != -1);
    // }

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

            // var results =
            customThreadPool.submit(() -> Arrays.asList(adaptedArgs).parallelStream()
                    .map(clavaExecutor)
                    .collect(Collectors.toList())).get();

            // Find the file for each execution
            return collectResults(resultFiles);

            // return results.stream()
            // .filter(result -> result == false)
            // .findFirst()
            // .orElse(true);
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
            // DEBUG
            // if (true) {
            // return ClavaWeaverLauncher.execute(args);
            // }

            List<String> newArgs = new ArrayList<>();
            // newArgs.add("java");
            // newArgs.add("-jar");
            // newArgs.add("Clava.jar");
            // newArgs.add("/usr/local/bin/clava");
            newArgs.addAll(clavaCommand);
            newArgs.addAll(Arrays.asList(args));

            // ClavaLog.info(() -> "Launching Clava on another JVM with command: " + newArgs);

            // var result = SpecsSystem.run(newArgs, SpecsIo.getWorkingDir());
            var result = SpecsSystem.run(newArgs, workingDir);

            return result == 0;

            // return execute(args);
        } catch (Exception e) {
            ClavaLog.info("Exception during Clava execution: " + e);
            return false;
        }
    }
}
