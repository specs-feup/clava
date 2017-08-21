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
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.UnaryExprOrTypeTraitExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.UnaryExprOrTypeTrait;
import pt.up.fe.specs.util.stringparser.StringParser;

public class UnaryExprOrTypeTraitExprParser extends AClangNodeParser<UnaryExprOrTypeTraitExpr> {

    public UnaryExprOrTypeTraitExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected UnaryExprOrTypeTraitExpr parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'unsigned long long' sizeof 'int'
        // 'unsigned long long' sizeof 'float *'

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        UnaryExprOrTypeTrait uettKind = parser.apply(ClangGenericParsers::parseEnum, UnaryExprOrTypeTrait.getHelper());

        String argTypeId = getStdErr().get(StreamKeys.UNARY_OR_TYPE_TRAIT_ARG_TYPES).get(node.getExtendedId());
        boolean isArgType = !parser.isEmpty();
        if (isArgType) {
            parser.apply(ClangGenericParsers::dropClangType);
        }
        Type argType = argTypeId == null ? null : getOriginalTypes().get(argTypeId);

        Preconditions.checkArgument(isArgType == (argType != null),
                "Expected that if is an argument type, for the type to be non-null");

        // if (argType != null) {
        // System.out.println("ARG TYPE:\n" + argType);
        // } else {
        // System.out.println("NOT ARG TYPE");
        // }

        // Parse type
        // Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());
        // Get name of expression
        // String exprName = parser.apply(StringParsers::parseWord);
        // String argType = parser.apply(ClangGenericParsers::parsePrimes);
        // Type argType = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());

        List<ClavaNode> children = parseChildren(node);
        checkChildrenBetween(children, 0, 1);

        Expr argumentExpression = children.isEmpty() ? null : toExpr(children.get(0));

        return ClavaNodeFactory.unaryExprOrTypeTraitExpr(uettKind, argType, exprData, node.getInfo(),
                argumentExpression);
        /*
        if (children.isEmpty()) {
            return ClavaNodeFactory.unaryExprOrTypeTraitExpr(exprName, argType, type, node.getInfo());
        }
        
        Expr argumentExpression = toExpr(children.get(0));
        
        return ClavaNodeFactory.unaryExprOrTypeTraitExpr(exprName, argType, type, node.getInfo(), argumentExpression);
        */
    }

}
