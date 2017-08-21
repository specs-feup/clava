/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clang.pragma;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.omp.OmpParser;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.util.stringparser.StringParser;

public class PragmaParsers {

    private static final Map<String, PragmaParser> PRAGMA_PARSERS;
    static {
        PRAGMA_PARSERS = new HashMap<>();
        PRAGMA_PARSERS.put("lara", new LaraPragmaParser());
        PRAGMA_PARSERS.put("omp", new OmpParser());
    }

    /**
     * Helper method which concatenates a list of Strings into a single String separated by whitespaces
     * 
     * @param contents
     * @param info
     * @return
     */
    public static Optional<Pragma> parse(List<String> contents, ClavaNodeInfo info) {
        return parse(contents.stream().collect(Collectors.joining(" ")), info);
    }

    public static Optional<Pragma> parse(String contents, ClavaNodeInfo info) {

        String trimmedContents = contents.trim();

        // Check first word of the contents
        int spaceIndex = trimmedContents.indexOf(" ");

        if (spaceIndex == -1) {
            return Optional.empty();
        }

        String pragmaType = trimmedContents.substring(0, spaceIndex);
        PragmaParser parser = PRAGMA_PARSERS.get(pragmaType);

        if (parser == null) {
            return Optional.empty();
        }

        StringParser parserContents = new StringParser(trimmedContents.substring(spaceIndex).trim());
        Optional<Pragma> pragma = Optional.of(parser.parse(parserContents, info));

        // Check that parser contents have been consumed
        if (pragma.isPresent() && !parserContents.isEmpty()) {
            throw new RuntimeException("Parser '" + parser.getClass().getSimpleName()
                    + "' did not consume completly the given string:" + parserContents);
        }

        return pragma;
    }
}
