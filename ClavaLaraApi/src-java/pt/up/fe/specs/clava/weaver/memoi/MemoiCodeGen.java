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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.weaver.memoi.policy.apply.DmtRepetition;
import pt.up.fe.specs.clava.weaver.memoi.policy.apply.SimplePolicies;

public class MemoiCodeGen {

    private static final String H = "0x";
    private static final String NAN_BITS = "0xfff8000000000000";
    private static Map<String, Predicate<DirectMappedTable>> policyMap;
    static {
        policyMap = new HashMap<>();
        policyMap.put("ALWAYS", SimplePolicies.ALWAYS);
        policyMap.put("NOT_EMPTY", SimplePolicies.NOT_EMPTY);
        policyMap.put("OVER_25_PCT", DmtRepetition.OVER_25_PCT);
        policyMap.put("OVER_50_PCT", DmtRepetition.OVER_50_PCT);
        policyMap.put("OVER_75_PCT", DmtRepetition.OVER_75_PCT);
        policyMap.put("OVER_90_PCT", DmtRepetition.OVER_90_PCT);
    }

    /**
     * Generates the {@link DirectMappedTable} based on the report.
     * 
     * @param numSets
     * @param report
     * @param applyPolicyName
     * @return
     */
    public static DirectMappedTable generateDmt(int numSets, MergedMemoiReport report, String applyPolicyName) {

        Predicate<DirectMappedTable> applyPolicy = policyMap.get(applyPolicyName);
        if (applyPolicy == null) {

            throw new RuntimeException("Could not translate the apply policy '" + applyPolicyName + "'.");
        }

        return DirectMappedTable.fromApplyPolicy(report, numSets, applyPolicy);
    }

    /**
     * Generates the table code as a static global, which is defined above the wrapper that uses it. If necessary, a
     * reset function is generated and defined below the table.
     * 
     * @param table
     * @param numSets
     * @param inputCount
     * @param outputCount
     * @param isMemoiOnline
     * @param isReset
     * @param isEmpty
     * @param tablePrefix
     * @return
     */
    public static String generateTableCode(Map<String, MergedMemoiEntry> table, int numSets, int inputCount,
            int outputCount, boolean isMemoiOnline, boolean isReset, boolean isEmpty, String tablePrefix) {

        if (isEmpty) {

            table = new HashMap<String, MergedMemoiEntry>();
        }

        return dmtCode(table, inputCount, outputCount, numSets, isMemoiOnline, isReset, tablePrefix);
    }

    /**
     * Generates the logic code that uses the global static table.
     * 
     * @param numSets
     * @param paramNames
     * @param memoiApproxBits
     * @param inputCount
     * @param outputCount
     * @param inputTypes
     * @param outputTypes
     * @param tablePrefix
     * @return
     */
    public static String generateLogicCode(int numSets,
            List<String> paramNames, int memoiApproxBits, int inputCount, int outputCount,
            List<String> inputTypes, List<String> outputTypes, String tablePrefix) {

        return dmtLogicCode(paramNames, numSets, memoiApproxBits, inputCount, outputCount,
                inputTypes, outputTypes, tablePrefix);
    }

    /**
     * Generates a reset function that is defined after the table.
     * 
     * @param tableSize
     * @param tablePrefix
     * @return
     */
    private static String resetCode(int tableSize, String tablePrefix) {

        StringBuilder b = new StringBuilder("\nvoid ");
        b.append(tablePrefix);
        b.append("reset (void) {\n");
        b.append("for(int i = 0; i < ");
        b.append(tableSize);
        b.append("; i++) {\n");
        b.append(tablePrefix);
        b.append("table[i][0] = ");
        b.append(NAN_BITS);
        b.append(";\n}\n");
        b.append("}\n");

        return b.toString();
    }

    /**
     * Generates the update logic.
     * 
     * @param numSets
     * @param paramNames
     * @param isUpdateAlways
     * @param inputCount
     * @param outputCount
     * @param updatesName
     * @param isZeroSim
     * @param tablePrefix
     * @return
     */
    public static String generateUpdateCode(int numSets, List<String> paramNames,
            boolean isUpdateAlways, int inputCount, int outputCount, String updatesName, boolean isZeroSim,
            String tablePrefix) {

        int indexBits = (int) MemoiUtils.log2(numSets);

        return updateCode(inputCount, outputCount, indexBits, paramNames, isUpdateAlways, updatesName, isZeroSim,
                tablePrefix);
    }

    private static List<String> makeVarNames(List<String> paramNames) {

        return paramNames.stream().map(s -> s + "_bits").collect(Collectors.toList());
    }

