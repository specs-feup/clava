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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.data.VarDeclData;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class ParmVarDeclParser extends AClangNodeParser<ParmVarDecl> {

    public ParmVarDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public ParmVarDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        // col:55 used e 'enum e_optimization_flag':'enum e_optimization_flag'
        // col:24 seed 'int'
        // col:7 'class Molecule &&'
        // col:28 used separator 'char' cinit
        // col:28 referenced a 'const t *':'const t *'

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        // Name can be optional, check if there is not a type
        String varName = null;
        if (!parser.apply(StringParsers::peekStartsWith, "'")) {
            varName = parser.apply(StringParsers::parseWord);
        }
        // String varName = parser.apply(StringParsers::parseWord);

        Type type = parser.apply(string -> ClangGenericParsers.parseClangType(string, node, getTypesMap()));

        VarDeclData varDeclData = parser.apply(ClangDataParsers::parseVarDecl);

        List<ClavaNode> children = parseChildren(node);

        boolean hasInit = varDeclData.getInitKind() != InitializationStyle.NO_INIT;
        if (hasInit) {
            checkNumChildren(children, 1);
        } else {
            Preconditions.checkArgument(children.isEmpty());
        }

        Expr initExpr = hasInit ? toExpr(children.get(0)) : null;

        boolean hasInheritedDefaultArg = getStdErr().get(StreamKeys.PARM_VAR_DECL_HAS_INHERITED_DEFAULT_ARG)
                .contains(node.getExtendedId());

        return ClavaNodeFactory.parmVarDecl(hasInheritedDefaultArg, varDeclData, varName, type, declData,
                node.getInfo(), initExpr);

        /*
        // return ClavaNodeFactory.varDecl(varDeclData, varName, type, declData, info(node), initExpr);
        
        String name = "";
        
        // If there is no prime ('), it means there is a name
        if (!parser.getCurrentString().startsWith("'")) {
            name = parser.apply(StringParsers::parseWord);
        }
        
        // List<Type> types = parser.apply(string -> ClangParseWorkers.parseClangType(string, node.getLocation()));
        Type type = parser.apply(string -> ClangGenericParsers.parseClangType(string, node, getTypesMap()));
        // Optionally, can have a initialization type
        InitializationStyle initStyle = parser.apply(ClangGenericParsers::parseInitializationStyle);
        
        // Can have a child, the initialization value
        List<ClavaNode> children = parseChildren(node);
        Preconditions.checkArgument(children.size() == 1 || children.isEmpty(),
                "Expects at most one child:" + node);
        
        if (children.isEmpty()) {
            return ClavaNodeFactory.parmVarDecl(name, declData, type, info(node));
        }
        
        return ClavaNodeFactory.parmVarDecl(name, initStyle, declData, type, info(node), toExpr(children.get(0)));
        */
    }

}
