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
import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.extra.CXXCtorInitializerParser;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clang.clavaparser.utils.FunctionDeclParserResult;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.decl.data.CXXMethodDeclData;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.extra.CXXCtorInitializer;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.CXXCtorInitializerKind;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.parsing.ListParser;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXConstructorDeclParser extends AClangNodeParser<CXXConstructorDecl> {

    public CXXConstructorDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXConstructorDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // col:13 CSVReader 'void (char)'
        // col:11 implicit CSVReader 'void (class Routing::CSVReader &&)' inline noexcept-unevaluated 0x4ff6948
        // col:4 MCSimulation 'void (const std::string, const std::string)'
        // line:304:24 MCSimulation 'void (const std::string, const std::string)' namespace Routing record CSVReader

        System.out.println("CXX CONSTRUCTOR DECL:" + parser);

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        boolean emptyName = getStdErr().get(StreamKeys.NAMED_DECL_WITHOUT_NAME).contains(node.getExtendedId());
        // String className = emptyName ? null : parser.apply(StringParsers::parseWord);

        String className = emptyName ? null : parser.apply(ClangGenericParsers::parseClassName);

        Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());

        // Work directly with children of node. Make a copy to avoid modifications to the tree
        List<ClangNode> clangChildren = new ArrayList<>(node.getChildren());

        // Extract CXXCtorInitializer nodes
        List<ClangNode> ctorInit = SpecsCollections.remove(clangChildren,
                clangNode -> clangNode.getName().equals("CXXCtorInitializer"));

        // Parse CXXCtorInitializer nodes
        List<CXXCtorInitializer> defaultInits = parseInitializers(node.getExtendedId(), ctorInit);

        ListParser<ClavaNode> children = new ListParser<>(parseChildren(clangChildren.stream()));
        FunctionDeclParserResult functionDeclParsed = parser.apply(ClangDataParsers::parseFunctionDecl, children);

        // Check namespace and store next word
        String namespace = parseKeyValue(parser, "namespace");

        // Check record and store next word
        String record = parseKeyValue(parser, "record");

        // Get corresponding record id
        String recordId = getStdErr().get(StreamKeys.CXX_METHOD_DECL_PARENT).get(node.getExtendedId());

        CXXMethodDeclData methodData = new CXXMethodDeclData(namespace, record, recordId);

        // List<CXXCtorInitializer> defaultInits = parseChildren(ctorInit, new
        // CXXCtorInitializerParser(getConverter()));

        // List<CXXCtorInitializer> defaultInits = new ArrayList<>();

        // List<ClavaNode> children = parseChildren(clangChildren.stream());

        // First children are the parameters of the constructor (ParmVarDecl)
        // List<ParmVarDecl> params = children.pop(ParmVarDecl.class);
        // List<ParmVarDecl> params = new ArrayList<>();
        // children = head(children, ParmVarDecl.class, params);

        // Optionally, there can be CXXCtorInitializerField
        // List<CXXCtorInitializer> defaultInits = new ArrayList<>();
        // children = head(children, CXXCtorInitializer.class, defaultInits);

        // Optionally, there can be a Stmt for the body
        // List<Stmt> definition = children.isEmpty() ? Collections.emptyList() : children.pop(1, stmt -> toStmt(stmt));
        // Stmt definition = null;
        // if (!children.isEmpty()) {
        // definition = toStmt(children.get(0));
        // children = children.subList(1, children.size());
        // }

        checkNumChildren(children.getList(), 0);

        // if (definition == null) {
        // return ClavaNodeFactory.cxxConstructorDecl(methodData, defaultInits, className, params, type,
        // functionDeclData, declData, node.getInfo());
        // }

        return ClavaNodeFactory.cxxConstructorDecl(methodData, defaultInits, className,
                functionDeclParsed.getParameters(), type, functionDeclParsed.getFunctionDeclData(),
                declData, node.getInfo(), functionDeclParsed.getDefinition());

    }

    private List<CXXCtorInitializer> parseInitializers(String id, List<ClangNode> ctorInit) {
        // If there are not initializers, return empty string
        // if (ctorInit.isEmpty()) {
        // if (!getStdErr().get(StdErrKeys.CXX_CTOR_INITIALIZERS).get(id).isEmpty()) {
        // System.out.println("CTOR NOT EMPTY:" + id);
        // }
        // return Collections.emptyList();
        // }
        List<String> initKinds = getStdErr().get(StreamKeys.CXX_CTOR_INITIALIZERS).get(id);
        Preconditions.checkArgument(initKinds.size() == ctorInit.size(),
                "For id '" + id + "', expected size of initializers ("
                        + ctorInit.size() + ") to be the same as initializers kinds (" + initKinds.size() + ")");

        List<CXXCtorInitializer> initializers = new ArrayList<>(ctorInit.size());

        // Parse each initializer
        for (int i = 0; i < ctorInit.size(); i++) {

            // Check if initializer has a type id
            String[] initKind = initKinds.get(i).split(":");
            Type initType = initKind.length == 2 ? getOriginalTypes().get(initKind[1]) : null;
            if (initKind.length == 2) {
                Preconditions.checkNotNull(initType, "No type found: " + initKind[1]);
            }

            CXXCtorInitializerKind kind = CXXCtorInitializerKind.getHelper().valueOf(initKind[0]);
            CXXCtorInitializer init = new CXXCtorInitializerParser(getConverter(), kind, initType)
                    .parse(ctorInit.get(i));
            initializers.add(init);
        }

        return initializers;
    }

}
