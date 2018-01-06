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

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clang.streamparser.data.FieldDeclInfo;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.FieldDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class FieldDeclParser extends AClangNodeParser<FieldDecl> {

    public FieldDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public FieldDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // optimization 'enum e_optimization_flag'
        // referenced r 'double [3]'
        // col:11 m_secondInterval 'float'
        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        // parser.apply(ClangGenericParsers::parseLocation);
        // boolean isReferecend = parser.apply(string -> ClangGenericParsers.checkWord(string, "referenced"));

        String fieldName = parseNamedDeclName(node, parser);
        // boolean hasName = !getStdErr().get(StreamKeys.NAMED_DECL_WITHOUT_NAME).contains(node.getExtendedId());
        // // System.out.println("NAMED DECL NO NAME:" + getStdErr().get(StreamKeys.NAMED_DECL_WITHOUT_NAME));
        // // System.out.println("CURRENT: " + node.getExtendedId());
        // String fieldName = null;
        // if (hasName) {
        // fieldName = parser.apply(StringParsers::parseWord);
        // }

        Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());
        // List<Type> type = parser.apply(string -> ClangParseWorkers.parseClangType(string, node.getLocation()));
        // List<Type> type = parser.apply(ClangGenericParsers::parseClangTypeList, node, getTypesMap());

        boolean isMutable = parser.apply(StringParsers::hasWord, "mutable");
        boolean isModulePrivate = parser.apply(StringParsers::hasWord, "__module_private__");

        FieldDeclInfo info = getStdErr().get(StreamKeys.FIELD_DECL_INFO).get(node.getExtendedId());

        List<ClavaNode> children = parseChildren(node);

        checkNumChildren(children, info.getNumExpectedChildren());
        // checkChildrenBetween(children, 0, 1);

        Expr bitwidth = info.isBitField() ? SpecsCollections.popSingle(children, Expr.class) : null;
        Expr inClassInitializer = info.hasInClassInitialized() ? SpecsCollections.popSingle(children, Expr.class)
                : null;

        return ClavaNodeFactory.fieldDecl(isMutable, isModulePrivate, fieldName, type, declData, info(node),
                bitwidth, inClassInitializer);

    }

}
