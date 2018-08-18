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
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clava.ast.decl.NamespaceAliasDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class NamespaceAliasDeclParser extends AClangNodeParser<NamespaceAliasDecl> {

    public NamespaceAliasDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected NamespaceAliasDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // col:11 fbz

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);
        String name = parser.apply(StringParsers::parseWord);

        // Get nested prefix
        String nestedPrefix = getStdErr().get(StreamKeys.NAMESPACE_ALIAS_PREFIX).get(node.getExtendedId());

        // Children:
        // DeclRef (1)

        DeclRef declInfo = parseDeclRef(node.getChild(0));

        checkNumChildren(node.getChildren(), 1);
        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.namespaceAliasDecl(nestedPrefix, declInfo, name, declData, node.getInfo());
    }

}
