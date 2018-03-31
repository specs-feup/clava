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
import java.util.function.BiConsumer;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clang.linestreamparser.GenericLineStreamParser;
import pt.up.fe.specs.clang.linestreamparser.SimpleSnippetParser;
import pt.up.fe.specs.clang.linestreamparser.SnippetParser;
import pt.up.fe.specs.util.utilities.LineStream;

public class VisitedChildrenParser extends GenericLineStreamParser {

    private static final String PARSER_ID = "<Visited Children>";
    private static final DataKey<Map<String, List<String>>> VISITED_CHILDREN = KeyFactory
            .generic("stream_visited_children", new HashMap<>());

    public static VisitedChildrenParser newInstance() {
        return new VisitedChildrenParser(getDataKey(), newSnippetParser());
    }

    public static DataKey<Map<String, List<String>>> getDataKey() {
        return VISITED_CHILDREN;
    }

    /**
     * Helper method for building SnipperParser instances.
     * 
     * @param id
     * @param resultInit
     * @param dataParser
     * @return
     */
    private static SimpleSnippetParser<Map<String, List<String>>> newSnippetParser() {

        String id = PARSER_ID;
        Map<String, List<String>> resultInit = new HashMap<>();

        BiConsumer<LineStream, Map<String, List<String>>> parser = VisitedChildrenParser::parseVisitedChildren;

        return SimpleSnippetParser.newInstance(id, resultInit, parser);
    }

    private VisitedChildrenParser(DataKey<?> key, SnippetParser<?, ?> parser) {
        super(key, parser);
    }

    private static void parseVisitedChildren(LineStream linestream, Map<String, List<String>> children) {
        String key = linestream.nextLine();

        GeneralParsers.checkDuplicate(PARSER_ID, key, children);

        int numChildren = GeneralParsers.parseInt(linestream);
        List<String> childrenIds = parseChildren(linestream, numChildren);
        children.put(key, childrenIds);
    }

    private static List<String> parseChildren(LineStream linestream, int numChildren) {
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
