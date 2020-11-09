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

package pt.up.fe.specs.clava.weaver.memoi;

import java.util.Locale;

public class DmtStats {

    private int reportCount;
    private int tableElements;
    private double tableCalls;
    private double capacity;
    private double totalCalls;
    private double collisionPercentage;
    private double elementCoverage;
    private double callCoverage;
    private int tableSize;
    private int numSets;
    private int totalCollisions;
    private int totalElements;
    private int maxCollision;

    public DmtStats(DirectMappedTable dmt) {

        this.reportCount = dmt.getReport().getReportCount();

        this.tableSize = dmt.getTable().size();
        this.tableElements = tableSize;

        this.tableCalls = dmt.getTable().values().parallelStream()
                .map(MergedMemoiEntry::getCounter)
                .map(l -> MemoiUtils.mean(l, reportCount))
                .reduce(0.0, Double::sum);

        this.numSets = dmt.getNumSets();
        this.totalCollisions = dmt.getTotalCollisions();
        this.totalElements = dmt.getTotalElements();
        this.maxCollision = dmt.getMaxCollision();

        this.capacity = 100.0 * tableSize / numSets;
        this.totalCalls = MemoiUtils.mean(dmt.getReport().getCalls(), reportCount);
        this.collisionPercentage = 100.0 * totalCollisions / totalElements;
        this.elementCoverage = 100.0 * tableElements / totalElements;
        this.callCoverage = 100.0 * tableCalls / totalCalls;
    }

    public int getReportCount() {
        return reportCount;
    }

    public int getTableElements() {
        return tableElements;
    }

    public double getTableCalls() {
        return tableCalls;
    }

    public double getCapacity() {
        return capacity;
    }

    public double getTotalCalls() {
        return totalCalls;
    }

    public double getCollisionPercentage() {
        return collisionPercentage;
    }

    public double getElementCoverage() {
        return elementCoverage;
    }

    public double getCallCoverage() {
        return callCoverage;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        Locale l = null;
        builder.append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("=== table stats ===");
        builder.append(System.lineSeparator());

        builder.append("table capacity: " + tableSize + "/" + numSets + " ("
                + String.format(l, "%.2f", capacity) + "%)");

        builder.append(System.lineSeparator());

        builder.append("collisions: " + totalCollisions + "/" + totalElements + " ("
                + String.format(l, "%.2f", collisionPercentage) + "%)");
        builder.append(System.lineSeparator());

        builder.append("largest collision: " + maxCollision);
        builder.append(System.lineSeparator());

        builder.append("element coverage: " + tableElements + "/" + totalElements + " ("
                + String.format(l, "%.2f", elementCoverage) + "%)");
        builder.append(System.lineSeparator());

        builder.append(
                "call coverage: " + tableCalls + "/" + totalCalls + " (" + String.format(l, "%.2f", callCoverage)
                        + "%)");
        builder.append(System.lineSeparator());

        return builder.toString();
    }

}
