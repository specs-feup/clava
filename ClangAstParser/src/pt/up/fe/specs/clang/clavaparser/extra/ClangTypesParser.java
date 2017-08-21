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

package pt.up.fe.specs.clang.clavaparser.extra;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.ClavaParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.extra.Undefined;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsLogs;

/**
 * Parses ClangNodes that represent types to ClavaNodes, and maps ClangNodes addresses to the equivalent ClavaNodes.
 * 
 * @author JoaoBispo
 *
 */
public class ClangTypesParser {

    private final ClangConverterTable converter;

    public ClangTypesParser(ClangConverterTable converter) {
        this.converter = converter;
    }

    public Map<String, Type> parse(List<ClangNode> clangTypes) {

        Map<String, Type> types = new HashMap<>();

        for (ClangNode clangType : clangTypes) {

            // Parse node
            ClavaNode clavaNode = converter.parse(clangType);

            if (clavaNode instanceof Undefined) {
                clavaNode = ClavaNodeFactory.dummyType(clavaNode);
            }

            if (!(clavaNode instanceof Type)) {
                throw new RuntimeException("Expected a Type node, got: " + clavaNode.getNodeName());
            }

            // Add type and all children that are types to the map
            // clavaNode.getDescendantsAndSelf(Type.class)
            // .forEach(type -> types.put(type.getInfo().getExtendedId(), type));
            // if (clavaNode instanceof RecordType) {
            // System.out.println("ADDED RECORD TYPE TO MAP:" + clangType.getInfo().getExtendedId());
            // }
            types.put(clangType.getInfo().getExtendedId(), (Type) clavaNode);
        }

        SpecsLogs.msgInfo("--- Types parsing report ---");
        ClavaParser.checkUndefinedNodes(() -> types.values().stream()
                .flatMap(node -> node.getDescendantsAndSelfStream()));

        return types;
    }

}
