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

package pt.up.fe.specs.tupatcher.parallel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import pt.up.fe.specs.tupatcher.PatchData;
import pt.up.fe.specs.tupatcher.TUPatcherConfig;
import pt.up.fe.specs.tupatcher.TUPatcherLauncher;
import pt.up.fe.specs.tupatcher.TUPatcherUtils;
import pt.up.fe.specs.tupatcher.parser.TUErrorsData;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.collections.concurrentchannel.ChannelProducer;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.system.ProcessOutput;

public class PatcherProducer {

    private final Map<File, File> sourceFiles;
    private final ChannelProducer<PatcherResult> producer;
    private final TUPatcherConfig config;
    private final Lazy<File> dumper;

    public PatcherProducer(Map<File, File> sourceFiles, ChannelProducer<PatcherResult> producer,
            TUPatcherConfig config) {

        this.sourceFiles = sourceFiles;
        this.producer = producer;
        this.config = config;
        this.dumper = Lazy.newInstance(TUPatcherLauncher::getDumper);
    }

    public static Callable<PatcherResult> createCall(File sourceFile, File baseFolder, TUPatcherConfig config) {
        var patcher = new PatcherProducer(null, null, config);
        return () -> patcher.patchFile(sourceFile, null);
    }

    public boolean execute() {
        for (File sourceFile : sourceFiles.keySet()) {
            var baseFolder = sourceFiles.get(sourceFile);
            var result = patchFile(sourceFile, baseFolder);
            System.out.println("Producer: " + sourceFile);
            producer.put(result);
        }

        // Finish by adding poison
        System.out.println("Producer: poison");
        producer.put(PatcherResult.getPoison());

        return true;
    }

    private PatcherResult patchFile(File sourceFile, File baseFolder) {
        SpecsLogs.info("Patching file " + sourceFile.getName());

        var outputFolder = SpecsIo.mkdir(config.get(TUPatcherConfig.OUTPUT_FOLDER));

        // Get base output folder for the file
        var fileOutputFolder = baseFolder != null
                ? new File(outputFolder, SpecsIo.getRelativePath(sourceFile.getParentFile(), baseFolder))
                : outputFolder;

        var patchedFile = new File(fileOutputFolder, TUPatcherUtils.getPatchedFilename(sourceFile.getName()));

        // Copy file to output folder of file
        SpecsIo.copy(sourceFile, patchedFile);

        var patchData = new PatchData();

        var dumperExe = dumper.get();
        SpecsCheck.checkArgument(dumperExe.isFile(), () -> "Could not obtain dumper executable!");
        // System.out.println("DUMPER: " + dumperExe);

        List<String> command = new ArrayList<>();
        command.add(dumperExe.getAbsolutePath());
        command.add(patchedFile.getAbsolutePath());
        command.add("--");
        command.add("-ferror-limit=1");

        // Always compile as C++
        command.add("-x");
        command.add("c++");

        var workingDir = dumperExe.getAbsoluteFile().getParentFile();

        int n = 0;
        int maxIterations = config.get(TUPatcherConfig.MAX_ITERATIONS);
        ProcessOutput<Boolean, TUErrorsData> output = null;
        var startTime = System.nanoTime();
        while (n < maxIterations) {

            try {
                output = SpecsSystem.runProcess(command,
                        workingDir,
                        TUPatcherLauncher::outputProcessor,
                        inputStream -> TUPatcherLauncher.lineStreamProcessor(inputStream, patchData));
                patchData.write(sourceFile, patchedFile);
                n++;

                // No more errors, break
                if (output.getStdErr().get(TUErrorsData.ERRORS).isEmpty()) {
                    break;
                }
            } catch (Exception e) {
                var endTime = System.nanoTime();
                SpecsLogs.info("Failed patching of file " + sourceFile.getName());
                return new PatcherResult(sourceFile, false, n, endTime - startTime);
            }

        }
        var endTime = System.nanoTime();

        var success = n < maxIterations;
        /*
        // Add stats
        addStats(filepath, success, n, endTime - startTime);
        
        // Write file
        SpecsIo.write(new File("tu_patcher_stats.csv"), stats.buildCsv());
        
        if (n >= maxIterations) {
            System.out.println("!Maximum number of iterations exceeded. Could not solve all errors");
        }
        
        if (output == null) {
            System.out.println("Did not run patcher even once!");
        } else {
            SpecsLogs.info("");
            System.out.println();
            System.out.println("Program status: " + output.getReturnValue());
            System.out.println("Std out result: " + output.getStdOut());
            System.out.println("Std err result: " + output.getStdErr());
        }
        */
        var finishedFailed = success ? "Success on" : "Failed";
        SpecsLogs.info(finishedFailed + " patching of file " + sourceFile.getName());
        return new PatcherResult(sourceFile, success, n, endTime - startTime);
    }

}
