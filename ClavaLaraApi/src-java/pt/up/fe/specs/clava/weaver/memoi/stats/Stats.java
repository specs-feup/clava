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

package pt.up.fe.specs.clava.weaver.memoi.stats;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.weaver.memoi.MemoiComparator;
import pt.up.fe.specs.clava.weaver.memoi.MemoiUtils;
import pt.up.fe.specs.clava.weaver.memoi.MergedMemoiReport;
import pt.up.fe.specs.util.SpecsIo;

public class Stats {

    final private double unique;
    final private double totalCalls;
    final private double repetition;
    final private double averageRepetition;
    final private double top3percentage;
    final private double top5percentage;
    final private double top10percentage;
    final private Object elements5;
    final private Object elements10;
    final private Object elements25;
    final private Object elements50;
    final private BoxWhisker bw;
    final private Histogram hist;

    public static boolean isSorted(List<Integer> listOfT) {
        Integer previous = null;
        for (Integer current : listOfT) {
            if (previous != null && current < previous)
                return false;
            previous = current;
        }
        return true;
    }

    public Stats(MergedMemoiReport report) {

        var elements = report.getElements();
        var reportCount = report.getReportCount();
        var calls = report.getCalls();
        var entries = report.getSortedCounts(MemoiComparator.mean(report).reversed());

        var meanCounts = entries.stream()
                .map(c -> (int) MemoiUtils.mean(c.getCounter(), reportCount))
                .collect(Collectors.toList());

        this.unique = MemoiUtils.mean(elements, reportCount);
        this.totalCalls = MemoiUtils.mean(calls, reportCount);

        this.repetition = 100.0 * (totalCalls - unique) / totalCalls;
        this.averageRepetition = totalCalls / unique;

        double top3total = totalTopN(meanCounts, 3);
        this.top3percentage = top3total / totalCalls * 100;

        double top5total = totalTopN(meanCounts, 5);
        this.top5percentage = top5total / totalCalls * 100;

        double top10total = totalTopN(meanCounts, 10);
        this.top10percentage = top10total / totalCalls * 100;

        int defaultValue = (int) MemoiUtils.mean(elements, reportCount);
        this.elements5 = elementsForRatio(meanCounts, totalCalls, 0.05, defaultValue);
        this.elements10 = elementsForRatio(meanCounts, totalCalls, 0.1, defaultValue);
        this.elements25 = elementsForRatio(meanCounts, totalCalls, 0.25, defaultValue);
        this.elements50 = elementsForRatio(meanCounts, totalCalls, 0.5, defaultValue);

        this.bw = new BoxWhisker(meanCounts);
        this.hist = new Histogram(meanCounts);
    }

    public static int elementsForRatio(List<Integer> meanCounts, double total, double ratio, int defaultValue) {

        int sum = 0;

        for (int i = 0; i < meanCounts.size(); i++) {

            sum += meanCounts.get(i);
            if (sum / total > ratio) {

                return i + 1;
            }
        }

        return defaultValue;
    }

    private static int totalTopN(List<Integer> meanCounts, int n) {

        int result = 0;

        final int min = Math.min(meanCounts.size(), n);

        for (int i = 0; i < min; i++) {

            result += meanCounts.get(i);
        }

        return result;
    }

    public void print() {

        System.out.println("unique inputs: " + unique);
        System.out.println("total calls: " + totalCalls);

        System.out.println("repetition: " + repetition);
        System.out.println("average repetition: " + averageRepetition);

        System.out.println("top 3: " + String.format("%.2f%%", top3percentage));
        System.out.println("top 5: " + String.format("%.2f%%", top5percentage));
        System.out.println("top 10: " + String.format("%.2f%%", top10percentage));

        System.out.println("elements for 5%: " + elements5);
        System.out.println("elements for 10%: " + elements10);
        System.out.println("elements for 25%: " + elements25);
        System.out.println("elements for 50%: " + elements50);

        System.out.println(bw);
        System.out.println(hist);
    }
}
