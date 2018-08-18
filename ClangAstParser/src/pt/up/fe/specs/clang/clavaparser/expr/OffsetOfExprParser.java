/**
 * Copyright 2017 SPeCS.
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
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clang.streamparser.data.OffsetOfClangComponent;
import pt.up.fe.specs.clang.streamparser.data.OffsetOfInfo;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data.OffsetOfData;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfComponent;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;
import pt.up.fe.specs.util.stringparser.StringParser;

public class OffsetOfExprParser extends AClangNodeParser<ClavaNode> {

    public OffsetOfExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected ClavaNode parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'unsigned long long'
        // System.out.println("OFFSET NODE:" + node);
        // If parsing is C++, offsetof is not supported
        // Standard standard = getClangRootData().getConfig().get(ClavaOptions.STANDARD);
        // if (standard.isCxx()) {
        // throw new RuntimeException(
        // "Clava does not support parsing of function offsetof() in C++ (current standard: " + standard
        // + ")");
        // }

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        OffsetOfInfo info = getStdErr().get(StreamKeys.OFFSET_OF_INFO).get(node.getExtendedId());

        Type sourceType = getOriginalTypes().get(info.getTypeId());

        List<ClavaNode> children = parseChildren(node);
        List<Expr> expressions = toExpr(children);

        checkNumChildren(children, info.getNumExpressions());

        // Create OffsetOfData
        OffsetOfData offsetOfData = createOffsetOfData(sourceType, info, expressions);

        // Should always have a child, if it is empty probably it is being parsed as C++ and gave a warning
        // if (children.isEmpty()) {
        // throw new RuntimeException(
        // "Could not parse offsetof() function, check if there were C++ warnings related with non-standard-layout
        // types");
        // }
        //
        // System.out.println("OFFSET LOCATION:" + node.getLocation());

        // Preconditions.checkArgument(children.size() == 1, "Expected only one child in OffsetOfExpr, got: " +
        // children);
        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.offsetOfExpr(offsetOfData, exprData, node.getInfo());
    }

    private OffsetOfData createOffsetOfData(Type sourceType, OffsetOfInfo info, List<Expr> expressions) {
        return new OffsetOfData(sourceType, info.getComponents().stream()
                .map(clangOffset -> parser(clangOffset, expressions))
                .collect(Collectors.toList()));

    }

    private OffsetOfComponent parser(OffsetOfClangComponent component, List<Expr> expressions) {
        switch (component.getKind()) {
        // case ARRAY:
        // return new OffsetOfArray(expressions.get(component.getExpressionIndex()));
        // case FIELD:
        // return new OffsetOfField(component.getFieldName());
        default:
            throw new CaseNotDefinedException(component.getKind());
        }
    }

}
