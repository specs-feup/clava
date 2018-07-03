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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParsers;
import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import pt.up.fe.specs.util.utilities.LineStream;

public class VisitedChildrenParser implements LineStreamWorker {

    private static final String PARSER_ID = "<Visited Children>";

    @Override
    public String getId() {
        return PARSER_ID;
    }

    @Override
    public void init(DataStore data) {
        data.add(ClangParserKeys.VISITED_CHILDREN, new HashMap<>());
    }

    @Override
    public void apply(LineStream lineStream, DataStore data) {
        Map<String, List<String>> children = data.get(ClangParserKeys.VISITED_CHILDREN);

        String key = lineStream.nextLine();

        int numChildren = LineStreamParsers.integer(lineStream);
        List<String> childrenIds = parseChildren(lineStream, numChildren);

        // Check after consuming all elements from the stream
        LineStreamParsers.checkDuplicate(PARSER_ID, key, childrenIds, children);
        children.put(key, childrenIds);
    }

    private List<String> parseChildren(LineStream linestream, int numChildren) {
        if (numChildren == 0) {
            return Collections.emptyList();
        }

        List<String> childrenIds = new ArrayList<>(numChildren);
        for (int i = 0; i < numChildren; i++) {
            childrenIds.add(linestream.nextLine());
        }

        return childrenIds;
    }

}
