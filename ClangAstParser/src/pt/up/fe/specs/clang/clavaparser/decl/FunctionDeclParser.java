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
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.parsing.ListParser;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class FunctionDeclParser extends AClangNodeParser<FunctionDecl> {

    public FunctionDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public FunctionDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        // ToString 'const std::string (enum e_optimization_flag)' inline
        // option_setup 'optList *(enum e_optimization_flag, enum e_precision_type, int)' inline
        // line:24:26 ToString 'const std::string (enum e_optimization_flag)' inline
        // line:193:16 dss_create_ 'int (_MKL_DSS_HANDLE_t *, const int *)' extern
        // col:13 used MPIR_Call_world_errhand 'void (int)' extern
        // line:15:6 referenced extractsys 'void (struct expression *, mysys *)'

        // System.out.println(
        // "FUNCTION DECL DATA: " + getStdErr().get(StreamKeys.FUNCTION_DECL_DATA).get(node.getExtendedId()));

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        String functionName = parser.apply(StringParsers::parseWord);
        assert !functionName.isEmpty();

        Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());

        ListParser<ClavaNode> parsedChildren = new ListParser<>(parseChildren(node));
        checkNewChildren(node.getExtendedId(), parsedChildren.getList());

        FunctionDeclParserResult data = parser.apply(ClangDataParsers::parseFunctionDecl, parsedChildren, node,
                getStdErr());

        checkNumChildren(parsedChildren.getList(), 0);

        return ClavaNodeFactory.functionDecl(functionName, data.getParameters(), type, data.getFunctionDeclData(),
                declData, info(node), data.getDefinition());

        // Expects a ParmVarDecl child for each parameter, followed by a single node if it is a function definition
        // TODO: In reality, it expects a lot more stuff, implement on as-needed basis

        // Collect the ParmVarDecl children
        // List<ClavaNode> parsedChildren = parseChildren(node);
        // List<ParmVarDecl> parameters = parsedChildren.stream()
        // .filter(child -> child instanceof ParmVarDecl)
        // .map(parm -> (ParmVarDecl) parm)
        // .collect(Collectors.toList());

        // Check that number of children is the same or +1 than number of parameters
        // checkChildrenPlusOptionalNullNode(parsedChildren, parameters.size());
        // Preconditions.checkArgument(parameters.size() == parsedChildren.size()
        // || parameters.size() + 1 == parsedChildren.size(),
        // node.getLocation() + "-> Parameters size:" + parameters.size() + "; Children size:"
        // + parsedChildren.size() + "\nParameters:"
        // + parameters + "\nChildren:" + parsedChildren);

        // If numChildren greater than parameters, check that last child is not a ParmVarDecl
        // if (parameters.size() + 1 == parsedChildren.size()) {
        // Preconditions.checkArgument(!(parsedChildren.get(parsedChildren.size() - 1) instanceof ParmVarDecl));
        // }

        // We consider that the function has a definition (not only declaration)
        // if the number of parameters is different than the number of children
        // boolean hasDefinition = parameters.size() != parsedChildren.size();
        //
        // if (hasDefinition) {
        // ClavaNode definition = SpecsCollections.last(parsedChildren);
        //
        // if (definition instanceof Undefined) {
        // definition = ClavaNodeFactory.dummyStmt(definition);
        // }
        //
        // Preconditions.checkArgument(definition instanceof Stmt,
        // "Expected definition to be a Stmt, instead is '" + definition.getNodeName() + "'\nLocation:"
        // + node.getLocation());
        //
        // return ClavaNodeFactory.functionDecl(functionName, parameters, type, functionDeclData, declData, info(node),
        // (Stmt) definition);
        // }
        //
        // return ClavaNodeFactory.functionDecl(functionName, parameters, type, functionDeclData, declData, info(node));

    }

}
