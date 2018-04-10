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
import java.util.function.BiConsumer;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clang.linestreamparser.GenericLineStreamParser;
import pt.up.fe.specs.clang.linestreamparser.SimpleSnippetParser;
import pt.up.fe.specs.clang.linestreamparser.SnippetParser;
import pt.up.fe.specs.util.utilities.LineStream;

public class IdToFilenameParser extends GenericLineStreamParser {

    private static final String PARSER_ID = "<Id-File Map>";
    private static final DataKey<Map<String, String>> ID_TO_CLASSNAME_MAP = KeyFactory
            .generic("stream_id_to_filename_map", new HashMap<>());

    public static IdToFilenameParser newInstance() {
        return new IdToFilenameParser(getDataKey(), newSnippetParser());
    }

    public static DataKey<Map<String, String>> getDataKey() {
        return ID_TO_CLASSNAME_MAP;
    }

    /**
     * Helper method for building SnipperParser instances.
     * 
     * @param id
     * @param resultInit
     * @param dataParser
     * @return
     */
    private static SimpleSnippetParser<Map<String, String>> newSnippetParser() {

        String id = PARSER_ID;
        Map<String, String> resultInit = new HashMap<>();

        BiConsumer<LineStream, Map<String, String>> parser = (linestream, map) -> GeneralParsers
                .parseStringMap(PARSER_ID, linestream, map);

        return SimpleSnippetParser.newInstance(id, resultInit, parser);
    }

    private IdToFilenameParser(DataKey<?> key, SnippetParser<?, ?> parser) {
        super(key, parser);
    }

}
