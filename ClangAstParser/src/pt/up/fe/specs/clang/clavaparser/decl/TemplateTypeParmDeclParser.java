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

import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.decl.TemplateTypeParmDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.extra.TemplateArgument;
import pt.up.fe.specs.clava.language.TemplateTypeParmKind;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class TemplateTypeParmDeclParser extends AClangNodeParser<TemplateTypeParmDecl> {

    public TemplateTypeParmDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected TemplateTypeParmDecl parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // col:20 referenced typename t
        // col:20 referenced typename t

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);
        TemplateTypeParmKind kind = parser.apply(ClangGenericParsers::parseEnum, TemplateTypeParmKind.getHelper());
        boolean isParameterPack = parser.apply(ClangGenericParsers::checkStringStarts, "...");

        // Name is optional
        String name = null;
        if (!parser.isEmpty()) {
            name = parser.apply(StringParsers::parseWord);
        }

        // Expected children
        // - default TemplateArgument (0 or 1)

        List<ClangNode> children = node.getChildren();
        TemplateArgument defaultArgument = null;
        if (!children.isEmpty()) {
            List<TemplateArgument> templateArgumentsNodes = parseTemplateArguments(children, 1);
            int numArgs = templateArgumentsNodes.size();
            Preconditions.checkArgument(numArgs == 1, "Expected list size to be 1, is " + numArgs);
            defaultArgument = templateArgumentsNodes.get(0);
        }

        Preconditions.checkArgument(children.isEmpty());

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.templateTypeParmDecl(kind, isParameterPack, name, declData, node.getInfo(),
        // defaultArgument);
    }

}
