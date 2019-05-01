/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang.parsers;

import java.util.HashMap;
import java.util.Map;

import org.suikasoft.jOptions.streamparser.LineStreamParsers;
import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.util.utilities.LineStream;

public class PragmasLocationsParser implements LineStreamWorker<ClangParserData> {

    private static final String PARSER_ID = "<Pragma>";

    private static void parsePragmasLocations(LineStream lineStream, Map<Integer, Integer> locations) {
        int line = LineStreamParsers.integer(lineStream);
        int column = LineStreamParsers.integer(lineStream);

        Integer previousColumn = locations.put(line, column);
        if (previousColumn != null) {
            ClavaLog.warning("Found multiple pragmas in the same line " + line);
        }
    }

    @Override
    public String getId() {
        return PARSER_ID;
    }

    @Override
    public void init(ClangParserData data) {
        data.set(ClangParserData.PRAGMAS_LOCATIONS, new HashMap<>());
    }

    @Override
    public void apply(LineStream lineStream, ClangParserData data) {
        Map<Integer, Integer> locations = data.get(ClangParserData.PRAGMAS_LOCATIONS);
        parsePragmasLocations(lineStream, locations);
    }
}
