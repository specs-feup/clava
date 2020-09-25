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

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import pt.up.fe.specs.JacksonPlus.SpecsJackson;
import pt.up.fe.specs.clava.weaver.memoi.comparator.MeanComparator;
import pt.up.fe.specs.clava.weaver.memoi.policy.insert.AlwaysInsert;
import pt.up.fe.specs.util.SpecsCheck;

public class DirectMappedTable implements java.io.Serializable {

    private static final long serialVersionUID = 3951666310732380835L;

    private static final List<Integer> ALLOWED_SIZES = Arrays.asList(256, 512, 1024, 2048, 4096, 8192, 16384, 32768,
            65536);
    private static final int BASE = 16;
    private static final int KEY_STRING_LENGTH = 16;
    private MergedMemoiReport report;
    private int indexBits;
    private int numSets;
    private Predicate<MergedMemoiEntry> insertPred;
    private Comparator<MergedMemoiEntry> countComparator;

    private Map<String, MergedMemoiEntry> table;
    int totalCollisions;
    int totalElements;
    int maxCollision;

    private boolean debug;

    public DirectMappedTable() {
    }

    /**
     * Defaults to using mean comparator.
     * 
     * @param report
     * @param indexBits
     * @param numSets
     * @param insertPred
     */
    public DirectMappedTable(MergedMemoiReport report, int numSets,
            Predicate<MergedMemoiEntry> insertPred) {

        this(report, numSets, insertPred, new MeanComparator(report));
    }

    /**
     * Defaults to using the ALWAYS predicate.
     * 
     * @param report
     * @param indexBits
     * @param numSets
     * @param countComparator
     */
    public DirectMappedTable(MergedMemoiReport report, int numSets,
            Comparator<MergedMemoiEntry> countComparator) {

        this(report, numSets, new AlwaysInsert(), countComparator);
    }

    /**
     * Defaults to using the ALWAYS predicate and the mean comparator.
     * 
     * @param report
     * @param indexBits
     * @param numSets
     */
    public DirectMappedTable(MergedMemoiReport report, int numSets) {

        this(report, numSets, new AlwaysInsert(), new MeanComparator(report));
    }

    public DirectMappedTable(MergedMemoiReport report, int numSets,
            Predicate<MergedMemoiEntry> insertPred,
            Comparator<MergedMemoiEntry> countComparator) {

        int maxSize = ALLOWED_SIZES.get(ALLOWED_SIZES.size() - 1);

        SpecsCheck.checkArgument(numSets <= maxSize, () -> "TableGenerator: tableSize > " + maxSize);
        SpecsCheck.checkArgument(ALLOWED_SIZES.contains(numSets),
                () -> "TableGenerator: tableSize not allowed, choose one of " + ALLOWED_SIZES);

        this.report = report;
        this.indexBits = (int) MemoiUtils.log2(numSets);
        this.insertPred = insertPred;
        this.countComparator = countComparator;
        this.numSets = numSets;

        this.debug = false;

        this.table = generateTable();
        printTableReport();
    }

