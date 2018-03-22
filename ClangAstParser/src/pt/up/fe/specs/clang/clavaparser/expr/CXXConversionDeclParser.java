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

package pt.up.fe.specs.clang.clavaparser.expr;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clang.clavaparser.utils.FunctionDeclParserResult;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.CXXConversionDecl;
import pt.up.fe.specs.clava.ast.decl.data.CXXMethodDeclData;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data2.CXXMethodDeclDataV2;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.NullType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.parsing.ListParser;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXConversionDeclParser extends AClangNodeParser<CXXConversionDecl> {

    public CXXConversionDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public CXXConversionDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // col:12 implicit operator int (*)() 'int (*(void) const)(void)' inline

        CXXMethodDeclDataV2 data = getData(CXXConversionDecl.class, CXXMethodDeclDataV2.class, node);

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        // boolean emptyName = getStdErr().get(StreamKeys.NAMED_DECL_WITHOUT_NAME).contains(node.getExtendedId());
        // String name = emptyName ? null : parser.apply(ClangGenericParsers::parseClassName);
        String name = parseNamedDeclName(node, parser);

        Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());

        // Check only if method is not implicit
        boolean validType = type instanceof FunctionProtoType || type instanceof NullType;
        // if (!declData.isImplicit() && !(type instanceof FunctionProtoType)) {
        if (!declData.isImplicit() && !validType) {
            SpecsLogs.msgInfo(node.getLocation() +
                    " -> CXXMethodDeclParser with a type that is not a FunctionProtoType:\n" + type);
        }

        ListParser<ClavaNode> children = new ListParser<>(parseChildren(node));
        FunctionDeclParserResult functionDeclParserdata = parser.apply(ClangDataParsers::parseFunctionDecl, children,
                node, getStdErr(),
                CXXConversionDecl.class);

        // Check namespace and store next word
        String namespace = parseKeyValue(parser, "namespace");

        // Check record and store next word
        String record = parseKeyValue(parser, "record");

        // Get corresponding record id
        String recordId = data.getRecordId();
        // String recordId = getStdErr().get(StreamKeys.CXX_METHOD_DECL_PARENT).get(node.getExtendedId());

        CXXMethodDeclData methodData = new CXXMethodDeclData(namespace, record, recordId);

        checkNumChildren(children.getList(), 0);

        return ClavaNodeFactory.cxxConversionDecl(methodData, name, type, functionDeclParserdata.getFunctionDeclData(),
                declData,
                node.getInfo(), functionDeclParserdata.getParameters(), functionDeclParserdata.getDefinition());

    }

}
