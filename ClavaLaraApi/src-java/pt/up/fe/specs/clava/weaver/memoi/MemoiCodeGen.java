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
import java.util.List;
import java.util.Map;

public class MemoiCodeGen {

    private static final int BASE = 16;

    public static String generateDirectMappedTableCode(MergedMemoiReport report, int numSets) {

        DirectMappedTable dmt = new DirectMappedTable(report, numSets);
        var table = dmt.generate();

        return generateDirectMappedTableCode(table, report, numSets);
    }

    private static String generateDirectMappedTableCode(Map<String, MergedMemoiEntry> table, MergedMemoiReport report,
            int numSets) {

        String tableCode = dmtCode(table, report, numSets);

        // String logicCode = dmtLogicCode();

        return tableCode;
    }

    private static String dmtLogicCode() {

        StringBuilder code = new StringBuilder();

        int maxVarBits = 0;
        List<String> varNames = new ArrayList<>();

        return code.toString();
    }

    private static String dmtCode(Map<String, MergedMemoiEntry> table, MergedMemoiReport report, int numSets) {

        String nanBits = "fff8000000000000";
        int inputCount = report.getInputCount();

        StringBuilder code = new StringBuilder("static const uint64_t table[");
        code.append(numSets);
        code.append("][");
        code.append(inputCount + 1);
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
            } else {

                String fullKey = entry.getKey();
                String[] keys = fullKey.split("#");
                for (int k = 0; k < keys.length; k++) {
                    code.append(H);
                    code.append(keys[k]);
                    code.append(", ");
                }
                code.append(H);
                code.append(entry.getOutput());
            }
            code.append("},\n");
        }
        code.append("};");

        return code.toString();
    }
}
