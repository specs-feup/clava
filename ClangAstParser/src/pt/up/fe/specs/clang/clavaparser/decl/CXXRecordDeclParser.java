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

package pt.up.fe.specs.clang.clavaparser.decl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.RecordBase;
import pt.up.fe.specs.clava.ast.decl.data.RecordDeclData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.stringparser.ParserResult;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.utilities.StringSlice;

public class CXXRecordDeclParser extends AClangNodeParser<CXXRecordDecl> {

    public CXXRecordDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public CXXRecordDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // line:104:8 struct compile_config_s definition
        // col:8 implicit struct compile_config_s
        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        // HACK: Before parsing children, remove first node that will be a CXXRecordDecl.
        // This node is not being used in the Clava AST, and currently we are not able to
        // visit it in the dumper, which makes the parsing of the node fail due to missing
        // information about the name
        List<ClangNode> clangChildren = new ArrayList<>(node.getChildren());
        for (int i = 0; i < clangChildren.size(); i++) {
            if (clangChildren.get(i).getName().equals("CXXRecordDecl")) {
                // Remove first CXXRecordDecl
                clangChildren.remove(i);
                break;
            }
        }

        // Parse children, to extract attributes to RecordDeclData
        // List<ClavaNode> children = parseChildren(node);
        List<ClavaNode> children = parseChildren(clangChildren.stream());

        // RecordDeclData recordDeclData = parser.apply(ClangDataParsers::parseRecordDecl, node, getTypesMap(),
        // getStdErr(), children);
        RecordDeclData recordDeclData = parseRecordDecl(node, children, parser);
        // System.out.println("AFTER RECORD DECL:" + parser);
        // Parse bases
        List<RecordBase> recordBases = Collections.emptyList();

        List<String> baseTypes = getStdErr().get(StreamKeys.BASES_TYPES).get(node.getExtendedId());
        if (!baseTypes.isEmpty()) {
            // Re-initialize list
            recordBases = new ArrayList<>();

            int startIndex = recordDeclData.getAttributes().size();

            // For each base type, build a RecordBase instance
            for (int i = 0; i < baseTypes.size(); i++) {

                // Remove child that represents base
                children.remove(0);

                // Obtain base info
                ClangNode baseNode = node.getChild(startIndex + i);

                // Rebuild content
                String nodeContent = baseNode.getName() + " " + baseNode.getContent();

                // Get type of base
                Type baseType = getOriginalTypes().get(baseTypes.get(i));
                Preconditions.checkNotNull(baseType, "Could not find type '" + baseTypes.get(i) + "'");

                // Create base record
                StringSlice content = new StringSlice(nodeContent);

                ParserResult<RecordBase> result = ClangDataParsers.parseRecordBase(content, baseType);
                Preconditions.checkArgument(result.getModifiedString().isEmpty(),
                        "Expected Base content to be empty, it still has '" + result.getModifiedString() + "'");

                recordBases.add(result.getResult());
            }
        }

        // If first child is a CXXRecordDecl, remove it
        // if (!children.isEmpty() && children.get(0) instanceof CXXRecordDecl) {
        // children.remove(0);
        // }

        List<Decl> decls = children.stream().map(child -> toDecl(child)).collect(Collectors.toList());

        ClavaNodeInfo info = info(node);

        // Obtain type from map. There are some cases where it can be null, as when it was a CXXRecordDecl first child
        // of another CXXRecordDecl.
        Type type = getTypesMap().get(node.getExtendedId());

        return ClavaNodeFactory.cxxRecordDecl(recordBases, recordDeclData, type, declData, info, decls);
    }

}
