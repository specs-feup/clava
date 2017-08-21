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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.StringLiteral;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class StringLiteralParser extends AClangNodeParser<StringLiteral> {

    public StringLiteralParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public StringLiteral parse(ClangNode node, StringParser parser) {
        // Examples:
        // 'const wchar_t [14]' lvalue L"a_wide_string"
        // 'const char [4]' lvalue "-O2"

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        String string = parser.apply(ClangGenericParsers::parseRemaining);
        Preconditions.checkArgument(node.getNumChildren() == 0, "Expected no children");

        return ClavaNodeFactory.stringLiteral(string, exprData, info(node));
    }

}
