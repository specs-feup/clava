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

package pt.up.fe.specs.clang.clavaparser.decl;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.UsingDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.NestedNamedSpecifier;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class UsingDeclParser extends AClangNodeParser<UsingDecl> {

    public UsingDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected UsingDecl parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // col:12 std::ostream

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);
        NestedNamedSpecifier qualifier = parser.apply(ClangGenericParsers::parseEnum,
                NestedNamedSpecifier.getEnumHelper(), NestedNamedSpecifier.NONE);

        String declName = parser.apply(StringParsers::parseWord);

        checkNoChildren(node);

        return ClavaNodeFactory.usingDecl(qualifier, declName, declData, node.getInfo());
    }

}
