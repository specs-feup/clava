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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import pt.up.fe.specs.tupatcher.TUPatcherConfig;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.collections.concurrentchannel.ConcurrentChannel;

public class ParallelPatcher {

    private final TUPatcherConfig config;

    public ParallelPatcher(TUPatcherConfig config) {
        this.config = config;
    }

    public int execute() {
        var sourcePaths = config.get(TUPatcherConfig.SOURCE_PATHS).getStringList();

        var validExtensions = new HashSet<>(config.get(TUPatcherConfig.SOURCE_EXTENSIONS).getStringList());

        // Convert source paths to list of files
        var sourceFiles = getSourceFiles(sourcePaths, validExtensions);

        var numThreads = config.get(TUPatcherConfig.NUM_THREADS);
        numThreads = numThreads > 0 ? numThreads : Runtime.getRuntime().availableProcessors();

        List<Map<File, File>> partitionedSourceFiles = partitionFiles(sourceFiles, numThreads);

        // Create ConcurrentChannel
        var channel = new ConcurrentChannel<PatcherResult>(numThreads);

        List<Future<?>> futures = new ArrayList<>();

        // Create producers
        for (var subSourceFiles : partitionedSourceFiles) {
            var producer = new PatcherProducer(subSourceFiles, channel.createProducer(), config);
            var executor = Executors.newSingleThreadExecutor();
            futures.add(executor.submit(() -> producer.execute()));
            executor.shutdown();
        }

        // Create consumer
        var consumer = new ResultsConsumer(numThreads, channel.createConsumer());
        var executor = Executors.newSingleThreadExecutor();
        futures.add(executor.submit(() -> consumer.execute()));
        executor.shutdown();

        // Obtain values of futures, to synchronize execution
        int acc = 1;
        for (var future : futures) {
            SpecsSystem.get(future);
            System.out.println("Main " + acc);
            acc++;
        }

        System.out.println("FINISHED!");
        // var subLists = Lists.partition(sourceFiles, sourceFiles.size() / numThreads);
        // for(int i=0; i<)
        // Create as many producers as threads
        // IntStream.of(numThreads).mapToObj(i -> new PatcherProducer(sourceFiles, channel.createProducer()))

        // Have a single consumer that collects results

        return 0;
    }

    private List<Map<File, File>> partitionFiles(Map<File, File> sourceFiles, Integer numThreads) {
        // System.out.println("NUM THREADS: " + numThreads);

        // Create maps
        var partitioned = IntStream.range(0, numThreads)
                .mapToObj(i -> (Map<File, File>) new HashMap<File, File>())
                .collect(Collectors.toList());
        // System.out.println("PARTITIONED SIZE: " + partitioned.size());
        // for (int i = 0; i < sourceFiles.size(); i++) {
        // int partitionedIndex = i % numThreads;
        // // System.out.println("INDEX: " + partitionedIndex);
        // partitioned.get(partitionedIndex).add(sourceFiles.get(i));
        // }

        int index = 0;
        for (var key : sourceFiles.keySet()) {
            int partitionedIndex = index % numThreads;
            // System.out.println("INDEX: " + partitionedIndex);
            partitioned.get(partitionedIndex).put(key, sourceFiles.get(key));
            index++;
        }

        return partitioned;
    }

    private Map<File, File> getSourceFiles(List<String> sourcePaths, HashSet<String> validExtensions) {

        var sourceFiles = new HashMap<File, File>();
        for (var sourcePathname : sourcePaths) {
            SpecsLogs.info("Processing path '" + sourcePathname + "'...");

            var sourcePath = new File(sourcePathname);

            if (sourcePath.isFile()) {
                if (validExtensions.contains(SpecsIo.getExtension(sourcePath))) {
                    sourceFiles.put(sourcePath, null);
                    continue;
                }
            }

            if (sourcePath.isDirectory()) {
                SpecsIo.getFilesRecursive(sourcePath, validExtensions).stream()
                        .forEach(file -> sourceFiles.put(file, sourcePath));
                continue;
            }

            SpecsLogs.info("Source path '" + sourcePathname + "' not found, ignoring");
        }

        // Sort, to maintain consistent order between executions
        // ...but since the execution is parallel, it does not make much of a difference
        // Collections.sort(sourceFiles);

        return sourceFiles;
    }
}
