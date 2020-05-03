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

package pt.up.fe.specs.clava.parsing.snippet.rules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.parsing.pragma.PragmaParsers;
import pt.up.fe.specs.clava.parsing.snippet.TextParserRule;
import pt.up.fe.specs.util.SpecsLogs;

public class PragmaRule implements TextParserRule {

    private static final String PRAGMA = "#pragma";

    @Override
    public Optional<ClavaNode> apply(String filepath, String line, int lineNumber, Iterator<String> iterator,
            ClavaContext context) {

        // To calculate position of pragma
        String lastLine = line;

        // Check if line starts with '#pragma'
        String trimmedLine = line.trim();

        // First characters that can contain #pragma
        if (trimmedLine.length() < PRAGMA.length()) {
            return Optional.empty();
        }

        String probe = trimmedLine.substring(0, PRAGMA.length());
        if (!probe.toLowerCase().equals("#pragma")) {
            return Optional.empty();
        }

        // Found start of pragma. Try to find the end
        trimmedLine = trimmedLine.substring(PRAGMA.length()).trim();

        List<String> pragmaContents = new ArrayList<String>();

        while (trimmedLine.endsWith("\\")) {
            // Add line, without the ending '\'
            pragmaContents.add(trimmedLine.substring(0, trimmedLine.length() - 1));

            if (!iterator.hasNext()) {
                SpecsLogs.msgInfo("Could not parse #pragma, there is no more lines after '" + trimmedLine + "'");
                return Optional.empty();
            }

            // Get next line
            lastLine = iterator.next();
            trimmedLine = lastLine.trim();
        }

        // Add last non-broken line
        pragmaContents.add(trimmedLine);

        // If no endIndex found, comment is malformed
        // Preconditions.checkArgument(endIndex != -1,
        // "Could not find end of multi-line comment start at '" + filepath + "':" + lineNumber);

        int startCol = line.indexOf('#') + 1;
        int endCol = lastLine.length();
        int endLine = lineNumber + pragmaContents.size() - 1;

        SourceRange loc = new SourceRange(filepath, lineNumber, startCol, endLine, endCol);
        // ClavaNodeInfo info = new ClavaNodeInfo(null, loc);

        // Try to parse pragma. If pragma not parsable, create generic pragma
        ClavaNode pragmaNode = PragmaParsers.parse(pragmaContents, context)
                .orElse(context.getFactory().genericPragma(pragmaContents));
        pragmaNode.set(ClavaNode.LOCATION, loc);

        return Optional.of(pragmaNode);
    }

}
