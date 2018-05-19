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
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.RedeclarableTemplateDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class RedeclarableTemplateDeclParser extends AClangNodeParser<RedeclarableTemplateDecl> {

    public RedeclarableTemplateDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected RedeclarableTemplateDecl parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // line:100:10 Distance2

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);
        // System.out.println("HAS NAME:" + getStdErr().get(StdErrKeys.HAS_NAME));
        // boolean hasName = getStdErr().get(StdErrKeys.HAS_NAME).contains(node.getExtendedId());
        // String declName = hasName ? parser.apply(StringParsers::parseWord) : null;
        String declName = parser.apply(StringParsers::parseWord);

        Integer numberTemplateParameters = getStdErr().get(StreamKeys.NUMBER_TEMPLATE_PARAMETERS)
                .get(node.getExtendedId());
        Preconditions.checkNotNull(numberTemplateParameters,
                "No number of template parameters at " + node.getLocation());
        // Expected children
        // - Template Parameters (1 or more Decls)
        // - Template Decl (always 1)
        // - TemplateDeclSpecialization (NOT IMPLEMENTED)
        List<ClavaNode> children = parseChildren(node);
        List<NamedDecl> templateParameters = SpecsCollections.pop(children, numberTemplateParameters)
                .cast(NamedDecl.class);
        // Preconditions.checkArgument(templateParameters.size() == numberTemplateParameters,
        // "Expected " + numberTemplateParameters + " template parameters, has " + templateParameters.size());

        Decl templateDecl = SpecsCollections.popSingle(children, Decl.class);

        // Remaining decls are specialization decls, one for each specialization
        List<Decl> specializations = toDecl(children);

        // checkNumChildren(children, 0);
        // Preconditions.checkArgument(children.isEmpty(),
        // "Expected children to be empty, its size is " + children.size());

        return ClavaNodeFactory.redeclarableTemplateDecl(declName, specializations, declData, node.getInfo(),
                templateParameters, templateDecl);
    }

}
