/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.tester;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.Platforms;
import pt.up.fe.specs.clang.SupportedPlatform;
import pt.up.fe.specs.clava.weaver.ClavaWeaverLauncher;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;
import pt.up.fe.specs.util.properties.SpecsProperties;
import pt.up.fe.specs.util.system.ProcessOutputAsString;
import pt.up.fe.specs.util.utilities.ProgressCounter;
import pt.up.fe.specs.util.utilities.StringLines;

public class ClavaTester {

    private static final String CMAKELISTS_FILENAME = "CMakeLists.txt";
    private static final String ARGS_FILENAME = "args.txt";

    // private static final Set<String> EXECUTABLES_IGNORE_LIST = new HashSet<>(Arrays.asList("makefile"));

    private final SupportedPlatform platform;
    private final SpecsProperties config;

    public ClavaTester(SupportedPlatform platform, SpecsProperties clavaTesterProps) {
        this.platform = platform;
        this.config = clavaTesterProps;
    }

    public ClavaTestResult test(File clavaConfig) {
        // Call Clava with the config file
        List<String> args = Arrays.asList("-c", clavaConfig.getAbsolutePath());

        try {
            // boolean success = ClavaLauncher.getInstance().execute(args);
            boolean success = ClavaWeaverLauncher.execute(args);
            if (!success) {
                return ClavaTestResult.newFail("Clava execution did not succeed", ClavaTestStage.CLAVA);
            }
        } catch (Exception e) {
            return ClavaTestResult.newFail(e, ClavaTestStage.CLAVA);
        }

        File configFolder = clavaConfig.getParentFile();

        // Check if there is a CMake file to build application
        File cmakelists = new File(configFolder, CMAKELISTS_FILENAME);
        if (!cmakelists.exists()) {
            SpecsLogs.msgInfo(
                    "Could not find a '" + CMAKELISTS_FILENAME + "' file in the folder of the Clava config, returning");
            return ClavaTestResult.newSuccess(ClavaTestStage.CLAVA);
        }

        // Call CMake stage
        StageResult<File> cmakeResult = stageCmake(configFolder);
        if (!cmakeResult.getResult().isSuccess()) {
            return cmakeResult.getResult();
        }

        // Call last stage, Run Program
        File buildFolder = cmakeResult.getData();
        try {
            return stageRunProgram(buildFolder, configFolder);
        } catch (Exception e) {
            return ClavaTestResult.newFail(e, ClavaTestStage.RUN_PROGRAM);
        }

    }

    private ClavaTestResult stageRunProgram(File buildFolder, File configFolder) {

        File executable = Platforms.getExecutableTry(buildFolder).orElse(null);
        if (executable == null) {
            return ClavaTestResult.newFail(
                    "Could not find executable in build folder '" + buildFolder.getAbsolutePath() + "'",
                    ClavaTestStage.RUN_PROGRAM);
        }
        /*
        // Find executable
        List<File> executables = IoUtils.getFiles(buildFolder).stream()
                .filter(this::isExecutable)
                .collect(Collectors.toList());
        
        if (executables.isEmpty()) {
            return ClavaTestResult.newFail(
                    "Could not find executable in build folder '" + buildFolder.getAbsolutePath() + "'",
                    ClavaTestStage.RUN_PROGRAM);
        }
        
        File executable = getExecutable(executables);
        */
        // Check if there are arguments to be used when calling the program
        File argsFile = new File(configFolder, ARGS_FILENAME);

        // If file does not exist, call program once, without arguments
        if (!argsFile.isFile()) {
            SpecsLogs.msgInfo("No '" + ARGS_FILENAME + "' file found, executing once without arguments");
            return runProgram(Arrays.asList(executable.getAbsolutePath()), buildFolder);
        }

        // Read arguments file, each non-empty line represents the arguments with which the program should be called
        List<String> executions = StringLines.getLines(argsFile).stream()
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());

        ProgressCounter progress = new ProgressCounter(executions.size());
        ArgumentsParser parser = ArgumentsParser.newCommandLine();
        String executablePath = executable.getAbsolutePath();
        SpecsLogs.msgInfo("Found " + executions.size() + " executions");
        for (String execution : executions) {
            SpecsLogs.msgInfo("Executing " + executable.getName() + " " + execution + "... " + progress.next());
            List<String> command = new ArrayList<>();
            command.add(executablePath);
            command.addAll(parser.parse(execution));

            ClavaTestResult result = runProgram(command, buildFolder);
            if (!result.isSuccess()) {
                return result;
            }
        }

