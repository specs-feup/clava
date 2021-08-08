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

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.util.utilities.LineStream;

public class SkippedNodesParser implements LineStreamWorker<ClangAstData> {

    private static final String PARSER_ID = "<Skipped Nodes Map>";

    @Override
    public String getId() {
        return PARSER_ID;
    }

    @Override
    public void init(ClangAstData data) {
        data.set(ClangAstData.SKIPPED_NODES_MAP, new HashMap<>());
    }

    @Override
    public void apply(LineStream lineStream, ClangAstData data) {
        Map<String, String> map = data.get(ClangAstData.SKIPPED_NODES_MAP);
        LineStreamParsers.stringMap(PARSER_ID, lineStream, map);
    }

}
