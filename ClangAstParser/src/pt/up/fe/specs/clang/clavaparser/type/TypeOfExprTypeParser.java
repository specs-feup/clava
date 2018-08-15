/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang.clavaparser.type;

import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.TypeOfExprType;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.util.stringparser.StringParser;

public class TypeOfExprTypeParser extends AClangNodeParser<TypeOfExprType> {

    public TypeOfExprTypeParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected TypeOfExprType parse(ClangNode node, StringParser parser) {
        // Example:
        //
        // 'typeof (x1)' sugar

        TypeData typeData = parser.apply(ClangDataParsers::parseType);

        Standard standard = getConfig().get(ClavaOptions.STANDARD);

        List<ClavaNode> children = parseChildren(node);

        checkNumChildren(children, 2);

        Expr underlyingExpr = toExpr(children.get(0));
        Type underlyingType = toType(children.get(1));

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.typeOfExprType(standard, typeData, node.getInfo(), underlyingExpr, underlyingType);
    }

}
