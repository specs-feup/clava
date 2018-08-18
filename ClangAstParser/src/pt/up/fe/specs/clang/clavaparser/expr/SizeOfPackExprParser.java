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

package pt.up.fe.specs.clang.clavaparser.expr;

import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.SizeOfPackExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.extra.TemplateArgument;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class SizeOfPackExprParser extends AClangNodeParser<SizeOfPackExpr> {

    public SizeOfPackExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected SizeOfPackExpr parse(ClangNode node, StringParser parser) {

        // Example:
        //
        // 'unsigned long long' 0x75e9ba8 Values

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        Long packAddress = parser.apply(ClangGenericParsers::parseHex);

        String packId = packAddress + node.getIdSuffix();
        String packName = parser.apply(StringParsers::parseWord);

        List<ClavaNode> children = parseChildren(node);

        // Not tested yet, but it is expected that children should be template arguments
        List<TemplateArgument> partialArguments = SpecsCollections.cast(children, TemplateArgument.class);
        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.sizeOfPackExpr(packId, packName, exprData, node.getInfo(), partialArguments);
    }

}
