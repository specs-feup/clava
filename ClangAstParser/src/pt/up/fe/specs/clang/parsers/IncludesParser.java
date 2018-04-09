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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clang.linestreamparser.GenericLineStreamParser;
import pt.up.fe.specs.clang.linestreamparser.SimpleSnippetParser;
import pt.up.fe.specs.clang.linestreamparser.SnippetParser;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.util.utilities.LineStream;

public class IncludesParser extends GenericLineStreamParser {

    private static final String PARSER_ID = "<Includes>";
    private static final DataKey<List<Include>> TOP_LEVEL_NODES = KeyFactory
            // .generic("stream_includes", new ArrayList<>());
            .generic("stream_includes", (List<Include>) new ArrayList<Include>())
            .setDefault(() -> new ArrayList<>());

    public static IncludesParser newInstance() {
        return new IncludesParser(getDataKey(), newSnippetParser());
    }

    public static DataKey<List<Include>> getDataKey() {
        return TOP_LEVEL_NODES;
    }

    /**
     * Helper method for building SnipperParser instances.
     * 
     * @param id
     * @param resultInit
     * @param dataParser
     * @return
     */
    private static SimpleSnippetParser<List<Include>> newSnippetParser() {

        String id = PARSER_ID;
        List<Include> resultInit = new ArrayList<>();

        return SimpleSnippetParser.newInstance(id, resultInit, IncludesParser::parseInclude);
    }

    private IncludesParser(DataKey<?> key, SnippetParser<?, ?> parser) {
        super(key, parser);
    }

    private static void parseInclude(LineStream lineStream, List<Include> includes) {

        File sourceFile = new File(lineStream.nextLine());
        String include = lineStream.nextLine();
        int line = GeneralParsers.parseInt(lineStream);
        boolean isAngled = GeneralParsers.parseOneOrZero(lineStream);

        includes.add(new Include(sourceFile, include, line, isAngled));
    }
}
