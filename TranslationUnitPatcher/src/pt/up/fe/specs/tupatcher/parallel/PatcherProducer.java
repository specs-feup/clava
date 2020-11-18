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
import java.util.Map;

import pt.up.fe.specs.tupatcher.TUPatcherConfig;
import pt.up.fe.specs.tupatcher.TUPatcherUtils;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.collections.concurrentchannel.ChannelProducer;

public class PatcherProducer {

    private final Map<File, File> sourceFiles;
    private final ChannelProducer<PatcherResult> producer;
    private final TUPatcherConfig config;

    public PatcherProducer(Map<File, File> sourceFiles, ChannelProducer<PatcherResult> producer,
            TUPatcherConfig config) {

        this.sourceFiles = sourceFiles;
        this.producer = producer;
        this.config = config;
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
        var outputFolder = SpecsIo.mkdir(config.get(TUPatcherConfig.OUTPUT_FOLDER));

        // Get base output folder for the file
        var fileOutputFolder = baseFolder != null
                ? new File(outputFolder, SpecsIo.getRelativePath(sourceFile.getParentFile(), baseFolder))
                : outputFolder;

        var patchedFile = new File(fileOutputFolder, TUPatcherUtils.getPatchedFilename(sourceFile.getName()));
        /*
        // Copy file to output folder of file
        SpecsIo.copy(filepath, patchedFile);
        
        // System.out.println("PATCHING " + filepath);
        var patchData = new PatchData();
        
        var dumperExe = dumper.get();
        SpecsCheck.checkArgument(dumperExe.isFile(), () -> "Could not obtain dumper executable!");
        // System.out.println("DUMPER: " + dumperExe);
        
        List<String> command = new ArrayList<>();
        // command.add(DUMPER_EXE);
        command.add(dumperExe.getAbsolutePath());
        command.add(patchedFile.getAbsolutePath());
        command.add("--");
        command.add("-ferror-limit=1");
        
        // Always compile as C++
        command.add("-x");
        command.add("c++");
        
        int n = 0;
        int maxIterations = config.get(TUPatcherConfig.MAX_ITERATIONS);
        ProcessOutput<Boolean, TUErrorsData> output = null;
        var startTime = System.nanoTime();
        while (n < maxIterations) {
        
            try {
                output = SpecsSystem.runProcess(command,
                        TUPatcherLauncher::outputProcessor,
                        inputStream -> TUPatcherLauncher.lineStreamProcessor(inputStream, patchData));
                patchData.write(filepath, patchedFile);
                n++;
                // if (n >= maxIterations) {
                // System.out.println();
                // System.out.println("Program status: " + output.getReturnValue());
                // System.out.println("Std out result: " + output.getStdOut());
                // System.out.println("Std err result: " + output.getStdErr());
                // throw new RuntimeException("Maximum number of iterations exceeded. Could not solve errors");
                // }
                // System.out.print('.');
        
                // No more errors, break
                if (output.getStdErr().get(TUErrorsData.ERRORS).isEmpty()) {
                    break;
                }
            } catch (Exception e) {
                var endTime = System.nanoTime();
                addStats(filepath, false, n, endTime - startTime);
                throw new RuntimeException("Could not patch file", e);
            }
        
        }
        var endTime = System.nanoTime();
        
        var success = n < maxIterations;
        
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
        return new PatcherResult(patchedFile.toString());
    }

}
