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
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.MaterializeTemporaryExpr;
import pt.up.fe.specs.clava.ast.expr.data.ValueKind;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class MaterializeTemporaryExprParser extends AClangNodeParser<MaterializeTemporaryExpr> {

    public MaterializeTemporaryExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public MaterializeTemporaryExpr parse(ClangNode node, StringParser parser) {
        // Examples:
        // 'class std::__cxx11::basic_string<char>' xvalue
        // struct std::_Rb_tree_const_iterator<struct std::pair<const int, class Atom> >' xvalue
        // 'const _Self':'const struct std::_Rb_tree_iterator<struct std::pair<const int, class Atom> >' lvalue
        // 'class std::__cxx11::list<class Edge *, class std::allocator<class Edge *> >':'class std::__cxx11::list<class
        // Edge *, class std::allocator<class Edge *> >' xvalue extended by Var 0x49c2d28 '__range' 'class
        // std::__cxx11::list<class Edge *, class std::allocator<class Edge *> > &&'

        Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());
        ValueKind valueKind = parser.apply(ClangGenericParsers::parseValueKind);

        boolean isExtended = parser.apply(string -> ClangGenericParsers.checkWord(string, "extended")) &&
                parser.apply(string -> ClangGenericParsers.checkWord(string, "by"));

        if (isExtended) {

            // EXTRA
            // Parent
            parser.apply(StringParsers::parseWord);
            // Address
            parser.apply(ClangGenericParsers::parseHex);

            // Parent Type 1
            parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());
            // Parent Type 2
            parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());
        }

        List<ClavaNode> children = parseChildren(node);

        Preconditions.checkArgument(children.size() == 1, "Expected single child");

        Expr temporaryExpr = toExpr(children.get(0));

        return ClavaNodeFactory.materializeTemporaryExpr(valueKind, type, info(node), temporaryExpr);
    }

}
