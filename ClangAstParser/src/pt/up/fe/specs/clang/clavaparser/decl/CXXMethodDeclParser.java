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

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clang.clavaparser.utils.FunctionDeclParserResult;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.data.CXXMethodDeclData;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.NullType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.parsing.ListParser;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXMethodDeclParser extends AClangNodeParser<CXXMethodDecl> {

    public CXXMethodDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXMethodDecl parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // col:32 operator[] 'const std::string &(std::size_t) const'
        // col:18 readNextRow 'void (std::istream &)'
        // col:11 implicit operator= 'class Routing::CSVReader &(class Routing::CSVReader &&)' inline
        // noexcept-unevaluated 0x4f96ad0
        // line:5:21 used readNextRow 'void (std::istream &)'
        // WriteResultSingle 'void (std::vector<float> &, std::string &&, float)' static namespace Routing record Data
        // PARSER:col:14 AreEqual '_Bool (double, double)' static
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
        FunctionDeclParserResult data = parser.apply(ClangDataParsers::parseFunctionDecl, children, node, getStdErr());
        // boolean isStatic = parser.apply(string -> ClangParseWorkers.checkWord(string, "static"));

        // boolean isInline = parser.apply(string -> ClangParseWorkers.checkWord(string, "inline"));
        /*
        	ExceptionType exceptionType = parser.apply(string -> ClangParseWorkers.parseEnum(string, ExceptionType.class,
        		ExceptionType.NONE, getExceptionTypeMappings()));
        
        
        	long exceptionAddress = CXXMethodDecl.getNullExceptAddress();
        	if (exceptionType != ExceptionType.NONE) {
        	    exceptionAddress = parser.apply(ClangParseWorkers::parseHex);
        	}
        */

        // Check namespace and store next word
        String namespace = parseKeyValue(parser, "namespace");

        // Check record and store next word
        String record = parseKeyValue(parser, "record");

        // Get corresponding record id
        String recordId = getStdErr().get(StreamKeys.CXX_METHOD_DECL_PARENT).get(node.getExtendedId());

        CXXMethodDeclData methodData = new CXXMethodDeclData(namespace, record, recordId);

        // int numberTemplateArguments = getStdErr().get(StdErrKeys.NUMBER_TEMPLATE_ARGUMENTS)
        // .getOrDefault(node.getExtendedId(), 0);

        // First, template arguments, if any
        // List<TemplateArgument> templateArguments = new ArrayList<>();
        // children = head(children, TemplateArgument.class, templateArguments);

        // Preconditions.checkArgument(templateArguments.size() == numberTemplateArguments,
        // "Expected " + numberTemplateArguments + ", there are " + templateArguments.size() + " instead");

        // Get the parameters of the constructor (ParmVarDecl)
        // List<ParmVarDecl> params = new ArrayList<>();
        // children = head(children, ParmVarDecl.class, params);
        // List<ParmVarDecl> params = children.pop(ParmVarDecl.class);

        // Optionally, there can be a Stmt
        // List<Stmt> definition = children.isEmpty() ? Collections.emptyList() : children.pop(1, stmt -> toStmt(stmt));
        // List<Stmt> definition = children.pop(Stmt.class);
        // List<Stmt> definition = Collections.emptyList();
        // if (!children.isEmpty()) {
        // definition = Arrays.asList(toStmt(children.get(0)));
        // children = children.subList(1, children.size());
        // }

        checkNumChildren(children.getList(), 0);

        return ClavaNodeFactory.cxxMethodDecl(methodData, name, type, data.getFunctionDeclData(), declData,
                node.getInfo(), data.getParameters(), data.getDefinition());

    }

}
