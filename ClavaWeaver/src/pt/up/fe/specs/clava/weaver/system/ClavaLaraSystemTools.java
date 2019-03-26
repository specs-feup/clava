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

package pt.up.fe.specs.clava.weaver.system;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;
import pt.up.fe.specs.util.system.ProcessOutputAsString;

public class ClavaLaraSystemTools {

    public static ProcessOutputAsString runCommand(String command, String workingDir, boolean printToConsole,
            Long timeoutNanos) {

        ArgumentsParser argsParser = ArgumentsParser.newCommandLine();
        List<String> commandList = argsParser.parse(command);

        return runCommand(commandList, workingDir, printToConsole, timeoutNanos);
    }

    public static ProcessOutputAsString runCommand(List<String> commandList, String workingDir,
            boolean printToConsole, Long timeoutNanos) {

        // System.out.println("CURRENT FOLDER:" + SpecsIo.getWorkingDir().getAbsolutePath());
        try {
            return ClavaSystem.runProcess(commandList, new File(workingDir), true, printToConsole, timeoutNanos);
        } catch (Exception e) {
            String command = commandList.stream().collect(Collectors.joining(" "));

            // if (stopOnError) {
            // throw new RuntimeException("Problems while running command '" + command + "':" + e.getMessage(), e);
            // }

            SpecsLogs.msgInfo("Problems while running command '" + command + "':" + e.getMessage());
            return new ProcessOutputAsString(-1, "", e.getMessage());
        }

    }

}

// cmd("sh dsfklsd fskld fsdkl fsjkl ") > "log.txt";