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

import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.collections.concurrentchannel.ChannelConsumer;

public class ResultsConsumer {

    private static final String CSV_SEP = ";";

    private final int numProducers;
    private final ChannelConsumer<PatcherResult> consumer;
    private final File csvFile;

    public ResultsConsumer(int numProducers, ChannelConsumer<PatcherResult> consumer) {
        this.numProducers = numProducers;
        this.consumer = consumer;
        this.csvFile = new File("tu_patcher_stats.csv");
    }

    public List<PatcherResult> execute() {

        // Reset stats file
        startCsvFile();

        List<PatcherResult> results = new ArrayList<>();

        int currentProducers = numProducers;
        var poison = PatcherResult.getPoison();

        while (currentProducers > 0) {

            try {
                // Get element from channel
                var result = consumer.take();
                results.add(result);

                System.out.println("Consumer: " + result);

                // If poison, decrement and continue
                if (result == poison) {
                    currentProducers--;
                    continue;
                }

                // Process results
                // Append line with results
                csvAppend(result);

            } catch (InterruptedException e) {
                SpecsLogs.info("Interrupted while ResultsConsumer.execute(): " + e.getMessage());
                Thread.currentThread().interrupt();
            }

        }

        return results;
    }

    private void csvAppend(PatcherResult result) {
        String csvLine = result.getFile().getAbsolutePath() + CSV_SEP + result.isSuccess() + CSV_SEP
                + result.getIterations() + CSV_SEP + result.getExecutionTime() + "\n";

        SpecsIo.append(csvFile, csvLine);
    }

    private void startCsvFile() {
        String csvInit = "sep=" + CSV_SEP + "\n"
                + "File" + CSV_SEP + "Success" + CSV_SEP + "Iterations" + CSV_SEP + "Execution Time (ns)\n";

        SpecsIo.write(csvFile, csvInit);
    }

}
