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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clang.linestreamparser.GenericLineStreamParser;
import pt.up.fe.specs.clang.linestreamparser.SimpleSnippetParser;
import pt.up.fe.specs.clang.linestreamparser.SnippetParser;
import pt.up.fe.specs.util.utilities.LineStream;

public class TopLevelAttributesParser extends GenericLineStreamParser {

    private static final String PARSER_ID = "<Top Level Attributes>";
    private static final DataKey<Set<String>> TOP_LEVEL_ATTRIBUTES = KeyFactory
            .generic("stream_top_level_attributes", new HashSet<>());

    public static TopLevelAttributesParser newInstance() {
        return new TopLevelAttributesParser(getDataKey(), newSnippetParser());
    }

    public static DataKey<Set<String>> getDataKey() {
        return TOP_LEVEL_ATTRIBUTES;
    }

    /**
     * Helper method for building SnipperParser instances.
     * 
     * @param id
     * @param resultInit
     * @param dataParser
     * @return
     */
    private static SimpleSnippetParser<Set<String>> newSnippetParser() {

        String id = PARSER_ID;
        Set<String> resultInit = new LinkedHashSet<>();

        // There can be duplicated ids for type nodes
        BiConsumer<LineStream, Set<String>> parser = (linestream, set) -> GeneralParsers
                .parseStringSet(PARSER_ID, linestream, set, false);

        return SimpleSnippetParser.newInstance(id, resultInit, parser);
    }

    private TopLevelAttributesParser(DataKey<?> key, SnippetParser<?, ?> parser) {
        super(key, parser);
    }

}
