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

package pt.up.fe.specs.clang.parsers.util;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.specs.clava.ClavaLog;

public class PragmasLocations {

    private final Map<File, Map<Integer, Integer>> pragmaLocations;

    public PragmasLocations() {
        pragmaLocations = new HashMap<>();
    }

    public Map<Integer, Integer> getPragmaLocations(File sourceFile) {
        return pragmaLocations.getOrDefault(sourceFile, Collections.emptyMap());
    }

    public Integer addPragmaLocation(File sourceFile, Integer line, Integer column) {
        Map<Integer, Integer> locations = pragmaLocations.get(sourceFile);
        if (locations == null) {
            locations = new HashMap<>();
            pragmaLocations.put(sourceFile, locations);
        }

        Integer previousColumn = locations.put(line, column);
        if (previousColumn != null) {
            ClavaLog.warning("Found multiple pragmas in the same line " + line + " in the file '" + sourceFile + "'");
        }

        return previousColumn;
    }

    @Override
    public String toString() {
        return pragmaLocations.toString();
    }
}
