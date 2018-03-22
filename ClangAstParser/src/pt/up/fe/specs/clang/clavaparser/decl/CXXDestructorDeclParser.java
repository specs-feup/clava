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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clang.clavaparser.utils.FunctionDeclParserResult;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.CXXDestructorDecl;
import pt.up.fe.specs.clava.ast.decl.data.CXXMethodDeclData;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data2.CXXMethodDeclDataV2;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.parsing.ListParser;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXDestructorDeclParser extends AClangNodeParser<CXXDestructorDecl> {

    public CXXDestructorDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXDestructorDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // col:11 implicit ~CSVReader 'void (void)' inline noexcept-unevaluated 0x4f96c90
        // col:4 ~MCSimulation 'void (void)' noexcept-unevaluated 0x47ef7c8
        // line:285:24 ~MCSimulation 'void (void) noexcept' namespace Routing record CSVReader
        // col:13 ~Exception 'void (void) noexcept' virtual

        CXXMethodDeclDataV2 data = getData(CXXDestructorDecl.class, CXXMethodDeclDataV2.class, node);

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        // boolean emptyName = getStdErr().get(StreamKeys.NAMED_DECL_WITHOUT_NAME).contains(node.getExtendedId());
        // String className = emptyName ? null : parser.apply(ClangGenericParsers::parseClassName);
        String className = parseNamedDeclName(node, parser);

        Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());

        Type functionType = getTypesMap().get(node.getExtendedId());
        if (functionType == null) {
            Preconditions.checkArgument(declData.isImplicit());
        }

        ListParser<ClavaNode> children = new ListParser<>(parseChildren(node));
        FunctionDeclParserResult functionDeclParserdata = parser.apply(ClangDataParsers::parseFunctionDecl, children,
                node, getStdErr(),
                CXXDestructorDecl.class);

        // Check namespace and store next word
        String namespace = parseKeyValue(parser, "namespace");

        // Check record and store next word
        String record = parseKeyValue(parser, "record");

        // Get corresponding record id
        String recordId = data.getRecordId();
        // String recordId = getStdErr().get(StreamKeys.CXX_METHOD_DECL_PARENT).get(node.getExtendedId());

        // TODO: Do not know yet position of virtual, check Clang dumper
        // boolean isVirtual = parser.apply(string -> ClangGenericParsers.checkWord(string, "virtual"));

        CXXMethodDeclData methodData = new CXXMethodDeclData(namespace, record, recordId);

        // List<ClavaNode> children = parseChildren(node);
        //
        // // First children are the parameters of the destructor (ParmVarDecl)
        // List<ParmVarDecl> params = new ArrayList<>();
        // children = head(children, ParmVarDecl.class, params);
        //
        // // Optionally, there can be a Stmt
        // List<Stmt> definition = Collections.emptyList();
        // if (!children.isEmpty()) {
        // definition = Arrays.asList(toStmt(children.get(0)));
        // children = children.subList(1, children.size());
        // }

        checkNumChildren(children.getList(), 0);

        return ClavaNodeFactory.cxxDestructorDecl(methodData, className, type,
                functionDeclParserdata.getFunctionDeclData(), declData,
                node.getInfo(), functionDeclParserdata.getParameters(), functionDeclParserdata.getDefinition());
    }

}
