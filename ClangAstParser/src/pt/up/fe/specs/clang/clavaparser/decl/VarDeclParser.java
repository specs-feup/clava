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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.VarDeclData;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.QualType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.Qualifier;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class VarDeclParser extends AClangNodeParser<VarDecl> {

    public VarDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public VarDecl parse(ClangNode node, StringParser parser) {
        // Examples:
        // col:9 foo 'footype':'unsigned int' cinit
        // line:62:10 used d 'double' cinit
        // col:21 used travelTimes 'std::vector<float>':'class std::vector<float, class std::allocator<float> >' nrvo
        // callinit

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        String varName = parser.apply(StringParsers::parseWord);
        Type type = parser.apply(string -> ClangGenericParsers.parseClangType(string, node, getTypesMap()));

        VarDeclData varDeclData = parser.apply(ClangDataParsers::parseVarDecl, node, getStdErr());

        // System.out.println("NAME: " + varName);
        // System.out.println(
        // "QUALIFIED NAME: " + varDeclData.getVarDeclDumperInfo().getQualifiedName());

        // System.out.println(
        // "IS CONST EXPR? " + varDeclData.isConstexpr() + " - on " + varName + " line "
        // + node.getLocation().getStartLine());
        // Adapt 'const' in case VarDecl is constexpr

        // System.out.println("TYPE BEFORE:" + type + " (" + type.hashCode() + ")");
        if (varDeclData.isConstexpr()) {
            type = adaptTypeConstToConstexpr(varName, type);
            // type.getDescendantsAndSelf(Type.class).stream()
            // .forEach(VarDeclParser::adaptConstToConstexpr);
        }

        // System.out.println("TYPE AFTER:" + type + " (" + type.hashCode() + ")");

        List<ClavaNode> children = parseChildren(node);
        checkNewChildren(node.getExtendedId(), children);

        boolean hasInit = varDeclData.getInitKind() != InitializationStyle.NO_INIT;
        if (hasInit) {
            checkNumChildren(children, 1);
        } else {
            Preconditions.checkArgument(children.isEmpty());
        }

        Expr initExpr = hasInit ? toExpr(children.get(0)) : null;

        return ClavaNodeFactory.varDecl(varDeclData, varName, type, declData, info(node), initExpr);
    }

    private static Type adaptTypeConstToConstexpr(String name, Type type) {
        if (type.hasSugar()) {
            adaptTypeConstToConstexpr("desugared " + name, type.desugar());
        }

        if (!(type instanceof QualType)) {
            return type;
        }

        QualType qualType = (QualType) type;
        QualType copy = (QualType) qualType.copy();
        copy.setId(null);

        // System.out.println("For " + name + ": " + copy.getQualifiers());
        copy.setQualifiers(copy.getQualifiers().stream()
                .map(qual -> qual == Qualifier.CONST ? Qualifier.CONSTEXPR : qual)
                .collect(Collectors.toList()));

        if (qualType.hasParent()) {
            // System.out.println("Replacing in parent");
            NodeInsertUtils.replace(qualType, copy);
        }

        return copy;
    }
    // private static void adaptConstToConstexpr(Type type) {
    // if (!(type instanceof QualType)) {
    // return;
    // }
    //
    // QualType qualType = (QualType) type;
    // // qualType.setQualifiers(qualType.getQualifiers().stream()
    // // .map(qual -> qual == Qualifier.CONST ? Qualifier.CONSTEXPR : qual)
    // // .collect(Collectors.toList()));
    // }
}
