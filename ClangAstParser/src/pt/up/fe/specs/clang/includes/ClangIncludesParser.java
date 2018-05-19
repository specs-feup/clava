/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clang.includes;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;

class ClangIncludesParser {

    private static final Map<String, BiFunction<String, Include, Include>> PARTS_PARSER;
    static {
        PARTS_PARSER = new HashMap<>();

        PARTS_PARSER.put("source",
                (string, include) -> include.setSourceFile(SpecsIo.getCanonicalFile(new File(string))));
        PARTS_PARSER.put("include", (string, include) -> include.setInclude(string));
        PARTS_PARSER.put("line", (string, include) -> include.setLine(Integer.parseInt(string)));
        PARTS_PARSER.put("angled", (string, include) -> include.setAngled(string.equals("1") ? true : false));
    }

    public Optional<Include> parse(String line) {
        // Split line using |
        String[] parts = line.split("\\|");

        Include currentInclude = Include.empty();
        for (String part : parts) {
            Optional<Include> newInclude = parsePart(part, currentInclude);

            if (!newInclude.isPresent()) {
                return newInclude;
            }

            currentInclude = newInclude.get();
        }

        return Optional.of(currentInclude);
    }

    private static Optional<Include> parsePart(String part, Include currentInclude) {
        // Get prefix
        int colonIndex = part.indexOf(":");
        if (colonIndex == -1) {
            SpecsLogs.msgWarn("Could not find ':' in '" + part + "'");
            return Optional.empty();
        }

        String key = part.substring(0, colonIndex);
        String value = part.substring(colonIndex + 1, part.length());

        if (!PARTS_PARSER.containsKey(key)) {
            SpecsLogs.msgWarn("Could not find parser for key ''" + key + "'");
            return Optional.empty();
        }

        return Optional.of(PARTS_PARSER.get(key).apply(value, currentInclude));

    }

}
