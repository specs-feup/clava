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

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.TypeAliasDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class TypeAliasDeclParser extends AClangNodeParser<TypeAliasDecl> {

    public TypeAliasDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected TypeAliasDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // col:10 referenced result_t 'std::vector<float>':'class std::vector<float, class std::allocator<float> >'

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        String declName = parser.apply(StringParsers::parseWord);
        Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());

        List<ClavaNode> children = parseChildren(node);
        checkNumChildren(children, 1);
        Type aliasedType = toType(children.get(0));

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.typeAliasDecl(aliasedType, declName, type, declData, node.getInfo());
    }

}
