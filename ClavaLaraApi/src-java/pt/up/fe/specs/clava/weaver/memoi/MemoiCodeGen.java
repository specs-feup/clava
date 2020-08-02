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
import java.util.stream.Collectors;

public class MemoiCodeGen {

    private static final String NAN_BITS = "fff8000000000000";

    /**
     * 
     * list of param names
     * 
     * @param numSets
     * @param report
     * @param func
     * 
     * 
     * @return
     */
    public static String generateDmtCode(int numSets, List<String> paramNames, boolean isMemoiEmpty,
            boolean isMemoiOnline, int memoiApproxBits, MergedMemoiReport report, int inputCount, int outputCount,
            List<String> inputTypes, List<String> outputTypes) {

        Map<String, MergedMemoiEntry> table = new HashMap<String, MergedMemoiEntry>();
        if (!isMemoiEmpty) {
            table = new DirectMappedTable(report, numSets).generate();
        }

        return generateDmtCode(table, numSets, paramNames, isMemoiOnline, memoiApproxBits, inputCount, outputCount,
                inputTypes, outputTypes);
    }

    private static String generateDmtCode(Map<String, MergedMemoiEntry> table, int numSets,
            List<String> paramNames, boolean isMemoiOnline, int memoiApproxBits, int inputCount, int outputCount,
            List<String> inputTypes, List<String> outputTypes) {

        String tableCode = dmtCode(table, inputCount, outputCount, numSets, isMemoiOnline);

        String logicCode = dmtLogicCode(paramNames, numSets, memoiApproxBits, inputCount, outputCount,
                inputTypes, outputTypes);

        return tableCode + "\n\n" + logicCode;
    }

    public static String generateUpdateCode(int numSets, List<String> paramNames,
            boolean isUpdateAlways, int inputCount, int outputCount) {

        int indexBits = (int) MemoiUtils.log2(numSets);

        return updateCode(inputCount, outputCount, indexBits, paramNames, isUpdateAlways);
    }

    private static List<String> makeVarNames(List<String> paramNames) {

        return paramNames.stream().map(s -> s + "_bits").collect(Collectors.toList());
    }

    private static String dmtLogicCode(List<String> paramNames, int numSets,
            int memoiApproxBits, int inputCount, int outputCount, List<String> inputTypes, List<String> outputTypes) {

        StringBuilder code = new StringBuilder();

        int indexBits = (int) MemoiUtils.log2(numSets);

        int maxVarBits = varBitsCode(paramNames, code, memoiApproxBits, inputCount, inputTypes);

        mergeBitsCode(code, paramNames, maxVarBits, indexBits, inputCount);

        lookupCode(code, indexBits, paramNames, inputCount, outputCount, outputTypes);

        return code.toString();
    }

    private static String updateCode(int inputCount, int outputCount, int indexBits,
            List<String> paramNames, boolean isUpdateAlways) {

        StringBuilder code = new StringBuilder();

        var varNames = makeVarNames(paramNames);

        List<String> inputUpdates = new ArrayList<>();
        StringBuilder access = new StringBuilder("table[hash_").append(indexBits).append("_bits]");

        for (int v = 0; v < inputCount; v++) {

            StringBuilder inputUpdate = new StringBuilder(access);
            inputUpdate.append("[");
            inputUpdate.append(v);
            inputUpdate.append("] = ");
            inputUpdate.append(varNames.get(v));
            inputUpdate.append(";");

            inputUpdates.add(inputUpdate.toString());
        }
        code.append(String.join("\n", inputUpdates));
        code.append("\n");

        if (outputCount == 1) {

            code.append(access);
            code.append("[");
            code.append(varNames.size());
            code.append("] = *(uint64_t*) &result;");

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
            code.append("\treturn;\n\n");
        }

        if (!isUpdateAlways) {
            StringBuilder condition = new StringBuilder("if(");
            condition.append(access);
            condition.append("[0] == 0x");
            condition.append(NAN_BITS);
            condition.append(") {\n");

            code.insert(0, condition);
            code.append("\n}\n");
        }

        return code.toString();
    }

    private static void lookupCode(StringBuilder code, int indexBits,
            List<String> paramNames, int inputCount, int outputCount, List<String> outputTypes) {

        code.append("\nif(");

        List<String> varNames = makeVarNames(paramNames);

        List<String> testClauses = new ArrayList<>();
        StringBuilder access = new StringBuilder("table[hash_").append(indexBits).append("_bits]");

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
            code.append(" *) &table[hash_");
            code.append(indexBits);
            code.append("_bits][");
            code.append(varNames.size());
            code.append("];\n}\n");
        } else {

            for (int o = 0; o < outputCount; o++) {

                code.append("\t*");
                code.append(paramNames.get(o + inputCount));
                code.append(" = *(");
                code.append(outputTypes.get(o));
                code.append(" *) &table[hash_");
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
            code.append(small);
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
            boolean isMemoiOnline) {

        StringBuilder code = new StringBuilder("static");
        if (!isMemoiOnline) {
            code.append(" const");
        }
        code.append(" uint64_t table[");

        code.append(numSets);
        code.append("][");
        code.append(inputCount + outputCount);
        code.append("] = {\n");

        for (int i = 0; i < numSets; i++) {

            code.append("\t{");

            // String key = Integer.toString(i, BASE);
            String key = String.format("%04x", i);

            MergedMemoiEntry entry = table.get(key);

            final String H = "0x";
            if (entry == null) {

                for (int ic = 0; ic < inputCount; ic++) {
                    code.append(H);
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
        code.append("};");

        return code.toString();
    }
}
