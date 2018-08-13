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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.EnumConstantDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class EnumConstantDeclParser extends AClangNodeParser<EnumConstantDecl> {

    public EnumConstantDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public EnumConstantDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        // col:3 NONE 'enum e_optimization_flag'
        // col:3 LONGINT_TYPE 'enum e_precision_type'

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        String value = parser.apply(StringParsers::parseWord);
        Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());

        if (node.getNumChildren() == 0) {
            throw new RuntimeException("deprecated");
            // return ClavaNodeFactory.enumConstantDecl(value, type, declData, info(node));
        }

        // EnumConstant can have an initialization
        Preconditions.checkArgument(node.getNumChildren() == 1, "Only expecting one Expr child");

        ClavaNode initExpr = getConverter().parse(node.getChild(0));

        Preconditions.checkArgument(initExpr instanceof Expr, "Expected an expression node:\n" + initExpr);

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.enumConstantDecl(value, type, declData, info(node), (Expr) initExpr);
    }

}
