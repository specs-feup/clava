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

public class MemoiCodeGen {

    private static final int BASE = 16;

    /**
     * 
     * list of param names
     * 
     * 
     * @param report
     * @param numSets
     * @param func
     * @return
     */
    public static String generateDirectMappedTableCode(MergedMemoiReport report, int numSets, List<String> paramNames) {

        var table = new DirectMappedTable(report, numSets).generate();

        return generateDirectMappedTableCode(table, report, numSets, paramNames);
    }

    private static String generateDirectMappedTableCode(Map<String, MergedMemoiEntry> table, MergedMemoiReport report,
            int numSets, List<String> paramNames) {

        String tableCode = dmtCode(table, report, numSets);

        String logicCode = dmtLogicCode(report, paramNames, numSets);

        return tableCode + "\n\n" + logicCode;
    }

    private static String dmtLogicCode(MergedMemoiReport report, List<String> paramNames, int numSets) {

        StringBuilder code = new StringBuilder();
        List<String> varNames = new ArrayList<>();
        int indexBits = (int) MemoiUtils.log2(numSets);

        int maxVarBits = varBitsCode(report, paramNames, code, varNames);

        mergeBitsCode(code, varNames, maxVarBits, indexBits);

        lookupCode(report, code, varNames, indexBits, paramNames);

        return code.toString();
    }

    private static void lookupCode(MergedMemoiReport report, StringBuilder code, List<String> varNames, int indexBits,
            List<String> paramNames) {
        code.append("\nif(");

        List<String> testClauses = new ArrayList<>();
        StringBuilder access = new StringBuilder("table[hash_").append(indexBits).append("_bits]");

        for (int v = 0; v < varNames.size(); v++) {

            StringBuilder testClause = new StringBuilder(access);
            testClause.append("[");
            testClause.append(v);
            testClause.append("] == ");
            testClause.append(varNames.get(v));

            testClauses.add(testClause.toString());
        }
        code.append(String.join(" && ", testClauses));

        final int outputCount = report.getOutputCount();
        final int inputCount = report.getInputCount();

        code.append(") {\n");

        if (outputCount == 1) {

            code.append("\treturn *(");
            code.append(report.getOutputTypes().get(0));
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
                code.append(report.getOutputTypes().get(o));
                code.append(" *) &table[hash_");
                code.append(indexBits);
                code.append("_bits][");
                code.append(o + inputCount);
                code.append("];\n");
            }
            code.append("\treturn;\n}\n");
        }
    }

    private static void mergeBitsCode(StringBuilder code, List<String> varNames, int maxVarBits, int indexBits) {

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

    private static int varBitsCode(MergedMemoiReport report, List<String> paramNames, StringBuilder code,
            List<String> varNames) {

        Map<String, Integer> sizeMap = new HashMap<>();
        sizeMap.put("float", 32);
        sizeMap.put("int", 32);
        sizeMap.put("double", 64);

        int maxVarBits = 0;

        for (int p = 0; p < report.getInputCount(); p++) {

            String paramName = paramNames.get(p);

            String paramType = report.getInputTypes().get(p);
            int varBits = sizeMap.get(paramType);

            if (varBits > maxVarBits) {
                maxVarBits = varBits;
            }

            String varName = paramName + "_bits";
            varNames.add(varName);

            code.append("uint");
            code.append(varBits);
            code.append("_t ");
            code.append(varName);
            code.append(" = *(uint");
            code.append(varBits);
            code.append("_t*)&");
            code.append(paramName);
            code.append(";\n");
        }

        return maxVarBits;
    }

    private static String dmtCode(Map<String, MergedMemoiEntry> table, MergedMemoiReport report, int numSets) {

        String nanBits = "fff8000000000000";
        int inputCount = report.getInputCount();
        int outputCount = report.getOutputCount();

        StringBuilder code = new StringBuilder("static const uint64_t table[");
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
                    code.append(nanBits);
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