    public void setDebug() {
        setDebug(true);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isDebug() {
        return debug;
    }

    public Map<String, MergedMemoiEntry> getTable() {

        return table;
    }

    private HashMap<String, MergedMemoiEntry> generateTable() {

        var table = new HashMap<String, MergedMemoiEntry>();

        List<MergedMemoiEntry> counts = report.getSortedCounts(countComparator.reversed());

        for (var count : counts) {

            if (!insertPred.test(count)) {
                continue;
            }

            String combinedKey = makeHashKey(count);

            String hash = hash(combinedKey);

            if (!table.containsKey(hash)) {

                table.put(hash, count);
            } else {

                // collisions in this position
                table.get(hash).incCollisions();

                int thisCollisions = table.get(hash).getCollisions();
                if (thisCollisions > maxCollision) {
                    maxCollision = thisCollisions;
                }

                // overall collisions
                totalCollisions++;

                continue;
            }
        }

        totalElements = (int) MemoiUtils.mean(report.getElements(), report.getReportCount());

        return table;
    }

    public void printTableReport() {

        int reportCount = report.getReportCount();

        int tableElements = table.size();

        double tableCalls = table.values().parallelStream()
                .map(MergedMemoiEntry::getCounter)
                .map(l -> MemoiUtils.mean(l, reportCount))
                .reduce(0.0, Double::sum);

        double capacity = 100.0 * table.size() / numSets;
        double totalCalls = MemoiUtils.mean(report.getCalls(), reportCount);
        double collisionPercentage = 100.0 * totalCollisions / totalElements;
        double elementCoverage = 100.0 * tableElements / totalElements;
        double callCoverage = 100.0 * tableCalls / totalCalls;

        Locale l = null;
        System.out.println("\n\n=== table stats ===");
        System.out.println("table capacity: " + table.size() + "/" + numSets + " ("
                + String.format(l, "%.2f", capacity) + "%)");
        System.out.println("collisions: " + totalCollisions + "/" + totalElements + " ("
                + String.format(l, "%.2f", collisionPercentage) + "%)");
        System.out.println("largest collision: " + maxCollision);
        System.out.println("element coverage: " + tableElements + "/" + totalElements + " ("
                + String.format(l, "%.2f", elementCoverage) + "%)");
        System.out.println(
                "call coverage: " + tableCalls + "/" + totalCalls + " (" + String.format(l, "%.2f", callCoverage)
                        + "%)");
    }

    private void printTable(HashMap<String, MergedMemoiEntry> table) {

        table.forEach((k, v) -> {

            StringBuilder b = new StringBuilder();

            for (String key : v.getKey().split("#")) {

                b.append("0x");
                b.append(key);
                b.append(", ");
            }

            b.append("0x");
            b.append(v.getOutput());

            System.out.println(b);
        });
    }

    private String hash(String key64bits) {

        int varBits = 64;

        double iters = MemoiUtils.log2(varBits / indexBits);
        int intIters = (int) iters;

        String hash = key64bits;

        for (int i = 0; i < intIters; i++) {
            hash = halfHash(hash);
        }

        // if not integer, we need to mask bits at the end
        if (iters != intIters) {

            // mask starts with 16 bits
            var mask = Integer.parseUnsignedInt("0xffff", 16);
            var shift = 16 - indexBits;
            mask = mask >> (shift);

            var hashInt = Integer.parseUnsignedInt(hash, 16);

            hashInt = hashInt & mask;
            return Integer.toString(hashInt, BASE);
        }

        return hash;
    }

    private String halfHash(String hash) {

        int length = hash.length();
        int halfLength = length / 2;
        StringBuilder newHash = new StringBuilder();

        for (int i = 0; i < halfLength; i++) {

            String highChar = hash.substring(i, i + 1);
            Integer high = Integer.parseUnsignedInt(highChar, BASE);
            String lowChar = hash.substring(i + halfLength, i + halfLength + 1);
            Integer low = Integer.parseUnsignedInt(lowChar, BASE);

            newHash.append(Integer.toString(high ^ low, BASE));
        }

        return newHash.toString();
    }

    private String makeHashKey(MergedMemoiEntry count) {

        String fullKey = count.getKey();

        var keys = fullKey.split("#");

        if (keys.length == 1) {
            return fullKey;
        } else {
            return combineKeys(keys);
        }
    }

    private String combineKeys(String[] keys) {

        StringBuilder combinedKey = new StringBuilder();

        int numKeys = keys.length;

        for (int charIx = 0; charIx < KEY_STRING_LENGTH; charIx++) {

            String currentChar = keys[0].substring(charIx, charIx + 1);
            Integer currentNumber = Integer.parseUnsignedInt(currentChar, BASE);

            for (int keyIx = 1; keyIx < numKeys; keyIx++) {

                String newChar = keys[keyIx].substring(charIx, charIx + 1);
                Integer newNumber = Integer.parseUnsignedInt(newChar, BASE);

                currentNumber = currentNumber ^ newNumber;
            }

            combinedKey.append(Integer.toString(currentNumber, BASE));
        }

        return combinedKey.toString();
    }

    public static DirectMappedTable load(File file) {

        // FileInputStream fileIn = new FileInputStream(file);
        // ObjectInputStream in = new ObjectInputStream(fileIn);
        // DirectMappedTable dmt = (DirectMappedTable) in.readObject();
        // in.close();
        // fileIn.close();

        return SpecsJackson.fromFile(file, DirectMappedTable.class);
    }

    public static void save(File file, DirectMappedTable dmt) {

        // FileOutputStream fileOut = new FileOutputStream(file);
        // ObjectOutputStream out = new ObjectOutputStream(fileOut);
        // out.writeObject(dmt);
        // out.close();
        // fileOut.close();

        SpecsJackson.toFile(dmt, file);
    }

    public MergedMemoiReport getReport() {
        return report;
    }

    public void setReport(MergedMemoiReport report) {
        this.report = report;
    }

    public int getIndexBits() {
        return indexBits;
    }

    public void setIndexBits(int indexBits) {
        this.indexBits = indexBits;
    }

    public int getNumSets() {
        return numSets;
    }

    public void setNumSets(int numSets) {
        this.numSets = numSets;
    }

    public Predicate<MergedMemoiEntry> getInsertPred() {
        return insertPred;
    }

    public void setInsertPred(Predicate<MergedMemoiEntry> insertPred) {
        this.insertPred = insertPred;
    }

    public Comparator<MergedMemoiEntry> getCountComparator() {
        return countComparator;
    }

    public void setCountComparator(Comparator<MergedMemoiEntry> countComparator) {
        this.countComparator = countComparator;
    }

    public int getTotalCollisions() {
        return totalCollisions;
    }

    public void setTotalCollisions(int totalCollisions) {
        this.totalCollisions = totalCollisions;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getMaxCollision() {
        return maxCollision;
    }

    public void setMaxCollision(int maxCollision) {
        this.maxCollision = maxCollision;
    }

    public void setTable(Map<String, MergedMemoiEntry> table) {
        this.table = table;
    }

}
