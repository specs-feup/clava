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

package pt.up.fe.specs.tupatcher.scripts;

import java.io.File;
import java.util.Locale;

import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.csv.CsvReader;

public class PatcherCsvProcessor {

    public static void main(String[] args) {
        var csvFile = new File("C:\\Users\\JBispo\\Desktop\\test\\tu_patcher_stats.csv");

        try (var csvReader = new CsvReader(csvFile)) {

            long totalFiles = 0;
            long totalSuccesses = 0;
            long totalIterations = 0;
            long totalTime = 0;
            while (csvReader.hasNext()) {
                var values = csvReader.next();

                totalFiles++;

                var isSuccess = Boolean.parseBoolean(values.get(1));
                if (isSuccess) {
                    totalSuccesses++;
                }

                totalIterations += Integer.parseInt(values.get(2));

                totalTime += Long.parseLong(values.get(3));
            }

            var successRatio = (double) totalSuccesses / (double) totalFiles;
            var averageIterations = (double) totalIterations / (double) totalFiles;

            System.out.println("Total Files: " + totalFiles);
            System.out.println("Successes: " + totalSuccesses);
            System.out.println("Success ratio: " + SpecsStrings.toPercentage(successRatio));
            System.out.println("Average iterations: " + String.format(Locale.UK, "%f", averageIterations));
            System.out.println("Accumulate execution time (all threads): " + SpecsStrings.parseTime(totalTime));

        }
    }
}
