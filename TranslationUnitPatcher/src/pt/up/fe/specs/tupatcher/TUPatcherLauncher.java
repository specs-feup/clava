/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.tupatcher;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.suikasoft.jOptions.streamparser.LineStreamParser;

import pt.up.fe.specs.tupatcher.parser.TUErrorData;
import pt.up.fe.specs.tupatcher.parser.TUErrorParser;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.utilities.LineStream;

public class TUPatcherLauncher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        List<String> command = new ArrayList<>();
        command.add("clang");

        var output = SpecsSystem.runProcess(command, TUPatcherLauncher::outputProcessor,
                TUPatcherLauncher::lineStreamProcessor);

        System.out.println("Program status: " + output.getReturnValue());
        System.out.println("Std out result: " + output.getStdOut());
        System.out.println("Std err result: " + output.getStdErr());
    }

    public static Boolean outputProcessor(InputStream stream) {
        try (var lines = LineStream.newInstance(stream, "Input Stream");) {
            while (lines.hasNextLine()) {
                var line = lines.nextLine();
                System.out.println("StdOut: " + line);
            }
        }

        return true;
    }

    public static String errorProcessor(InputStream stream) {
        try (var lines = LineStream.newInstance(stream, "Input Stream");) {
            while (lines.hasNextLine()) {
                var line = lines.nextLine();
                System.out.println("StdErr: " + line);
            }
        }

        return "Hello";
    }

    public static TUErrorData lineStreamProcessor(InputStream stream) {
        // Create LineStreamParser
        try (LineStreamParser<TUErrorData> lineStreamParser = TUErrorParser.newInstance()) {

            File dumpFile = null;

            // Parse input stream
            String linesNotParsed = lineStreamParser.parse(stream, dumpFile);

            var data = lineStreamParser.getData();

            System.out.println("[TEST] lines not parsed:\n" + linesNotParsed);

            System.out.println("[TEST] Collected data:\n" + data);

            // Return data
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Error while parsing output of Clang error dumper", e);
        }
    }

}