    private static String dmtLogicCode(List<String> paramNames, int numSets,
            int memoiApproxBits, int inputCount, int outputCount, List<String> inputTypes, List<String> outputTypes,
            String tablePrefix) {

        StringBuilder code = new StringBuilder();

        int indexBits = (int) MemoiUtils.log2(numSets);

        int maxVarBits = varBitsCode(paramNames, code, memoiApproxBits, inputCount, inputTypes);

        mergeBitsCode(code, paramNames, maxVarBits, indexBits, inputCount);

        lookupCode(code, indexBits, paramNames, inputCount, outputCount, outputTypes, tablePrefix);

        return code.toString();
    }

    private static String updateCode(int inputCount, int outputCount, int indexBits,
            List<String> paramNames, boolean isUpdateAlways, String updatesName, boolean isZeroSim,
            String tablePrefix) {

        String tableName = tablePrefix + "table";

        StringBuilder code = new StringBuilder();

        var varNames = makeVarNames(paramNames);

        List<String> inputUpdates = new ArrayList<>();
        StringBuilder access = new StringBuilder(tableName).append("[hash_").append(indexBits).append("_bits]");

        for (int v = 0; v < inputCount; v++) {

            StringBuilder inputUpdate = new StringBuilder(access);
            inputUpdate.append("[");
            inputUpdate.append(v);
            inputUpdate.append("] = ");
            if (isZeroSim) {
                inputUpdate.append(NAN_BITS); // for 0% -> assign NaN
            } else {
                inputUpdate.append(varNames.get(v));
            }
            inputUpdate.append(";");

            inputUpdates.add(inputUpdate.toString());
        }
        code.append(String.join("\n", inputUpdates));
        code.append("\n");

        if (outputCount == 1) {

            code.append(access);
            code.append("[");
            code.append(inputCount);
            code.append("] = *(uint64_t*) &result;");

            code.append("\n");
            if (updatesName != null) {
                code.append(updatesName);
                code.append("++;\n");
            }

        } else {

            for (int o = 0; o < outputCount; o++) {

                int outputIndex = o + inputCount;

                code.append(access);
                code.append("[");
                code.append(outputIndex);
                code.append("] = *(uint64_t*) ");
                code.append(paramNames.get(outputIndex));
                code.append(";\n");
            }

            code.append("\n");
            if (updatesName != null) {
                code.append(updatesName);
                code.append("++;\n");
            }

            code.append("\treturn;\n\n");
        }

        if (!isUpdateAlways) {
            StringBuilder condition = new StringBuilder("if(");
            condition.append(access);
            condition.append("[0] == ");
            condition.append(NAN_BITS);
            condition.append(") {\n");

            code.insert(0, condition);
            code.append("\n}\n");
        }

        return code.toString();
    }

    private static void lookupCode(StringBuilder code, int indexBits,
            List<String> paramNames, int inputCount, int outputCount, List<String> outputTypes, String tablePrefix) {

        String tableName = tablePrefix + "table";

        code.append("\nif(");

        List<String> varNames = makeVarNames(paramNames);

        List<String> testClauses = new ArrayList<>();
        StringBuilder access = new StringBuilder(tableName).append("[hash_").append(indexBits).append("_bits]");

        for (int v = 0; v < inputCount; v++) {

            StringBuilder testClause = new StringBuilder(access);
            testClause.append("[");
            testClause.append(v);
            testClause.append("] == ");
            testClause.append(varNames.get(v));

            testClauses.add(testClause.toString());
        }
        code.append(String.join(" && ", testClauses));

        code.append(") {\n");

        if (outputCount == 1) {

            code.append("\treturn *(");
            code.append(outputTypes.get(0));
            code.append(" *) &");
            code.append(tableName);
            code.append("[hash_");
            code.append(indexBits);
            code.append("_bits][");
            code.append(inputCount);
            code.append("];\n}\n");
        } else {

            for (int o = 0; o < outputCount; o++) {

                code.append("\t*");
                code.append(paramNames.get(o + inputCount));
                code.append(" = *(");
                code.append(outputTypes.get(o));
                code.append(" *) &");
                code.append(tableName);
                code.append("[hash_");
                code.append(indexBits);
                code.append("_bits][");
                code.append(o + inputCount);
                code.append("];\n");
            }
            code.append("\treturn;\n}\n");
        }
    }

