/**
 * Copyright 2012 SPeCS Research Group.
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

package pt.up.fe.specs.clang.astlineparser;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.streamparser.StreamParser;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.utilities.BufferedStringBuilder;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Helper class which converts MATLAB code into a MATLAB-IR tree, a file at a time.
 * 
 * @author Joao Bispo
 * 
 */
public class AstParser {

    private static final Set<Character> ALLOWED_TREE_CHARS = new HashSet<>(Arrays.asList('|', ' ', '`'));

    private final BufferedStringBuilder dumpFile;

    private final String customSet;

    public AstParser(File dumpFile) {
        this(dumpFile, null);
    }

    /**
     * Creates a Parser that uses System.err as the default message stream.
     */
    public AstParser(File dumpFile, String customSet) {
        this.dumpFile = dumpFile == null ? null : new BufferedStringBuilder(dumpFile);
        this.customSet = customSet;
    }

    public AstParser() {
        this(null);
    }

    /*
    public List<ClangNode> parse(ResourceProvider resource) {
        try (LineStream reader = LineStream.newInstance(resource, false)) {
            // System.out.println("FILENAME:" + reader.getFilename());
            return parse(reader);
        }
    }
    */

    /*
    public Optional<List<ClangNode>> parseTry(String mCode) {
        try {
            return Optional.of(parse(mCode));
        } catch (RuntimeException e) {
            LoggingUtils.msgWarn("Problems during parsing", e);
            return Optional.empty();
        }
    }
    */

    /**
     * Convenience method which accepts a string, and uses FileNode.getNoFilename() as default name.
     * 
     * @param mCode
     * @return
     */
    public List<ClangNode> parse(String mCode) {
        try (LineStream stream = LineStream.newInstance(new StringReader(mCode), Optional.empty())) {
            return parse(stream);
        }
    }

    // private static List<ClangNode> parse(LineStream lineReader) {
    // return parse(lineReader, null);
    // }
    /*
    // public List<ClangNode> parse(String mCode, DataStore stderr) {
    public List<ClangNode> parse(String mCode) {
        try (LineStream stream = LineStream.newInstance(new StringReader(mCode), Optional.empty())) {
            // return parse(stream, stderr);
            return parse(stream);
        }
    }
    */
    public List<ClangNode> parse(InputStream inputStream) {
        return parse(LineStream.newInstance(inputStream, null));
    }

    public List<ClangNode> parse(LineStream lineReader) {
        // public static List<ClangNode> parse(LineStream lineReader, DataStore stderr) {

        // AstLineParser lineParser = stderr == null ? new AstLineParser() : new AstLineParser(stderr);
        AstLineParser lineParser = new AstLineParser();

        // Map tree node level with last created node
        Map<Integer, ClangNode> lastCreatedNodes = new HashMap<>();

        List<ClangNode> nodes = new ArrayList<>();

        String currentSet = customSet;

        // Process Clang dump line-by-line
        while (lineReader.hasNextLine()) {
            // for (String line : lineReader.getIterable()) {
            String line = lineReader.nextLine();

            if (dumpFile != null) {
                dumpFile.append(line).append("\n");
            }

            // Check if counter
            if (line.equals(StreamParser.getTranslationUnitSetPrefix())) {
                currentSet = lineReader.nextLine();
                // currentSet = line.substring(ClangAstParser.getTranslationUnitSetPrefix().length());
                continue;
            }

            // Check if top counter
            // if (line.startsWith("TOP_COUNTER:")) {
            // long counter = Long.parseLong(line.substring("TOP_COUNTER:".length()));
            //
            // // If counter goes back to 1, increment set
            // if (counter == 1) {
            // ++currentSet;
            // }
            //
            // continue;
            // }

            // Ignore error lines
            if (line.startsWith("LLVM ERROR")) {
                SpecsLogs.msgInfo("Warning:" + line);
                continue;
            }

            // Determine the level of the node
            int index = getFirstIndexOfNode(line);
            // if (index == -1) {
            // throw new RuntimeException("Could not find the beginning of an alphabetic charater in the line: '" + line
            // + "'");
            // }
            // System.out.println("INDEX OF LETER:" + index);
            String lineToParse = line.substring(index);

            int level = index / 2;
            // Increase level by 1, so that nodes start at 1
            level++;

            // System.out.println("LEVEL:" + level);
            // Optional<GenericClangNode> nodeTry = lineParser.parse(lineToParse);
            ClangNode node = null;
            try {
                node = lineParser.parse(lineToParse, currentSet);
            } catch (Exception e) {
                throw new RuntimeException("Could not parse line: " + lineToParse, e);
            }

            // Set set
            // node.getInfo().setSet(currentSet);

            // Null node, continue
            // if (!nodeTry.isPresent()) {
            // continue;
            // }
            //
            // GenericClangNode node = nodeTry.get();

            // Get node from previous level
            ClangNode parentNode = lastCreatedNodes.get(level - 1);
            // if (parentNode == null) {
            // throw new RuntimeException("No parent for level " + level);
            // }
            if (parentNode == null) {
                // Found top-level node
                nodes.add(node);
            } else {
                parentNode.addChild(node);
                /*
                // call getLocation, to correctly compute the path of the node
                Location location = node.getLocation();
                if (node.getName().equals("FloatingLiteral")) {
                    System.out.println("ID:" + node.getExtendedId());
                    System.out.println("LOCATION:" + location);
                }
                */
            }

            lastCreatedNodes.put(level, node);
        }

        if (dumpFile != null) {
            dumpFile.close();
        }

        // ClangNode fileNode = lineParser.getRoot();

        // return rootNode;
        return nodes;
    }

    /**
     * To find the index of where the node declaration starts, find the first dash before any character that is not in
     * the list of permitted characters.
     * 
     * @param line
     * @return
     */
    private static int getFirstIndexOfNode(String line) {
        // If first character is not part of the allowed characters, return immediately
        if (!ALLOWED_TREE_CHARS.contains(line.charAt(0))) {
            return 0;
        }

        int counter = 0;
        while (counter < line.length()) {

            // if (Character.isAlphabetic(line.charAt(counter))) {
            if (line.charAt(counter) == '-') {
                return counter + 1;
            }
            counter++;
        }

        // No dash found, throw exception
        throw new RuntimeException("Could not find beginning of node description: '" + line + "'");
    }
}
