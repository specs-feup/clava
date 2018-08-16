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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.CXXNamedCastExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.CXXNamedCastExprData;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXNamedCastExprParser extends AClangNodeParser<CXXNamedCastExpr> {

    private static final Map<String, String> CAST_NAMES;
    static {
        CAST_NAMES = new HashMap<>();
        CAST_NAMES.put("CXXStaticCastExpr", "static_cast");
        CAST_NAMES.put("CXXReinterpretCastExpr", "reinterpret_cast");
        CAST_NAMES.put("CXXConstCastExpr", "const_cast");
    }

    public CXXNamedCastExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXNamedCastExpr parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // 'float' static_cast<float> <NoOp>
        // 'int32_t':'int' static_cast<int32_t> <NoOp>

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        String castName = CAST_NAMES.get(node.getName());
        Preconditions.checkNotNull(castName, "No cast name for node '" + node.getName() + "'");

        CXXNamedCastExprData cxxNamedCastExprData = parser.apply(ClangDataParsers::parseCXXNamedCastExpr, castName);

        List<ClavaNode> children = parseChildren(node);
        checkNumChildren(children, 1);

        Expr subExpr = toExpr(children.get(0));

        throw new RuntimeException("deprecated");
        // switch (node.getName()) {
        // case "CXXStaticCastExpr":
        // return ClavaNodeFactory.cxxStaticCastExpr(cxxNamedCastExprData, exprData, node.getInfo(), subExpr);
        // case "CXXReinterpretCastExpr":
        // return ClavaNodeFactory.cxxReinterpretCastExpr(cxxNamedCastExprData, exprData, node.getInfo(), subExpr);
        // case "CXXConstCastExpr":
        // return ClavaNodeFactory.cxxConstCastExpr(cxxNamedCastExprData, exprData, node.getInfo(), subExpr);
        // default:
        // throw new RuntimeException("Case not defined: " + node.getName());
        // }

    }

}