        return ClavaTestResult.newSuccess(ClavaTestStage.RUN_PROGRAM);
    }

    private ClavaTestResult runProgram(List<String> command, File buildFolder) {
        ProcessOutputAsString output = SpecsSystem.runProcess(command, buildFolder, false, true);
        if (output.isError()) {
            return ClavaTestResult.newFail(
                    "Problems while running the program '" + command.get(0) + "'",
                    ClavaTestStage.RUN_PROGRAM);
        }

        return ClavaTestResult.newSuccess(ClavaTestStage.RUN_PROGRAM);
    }

    /*
    private static File getExecutable(List<File> executables) {
        if (executables.isEmpty()) {
            throw new RuntimeException("Could not find an executable file in the build folder");
        }
    
        if (executables.size() == 1) {
            return executables.get(0);
        }
    
        File lastModified = null;
        for (int i = 0; i < executables.size(); i++) {
            File currentFile = executables.get(i);
    
            // Check if file is part of the ignore list
            if (EXECUTABLES_IGNORE_LIST.contains(currentFile.getName().toLowerCase())) {
                LoggingUtils.msgInfo("Ignoring executable '" + currentFile.getName() + "'");
                continue;
            }
    
            // If no file yet, just use it
            if (lastModified == null) {
                lastModified = currentFile;
                continue;
            }
    
            if (lastModified.lastModified() < currentFile.lastModified()) {
                lastModified = currentFile;
            }
        }
    
        LoggingUtils.msgInfo("Found more than 1 executable file in the build folder, choosing the most recent one: "
                + lastModified.getAbsolutePath());
    
        return lastModified;
    }
    */
    /*
    private boolean isExecutable(File file) {
        if (platform.isWindows()) {
            return file.getName().endsWith(".exe");
        }
    
        if (platform.isLinux()) {
            return file.canExecute();
        }
    
        throw new NotImplementedException("Not implemented for platform " + platform);
    }
    */

    private StageResult<File> stageCmake(File configFolder) {
        // Prepare folder for compilation
        String buildFoldername = "build-" + platform.getName();
        File buildFolder = SpecsIo.mkdir(configFolder, buildFoldername);

        if (config.getBoolean(ClavaTesterProperties.CLEAN_BUILD_FOLDER)) {
            if (!SpecsIo.deleteFolderContents(buildFolder)) {
                return new StageResult<>(ClavaTestResult.newFail(
                        "Could not delete contents of build folder '" + buildFolder.getAbsolutePath() + "'",
                        ClavaTestStage.CMAKE));
            }
        }

        // Create makefile with cmake
        List<String> cmakeCommand = new ArrayList<>();
        cmakeCommand.add("cmake");
        cmakeCommand.add("..");

        String cmakeGenerator = config.get(ClavaTesterProperties.CMAKE_GENERATOR);
        if (!cmakeGenerator.isEmpty()) {
            cmakeCommand.add("-G");
            cmakeCommand.add(cmakeGenerator);
        }

        // Call cmake in build folder
        ProcessOutputAsString cmakeOutput = SpecsSystem.runProcess(cmakeCommand, buildFolder, false, true);
        if (cmakeOutput.isError()) {
            return new StageResult<>(ClavaTestResult.newFail(
                    "Problems while executing 'cmake'", ClavaTestStage.CMAKE));
        }

        // Call make
        String makeCommand = config.get(ClavaTesterProperties.MAKE_COMMAND);
        if (makeCommand.isEmpty()) {
            SpecsLogs.msgInfo(
                    "No make command defined (property '" + ClavaTesterProperties.MAKE_COMMAND.getKey()
                            + "', using 'make')");
            makeCommand = "make";
        }

        ProcessOutputAsString makeOutput = SpecsSystem.runProcess(Arrays.asList(makeCommand), buildFolder, false, true);
        if (makeOutput.isError()) {
            return new StageResult<>(ClavaTestResult.newFail(
                    "Problems while executing 'make'", ClavaTestStage.CMAKE));
        }

        return new StageResult<File>(buildFolder, ClavaTestResult.newSuccess(ClavaTestStage.CMAKE));
    }

}
