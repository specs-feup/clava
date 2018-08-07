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

import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.RecordDeclData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.stringparser.StringParser;

public class RecordDeclParser extends AClangNodeParser<RecordDecl> {

    public RecordDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public RecordDecl parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // line:1:8 struct struct_test definition
        // line:6:7 union union_test definition

        // Drop location
        // parser.apply(ClangGenericParsers::parseLocation);
        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        // Parse children, to extract attributes to RecordDeclData
        List<ClavaNode> children = parseChildren(node);

        // RecordDeclData recordDeclData = parser.apply(ClangDataParsers::parseRecordDecl, node, getTypesMap(),
        // getStdErr(), children);
        RecordDeclData recordDeclData = parseRecordDecl(node, children, parser);

        List<Decl> decls = children.stream().map(child -> toDecl(child)).collect(Collectors.toList());

        ClavaNodeInfo info = info(node);

        Type type = getTypesMap().get(node.getExtendedId());

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.recordDecl(recordDeclData, type, declData, info, decls);
    }

}