    private static void mergeBitsCode(StringBuilder code, List<String> paramNames,
            int maxVarBits, int indexBits, int inputCount) {

        List<String> varNames = makeVarNames(paramNames).subList(0, inputCount);

        code.append("\n");
        code.append("uint");
        code.append(maxVarBits);
        code.append("_t hash_");
        code.append(maxVarBits);
        code.append("_bits = ");
        code.append(String.join(" ^ ", varNames));
        code.append(";\n");

        double iters = (int) MemoiUtils.log2(maxVarBits / indexBits);
        int intIters = (int) Math.floor(iters);

        int large = maxVarBits;
        int small = 0;
        for (int i = 0; i < intIters; i++) {

            small = large / 2;

            code.append("uint");
            code.append(maxVarBits); // always use the same type and perform && at the end
            code.append("_t hash_");
            code.append(small);
            code.append("_bits = (hash_");
            code.append(large);
            code.append("_bits ^ (hash_");
            code.append(large);
            code.append("_bits >> ");
            code.append(small);
            code.append("));\n");

            large /= 2;
        }

        // if not integer, we need to mask bits at the end
        if (iters != intIters) {

            code.append("uint");
            code.append(small);
            code.append("_t mask = 0xffff >> (16 - ");
            code.append(indexBits);
            code.append(");\n");

            code.append("uint");
            code.append(small);
            code.append("_t hash_");
            code.append(indexBits);
            code.append("_bits = hash_");
            code.append(small);
            code.append("bits & mask;\n");
        } else { // mask for small, since a larger type is used and the values are not truncated

            StringBuilder mask = new StringBuilder(H);
            for (int i = 0; i < indexBits / 4; i++) {

                mask.append("f");
            }

            code.append("hash_");
            code.append(indexBits);
            code.append("_bits = hash_");
            code.append(indexBits);
            code.append("_bits & ");
            code.append(mask);
            code.append(";\n");
        }
    }

    private static int varBitsCode(List<String> paramNames, StringBuilder code,
            int memoiApproxBits, int inputCount, List<String> inputTypes) {

        Map<String, Integer> sizeMap = new HashMap<>();
        sizeMap.put("float", 32);
        sizeMap.put("int", 32);
        sizeMap.put("double", 64);

        List<String> varNames = makeVarNames(paramNames);

        int maxVarBits = 0;

        for (int p = 0; p < inputCount; p++) {

            String paramName = paramNames.get(p);

            String paramType = inputTypes.get(p);
            int varBits = sizeMap.get(paramType);

            if (varBits > maxVarBits) {
                maxVarBits = varBits;
            }

            code.append("uint");
            code.append(varBits);
            code.append("_t ");
            code.append(varNames.get(p));
            code.append(" = *(uint");
            code.append(varBits);
            code.append("_t*)&");
            code.append(paramName);
            code.append(";\n");

            if (memoiApproxBits != 0) {

                code.append(varNames.get(p));
                code.append(" = ");
                code.append(varNames.get(p));
                code.append(" & (");
                code.append("(0xffffffffffffffff >> ");
                code.append(memoiApproxBits);
                code.append(") << ");
                code.append(memoiApproxBits);
                code.append(");\n");
            }
        }

        return maxVarBits;
    }

    private static String dmtCode(Map<String, MergedMemoiEntry> table, int inputCount, int outputCount, int numSets,
            boolean isMemoiOnline, boolean isReset, String tablePrefix) {

        StringBuilder code = new StringBuilder("static");
        if (!isMemoiOnline) {
            code.append(" const");
        }
        code.append(" uint64_t ");
        code.append(tablePrefix);
        code.append("table[");

        code.append(numSets);
        code.append("][");
        code.append(inputCount + outputCount);
        code.append("] = {\n");

        for (int i = 0; i < numSets; i++) {

            code.append("\t{");

            // String key = Integer.toString(i, BASE);
            String key = String.format("%04x", i);

            MergedMemoiEntry entry = table.get(key);

            if (entry == null) {

                for (int ic = 0; ic < inputCount; ic++) {
                    code.append(NAN_BITS);
                    code.append(", ");
                }

                code.append(0);
                for (int oc = 1; oc < outputCount; oc++) {
                    code.append(", ");
                    code.append(0);
                }
            } else {

                String fullKey = entry.getKey();
                String[] keys = fullKey.split("#");
                for (int k = 0; k < keys.length; k++) {
                    code.append(H);
                    code.append(keys[k]);
                    code.append(", ");
                }

                String fullOutputString = entry.getOutput();
                String[] outputStrings = fullOutputString.split("#");

                code.append(H);
                code.append(outputStrings[0]);
                for (int o = 1; o < outputCount; o++) {
                    code.append(", ");
                    code.append(H);
                    code.append(outputStrings[o]);
                }
            }
            code.append("},\n");
        }
        code.append("};\n");

        if (isReset) {

            code.append(resetCode(numSets, tablePrefix));
        }

        return code.toString();
    }
}
