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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.InitListExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class InitListExprParser extends AClangNodeParser<InitListExpr> {

    public InitListExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected InitListExpr parse(ClangNode node, StringParser parser) {

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        boolean hasInitializedFieldInUnion = parser.apply(ClangGenericParsers::checkWord, "fields");
        BareDeclData fieldData = null;
        if (hasInitializedFieldInUnion) {
            fieldData = parser.apply(ClangDataParsers::parseBareDecl);
        }

        List<ClangNode> clangChildren = new ArrayList<>(node.getChildren());

        // Check if first child is an array filler
        Expr arrayFiller = getArrayFiller(clangChildren);

        List<ClavaNode> children = parseChildren(clangChildren.stream());

        List<Expr> initExprs = children.stream()
                .map(child -> toExpr(child))
                .collect(Collectors.toList());
        // List<Expr> initExprs = SpecsCollections.cast(children, Expr.class);

        return ClavaNodeFactory.initListExpr(hasInitializedFieldInUnion, arrayFiller, fieldData, exprData,
                node.getInfo(), initExprs);
    }

    private Expr getArrayFiller(List<ClangNode> children) {
        if (children.isEmpty()) {
            return null;
        }

        ClangNode firstChild = children.get(0);

        String fullName = firstChild.getName() + " " + firstChild.getContentTry().orElse("");

        if (!fullName.equals("array filler")) {
            return null;
        }

        // Remove first child
        children.remove(0);

        Preconditions.checkArgument(firstChild.getNumChildren() == 1,
                "Expected array filler node to have only one child: %s", firstChild);

        ClangNode expression = firstChild.getChild(0);

        return toExpr(parseChildren(Stream.of(expression)).get(0));
    }

}
