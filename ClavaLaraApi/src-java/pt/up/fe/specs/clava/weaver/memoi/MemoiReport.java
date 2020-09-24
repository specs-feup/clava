package pt.up.fe.specs.clava.weaver.memoi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.io.PathFilter;

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

public class MemoiReport implements java.io.Serializable {

    private static final long serialVersionUID = 2478911300859975647L;

    private String uuid;
    private String id;
    private String funcSig;
    private int inputCount;
    private int outputCount;
    private List<String> inputTypes;
    private List<String> outputTypes;
    private List<String> call_sites;

    private int elements;
    private int calls;
    private int hits;
    private int misses;

    private Map<String, MemoiEntry> counts; // maps a key to a memoi entry

    public static MergedMemoiReport mergeReportsFromNames(List<String> fileNames) {

        return mergeReportsFromNames(fileNames, false);
    }

    public static MergedMemoiReport mergeReportsFromNames(List<String> fileNames, boolean check) {

        MergedMemoiReport report = null;

        for (String fileName : fileNames) {

            MemoiReport tempReport = mergePartsFromName(fileName);

            if (report == null) {

                report = new MergedMemoiReport(tempReport);
            } else {

                report.mergeReport(tempReport, check);
            }
        }

        // report.makeStats();
        report.printStats();
        return report;
    }

    public static MemoiReport mergePartsFromName(String fileName) {

        File file = new File(fileName);

        SpecsCheck.checkArgument(file.exists(), () -> "the file " + fileName + " doesn't exist");

        // get the final report
        MemoiReport finalReport = fromFile(file);

        // get the other reports and merge them
        File parentDir = SpecsIo.getParent(file);
        String pattern = file.getName() + ".part*";
        List<File> partFiles = SpecsIo.getPathsWithPattern(parentDir, pattern, false, PathFilter.FILES);

        for (var partFile : partFiles) {

            MemoiReport partReport = fromFile(partFile);
            finalReport.mergePart(partReport);
        }

        // calculate the correct number of elements, misses, and hits
        finalReport.setElements(finalReport.getCounts().size());
        finalReport.setMisses(finalReport.getElements());
        finalReport.setHits(finalReport.getCalls() - finalReport.getMisses());

        return finalReport;
    }

    public static MemoiReport fromFile(File file) {

        SpecsCheck.checkArgument(file.exists(), () -> "the file " + file + " doesn't exist");

        FileReader fr = null;
        try {
            fr = new FileReader(file);
        } catch (FileNotFoundException e) {
            SpecsLogs.warn("Could not find the file " + file.getAbsolutePath());
        }

        BufferedReader br = new BufferedReader(fr);
        MemoiReport fromJson = new Gson().fromJson(br, MemoiReport.class);
        return fromJson;
    }

    /**
     * The number of calls is correct after each merge. The number of hits and misses is not and is calculated at the
     * end based on the number of elements.
     * 
     * @param otherReport
     */
    private void mergePart(MemoiReport otherReport) {

        // Map<String, MemoiEntry> tempMap = makeMap();
        //
        // for (MemoiEntry entry : otherReport.counts) {
        //
        // var key = entry.getKey();
        // if (tempMap.containsKey(key)) {
        //
        // tempMap.get(key).inc(entry.getCounter());
        // } else {
        //
        // tempMap.put(key, new MemoiEntry(entry));
        // }
        // }
        // counts = new ArrayList<MemoiEntry>(tempMap.values());

        otherReport.counts.forEach(
                (k, v) -> counts.merge(
                        k,
                        v,
                        (v1, v2) -> {
                            v1.inc(v2.getCounter());
                            return v1;
                        }));

        this.calls += otherReport.calls;
        // this.elements += otherReport.elements;
        // this.misses += otherReport.misses;
        // this.hits += otherReport.hits;
    }

    // private Map<String, MemoiEntry> makeMap() {
    //
    // Map<String, MemoiEntry> map = new HashMap<>(this.counts.size());
    //
    // counts.stream().forEach(e -> map.put(e.getKey(), e));
    //
    // return map;
    // }

    public void toJson(String fileName) {

        String json = new Gson().toJson(this);
        SpecsIo.write(new File(fileName), json);
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

    public void setOutputTypes(List<String> outputTypes) {
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

    public List<String> getCall_sites() {
        return call_sites;
    }

    public void setCall_sites(List<String> call_sites) {
        this.call_sites = call_sites;
    }

    public int getElements() {
        return elements;
    }

    public void setElements(int elements) {
        this.elements = elements;
    }

    public int getCalls() {
        return calls;
    }

    public void setCalls(int calls) {
        this.calls = calls;
    }

    public int getHits() {
        return hits;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public int getMisses() {
        return misses;
    }

    public void setMisses(int misses) {
        this.misses = misses;
    }

    public Map<String, MemoiEntry> getCounts() {
        return counts;
    }

    public void setCounts(Map<String, MemoiEntry> counts) {
        this.counts = counts;
    }

    public String getUuid() {
        return uuid;
    }
}
