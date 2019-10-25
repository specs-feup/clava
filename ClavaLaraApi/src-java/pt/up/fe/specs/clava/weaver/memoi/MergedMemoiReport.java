package pt.up.fe.specs.clava.weaver.memoi;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pt.up.fe.specs.util.SpecsCheck;

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

public class MergedMemoiReport {

    private String id;
    private String funcSig;
    private String outputType;
    private int inputCount;
    private List<String> inputTypes;
    private List<String> call_sites;

    private List<Integer> elements;
    private List<Integer> calls;
    private List<Integer> hits;
    private List<Integer> misses;

    private Map<String, MergedMemoiEntry> counts;

    private int reportCount = 1;

    public MergedMemoiReport(MemoiReport report) {

        this.id = report.getId();
        this.funcSig = report.getFuncSig();
        this.outputType = report.getOutputType();
        this.inputCount = report.getInputCount();
        this.inputTypes = new ArrayList<>(report.getInputTypes());
        this.call_sites = new ArrayList<>(report.getCall_sites());
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

        elements.add(tempReport.getElements());
        calls.add(tempReport.getCalls());
        hits.add(tempReport.getHits());
        misses.add(tempReport.getMisses());

        for (MemoiEntry oldEntry : tempReport.getCounts()) {

            String key = oldEntry.getKey();

            if (!counts.containsKey(key)) {

                MergedMemoiEntry newEntry = new MergedMemoiEntry(oldEntry);

                counts.put(key, newEntry);
            }

            counts.get(key).addCounter(oldEntry.getCounter());
        }
    }

    private void testReport(MemoiReport tempReport) {

        SpecsCheck.checkArgument(this.funcSig.equals(tempReport.getFuncSig()),
                () -> "The function signatures of the reports are not equal");
        SpecsCheck.checkArgument(this.call_sites.equals(tempReport.getCall_sites()),
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

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public int getInputCount() {
        return inputCount;
    }

    public void setInputCount(int inputCount) {
        this.inputCount = inputCount;
    }

    public List<String> getInputTypes() {
        return inputTypes;
    }

    public void setInputTypes(List<String> inputTypes) {
        this.inputTypes = inputTypes;
    }

    public List<String> getCall_sites() {
        return call_sites;
    }

    public void setCall_sites(List<String> call_sites) {
        this.call_sites = call_sites;
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

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

}
