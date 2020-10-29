package pt.up.fe.specs.clava.weaver.memoi;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.specs.clava.weaver.memoi.stats.Stats;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsIo;

/**
 * Copyright 2019 SPeCS.
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

/**
 * Represents the merge of several profiling runs for the same function (signature) and call site.
 * 
 * @author pedro
 *
 */
public class MergedMemoiReport implements java.io.Serializable {

    private static final long serialVersionUID = -8261433007204000797L;

    private String uuid;
    private String id;
    private String funcSig;
    private int inputCount;
    private int outputCount;
    private List<String> inputTypes;
    private List<String> outputTypes;
    private List<String> callSites;

    private List<Integer> elements;
    private List<Integer> calls;
    private List<Integer> hits;
    private List<Integer> misses;

    private Map<String, MergedMemoiEntry> counts;

    private int reportCount = 1;

    private Stats stats;

    public MergedMemoiReport() {

    }

    public MergedMemoiReport(MemoiReport report) {

        this.uuid = report.getUuid();
        this.id = report.getId();
        this.funcSig = report.getFuncSig();
        this.outputTypes = report.getOutputTypes();
        this.inputCount = report.getInputCount();
        this.outputCount = report.getOutputCount();
        this.inputTypes = new ArrayList<>(report.getInputTypes());
        this.callSites = new ArrayList<>(report.getCall_sites());

        this.elements = new ArrayList<Integer>(50);
        this.elements.add(report.getElements());

        this.calls = new ArrayList<Integer>(50);
        this.calls.add(report.getCalls());

        this.hits = new ArrayList<Integer>(50);
        this.hits.add(report.getHits());

        this.misses = new ArrayList<Integer>(50);
        this.misses.add(report.getMisses());

        this.counts = new HashMap<String, MergedMemoiEntry>(report.getCounts().size());
        // for (MemoiEntry oldEntry : report.getCounts()) {
        //
        // String key = oldEntry.getKey();
        // MergedMemoiEntry newEntry = new MergedMemoiEntry(oldEntry, this);
        // counts.put(key, newEntry);
        // }

        // report.getCounts().forEach(
        // (k, v) -> {
        // MergedMemoiEntry newEntry = new MergedMemoiEntry(v, this);
        // counts.put(k, newEntry);
        // });

        report.getCounts().values()
                .stream()
                .map(me -> new MergedMemoiEntry(me))
                .forEach(mme -> counts.put(mme.getKey(), mme));

        // this.counts = report.getCounts().values()
        // .parallelStream()
        // .map(me -> new MergedMemoiEntry(me, this))
        // .collect(
        // Collectors.toMap(MergedMemoiEntry::getKey, mme -> mme));
    }

    // public List<MergedMemoiEntry> getMeanSorted() {
    //
    // var list = new ArrayList<MergedMemoiEntry>(counts.values());
    // list.sort(new MeanComparator(this));
    //
    // return list;
    // }

    public List<MergedMemoiEntry> getSortedCounts(Comparator<MergedMemoiEntry> countComparator) {

        var list = new ArrayList<MergedMemoiEntry>(counts.values());

        list.sort(countComparator);

        return list;
    }

    void mergeReport(MemoiReport tempReport) {

        mergeReport(tempReport, false);
    }

    /**
     * Merges a MemoiReport into this MergedMemoiReport.
     * 
     * @param tempReport
     * @param check
     */
    void mergeReport(MemoiReport tempReport, boolean check) {

        if (check) {
            testReport(tempReport);
        }

        uuid.concat(tempReport.getUuid());
        elements.add(tempReport.getElements());
        calls.add(tempReport.getCalls());
        hits.add(tempReport.getHits());
        misses.add(tempReport.getMisses());

        tempReport.getCounts().forEach(
                (k, v) -> {

                    if (counts.containsKey(k)) {

                        counts.get(k).addCounter(v.getCounter());
                    } else {

                        MergedMemoiEntry newEntry = new MergedMemoiEntry(v);
                        counts.put(k, newEntry);
                    }
                });

        // for (MemoiEntry oldEntry : tempReport.getCounts()) {
        //
        // String key = oldEntry.getKey();
        //
        // if (!counts.containsKey(key)) {
        //
        // MergedMemoiEntry newEntry = new MergedMemoiEntry(oldEntry, this);
        //
        // counts.put(key, newEntry);
        // } else {
        //
        // counts.get(key).addCounter(oldEntry.getCounter());
        // }
        //
        // }

        this.reportCount += 1;
    }

    private void testReport(MemoiReport tempReport) {

        SpecsCheck.checkArgument(this.funcSig.equals(tempReport.getFuncSig()),
                () -> "The function signatures of the reports are not equal");
        SpecsCheck.checkArgument(this.callSites.equals(tempReport.getCall_sites()),
                () -> "The call sites of the reports are not equal");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFuncSig() {
        return funcSig;
    }

    public void setFuncSig(String funcSig) {
        this.funcSig = funcSig;
    }

    public List<String> getOutputTypes() {
        return outputTypes;
    }

    public void setOutputType(List<String> outputTypes) {
        this.outputTypes = outputTypes;
    }

    public int getInputCount() {
        return inputCount;
    }

    public void setInputCount(int inputCount) {
        this.inputCount = inputCount;
    }

    public int getOutputCount() {
        return outputCount;
    }

    public void setOutputCount(int outputCount) {
        this.outputCount = outputCount;
    }

    public List<String> getInputTypes() {
        return inputTypes;
    }

    public void setInputTypes(List<String> inputTypes) {
        this.inputTypes = inputTypes;
    }

    public List<String> getCallSites() {
        return callSites;
    }

    public void setCallSites(List<String> call_sites) {
        this.callSites = call_sites;
    }

    public List<Integer> getElements() {
        return elements;
    }

    public void setElements(List<Integer> elements) {
        this.elements = elements;
    }

    public List<Integer> getCalls() {
        return calls;
    }

    public void setCalls(List<Integer> calls) {
        this.calls = calls;
    }

    public List<Integer> getHits() {
        return hits;
    }

    public void setHits(List<Integer> hits) {
        this.hits = hits;
    }

    public List<Integer> getMisses() {
        return misses;
    }

    public void setMisses(List<Integer> misses) {
        this.misses = misses;
    }

    public Map<String, MergedMemoiEntry> getCounts() {
        return counts;
    }

    public void setCounts(Map<String, MergedMemoiEntry> counts) {
        this.counts = counts;
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setOutputTypes(List<String> outputTypes) {
        this.outputTypes = outputTypes;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

    public void printStats() {

        System.out.println("\n\n=== profile stats ===");
        System.out.println("target function: " + funcSig);
        System.out.println("call sites: " + callSites);
        System.out.println("report count: " + reportCount);

        // stats.print();
        File d = SpecsIo.mkdir("./memoi-report-stats/" + funcSig);
        File f = new File(d, callSites.toString());
        SpecsIo.write(f, stats.toString());
    }

    public void makeStats() {
        this.stats = new Stats(this);
    }

    public String getUuid() {
        return uuid;
    }

}
