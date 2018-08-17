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
import pt.up.fe.specs.clava.ast.decl.NamespaceDecl;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;
import pt.up.fe.specs.util.utilities.StringSlice;

public class NamespaceDeclParser extends AClangNodeParser<NamespaceDecl> {

    public NamespaceDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected NamespaceDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        // line:9:11 Routing

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);
        String namespace = parser.apply(StringParsers::parseWord);
        boolean isInline = parser.apply(ClangGenericParsers::checkWord, "inline");

        List<ClangNode> clangChildren = node.getChildren();
        BareDeclData originalNamespace = parseOriginalDecl(clangChildren);

        List<ClavaNode> children = parseChildren(clangChildren.stream());

        // boolean isOriginal = isOriginal(children);

        // List<ClavaNode> namespaceDecls = getDecls(children);

        // DOUBT: All nodes should decls, but due to ';' issues, some decls might be transformed into statements?
        // toDecl(namespaceDecls);
        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.namespaceDecl(isInline, originalNamespace, namespace, declData, node.getInfo(),
        // toDecl(children));
    }

    private BareDeclData parseOriginalDecl(List<ClangNode> clangChildren) {
        // If first child is not an 'original' node, return
        if (!clangChildren.get(0).getName().equals("original")) {
            return null;
        }

        // Remove node
        ClangNode originalNode = clangChildren.remove(0);

        // Parse BareDecl
        return ClangDataParsers.parseBareDecl(new StringSlice(originalNode.getContent())).getResult();
    }

}
