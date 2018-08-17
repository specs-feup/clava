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
import pt.up.fe.specs.clava.ast.decl.UsingDirectiveDecl;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class UsingDirectiveDeclParser extends AClangNodeParser<UsingDirectiveDecl> {

    public UsingDirectiveDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected UsingDirectiveDecl parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // col:17 Namespace 0x34f9488 'std'

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        BareDeclData bareDeclData = parser.apply(ClangDataParsers::parseBareDecl);

        String declName = bareDeclData.getDeclNameTry().get();

        checkNoChildren(node);

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.usingDirectiveDecl(declName, declData, node.getInfo());
    }

}
