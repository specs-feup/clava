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

import java.util.Optional;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.ast.genericnode.GenericClangNode;
import pt.up.fe.specs.clang.ast.genericnode.LiteralNode;
import pt.up.fe.specs.clang.ast.genericnode.NullNode;
import pt.up.fe.specs.clang.ast.genericnode.VariadicType;
import pt.up.fe.specs.clang.exceptions.ClangAstParseException;
import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.util.SpecsLogs;

/**
 * Converts Clang dump information to a tree.
 *
 * 
 * @author Joao Bispo
 * 
 */
public class AstLineParser {

    private SourceLocation lastLocation = null;;

    // private final DataStore stderr;

    /**
     * 
     * 
     * @param filename
     */
    public AstLineParser() {
        // this(DataStore.newInstance("Empty StdErr"));
        // LoggingUtils.msgInfo(
        // "AstLineParser: No DataStore provider, capabilities such as node locations will not be available");
    }

    // public AstLineParser(DataStore stderr) {
    // this.stderr = stderr;
    // }

    // public Optional<GenericClangNode> parse(String line) {
    public ClangNode parse(String line, String set) {
        // Check if null node
        if (line.equals("<<<NULL>>>")) {
            return new NullNode();
        }

        // Check if variadic input
        if (line.equals("...")) {
            return new VariadicType();
        }

        // Name is always after a space
        int nameEnd = line.indexOf(" ");

        // If no space, interpret as a literal node
        if (nameEnd == -1) {
            // if (spaceIndex == -1) {
            SpecsLogs.msgWarn("Could not decode:" + line);
            return new LiteralNode(line);
        }

        /*
        // Extract name for name
         int nameEnd = line.indexOf(" 0x");
        if (nameEnd == -1) {
            // // Could not find address, look for a space
            // int spaceIndex = line.indexOf(' ');
        
            // If no space, interpret as a literal node
            if (nameEnd != -1) {
                // if (spaceIndex == -1) {
                LoggingUtils.msgWarn("Could not decode:" + line);
                return new LiteralNode(line);
            }
        
            String name = line.substring(0, spaceIndex);
            GenericClangNode node = new GenericClangNode(name);
            node.setContent(line.substring(spaceIndex));
            return node;
        
            // throw new RuntimeException("Could not find node name: '" + line + "'");
        }
        */
        String name = line.substring(0, nameEnd);

        GenericClangNode node = new GenericClangNode(name);

        String workString = line.substring(nameEnd).trim();

        // If no address, return the node as is
        // Probably it is an "extra" node, not part of the proper Clang AST
        if (!workString.startsWith("0x")) {
            // System.out.println("NO ADDRESS:" + line);
            node.setContent(workString);
            return node;
        }

        /*
        // Now should be the address
        if (!workString.startsWith("0x")) {
            throw new ClangAstParseException("Could not find the address", line);
        }
        */
        int endIndex = getIndex(workString, " ");
        String address = workString.substring(0, endIndex) + "_" + set;
        // node = node.setAddress(workString.substring(0, endIndex));
        node = node.setAddress(address);

        workString = workString.substring(endIndex).trim();

        // There can be another address further, preceeded by 'prev' or 'parent'
        while (workString.startsWith("prev ") || workString.startsWith("parent ")) {
            // Get index of next address
            int hexIndex = workString.indexOf("0x");
            if (hexIndex == -1) {
                throw new ClangAstParseException("Expected another hex number", line);
            }

            StringBuilder newAddress = new StringBuilder();
            newAddress.append(node.getIdRaw()).append(" ");
            newAddress.append(workString.substring(0, hexIndex));

            // Cut until second hex address
            workString = workString.substring(hexIndex);

            // Extract second hex address
            int hexEndIndex = getIndex(workString, " ");
            String nextAddress = workString.substring(0, hexEndIndex) + "_" + set;
            newAddress.append(nextAddress);
            node = node.setAddress(newAddress.toString());

            workString = workString.substring(hexEndIndex).trim();
        }

        // Location starts with <>
        String location = "";

        if (workString.startsWith("<")) {
            // There can be several '<', go char by char until finding final '>'
            int counter = 1;
            int currentIndex = 0;
            while (counter > 0) {
                currentIndex++;

                if (workString.charAt(currentIndex) == '<') {
                    counter++;
                    continue;
                }

                if (workString.charAt(currentIndex) == '>') {
                    counter--;
                    continue;
                }

            }

            endIndex = currentIndex;
            location = workString.substring(1, endIndex);
            workString = workString.substring(endIndex + 1).trim();
        }
        /*
        SourceRange parsedLocation = stderr.get(StdErrKeys.SOURCE_RANGES).get(address);
        if (parsedLocation == null) {
            parsedLocation = SourceRange.invalidRange();
        }
        
        node = node.setLocation(parsedLocation);
        */

        /*
        // Check if defines a path, and store path if that is the case
        if (!location.isEmpty() && (location.charAt(1) == ':' || location.charAt(0) == '/')) {
            int offset = 0;
            if (location.charAt(1) == ':') {
                offset = 2;
            }
        
            int colonIndex = location.substring(offset).indexOf(':');
            lastFile = location.substring(0, offset + colonIndex);
        }
        
        node.setFilepath(lastFile);
        */
        /*
        if (name.equals("FloatingLiteral")) {
            System.out.println("NODE ID:" + node.getExtendedId());
            System.out.println("LOCATION:" + location);
            System.out.println("LAST FILE:" + lastFile);
        }
        */
        // Ignore line
        // if (workString.startsWith("line")) {
        // endIndex = getIndex(workString, " ");
        // workString = workString.substring(endIndex + 1).trim();
        // }

        // Set remaining string as content
        if (!workString.isEmpty()) {
            // Transform back-slash into slash, to normalize possible paths, but only if it is not a StringLiteral
            if (!name.equals("StringLiteral")) {
                workString = workString.replace('\\', '/');
            }

            // if (workString.indexOf('/') != -1) {
            // System.out.println("!!! LOC:" + workString);
            // }

            // node = node.setContent(workString);
            node.setContent(workString);
        }

        /// Special code cases
        Optional<String> code = getSpecialCode(node);
        if (code.isPresent()) {
            node = node.setLocationCode(code.get());
        }

        return node;
    }

    private static Optional<String> getSpecialCode(ClangNode node) {
        if (node.getName().equals("VarDecl")) {
            return decodeVarDeclCode(node);
        }

        return Optional.empty();
    }

    private static Optional<String> decodeVarDeclCode(ClangNode node) {
        if (!node.getContentTry().isPresent()) {
            return Optional.empty();
        }

        String[] parts = node.getContentTry().get().split(" ");

        if (parts.length < 2) {
            return Optional.empty();
        }

        // Ignore first, check if second part is 'used'
        String potentialCode = parts[1];

        if (!potentialCode.equals("used")) {
            return Optional.of(potentialCode);
        }

        // Try to get the third part
        if (parts.length < 3) {
            return Optional.empty();
        }

        return Optional.of(parts[2]);
    }

    private static int getIndex(String line, String string) {
        int index = line.indexOf(string);
        if (index == -1) {
            throw new ClangAstParseException("Could not find '" + string + "' in:", line);

        }

        return index;
    }

}
