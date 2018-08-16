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

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.decl.AccessSpecDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.language.AccessSpecifier;
import pt.up.fe.specs.util.stringparser.StringParser;

public class AccessSpecDeclParser extends AClangNodeParser<AccessSpecDecl> {

    public AccessSpecDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected AccessSpecDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // col:9 public
        // col:9 private

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        AccessSpecifier accessSpecifier = parser.apply(ClangGenericParsers::parseEnum, AccessSpecifier.getHelper());

        checkNoChildren(node);
        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.accessSpecDecl(accessSpecifier, declData, node.getInfo());
    }

}
