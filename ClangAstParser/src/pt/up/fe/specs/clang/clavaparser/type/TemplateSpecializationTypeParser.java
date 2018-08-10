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

package pt.up.fe.specs.clang.clavaparser.type;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.TemplateArgument;
import pt.up.fe.specs.clava.ast.type.TemplateSpecializationType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class TemplateSpecializationTypeParser extends AClangNodeParser<TemplateSpecializationType> {

    public TemplateSpecializationTypeParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected TemplateSpecializationType parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'vector<std::string>' sugar vector
        // 'basic_ostream<char, struct std::char_traits<char> >' sugar basic_ostream
        // PARSER:'rebind<long *>' sugar alias rebind

        TypeData typeData = parser.apply(ClangDataParsers::parseType);

        boolean isTypeAlias = parser.apply(ClangGenericParsers::checkWord, "alias");
        String templateName = parser.apply(StringParsers::parseWord);
        // parser.apply(ClangParseWorkers::clear);

        List<String> templateArguments = getClangRootData().getTemplateNames().get(node.getExtendedId());

        Preconditions.checkArgument(!templateArguments.isEmpty(),
                "TypeDumper not supporting tree structure down to node " + node.getExtendedId());
        List<String> templateArgumentTypes = getClangRootData().getTemplateArgTypes().get(node.getExtendedId());

        List<ClangNode> clangChildren = new ArrayList<>(node.getChildren());

        List<TemplateArgument> templateArgumentsNodes = parseTemplateArguments(clangChildren, templateArguments.size());

        // // Remove attributes from original children
        // List<ClangNode> attributesUnparsed = new ArrayList<>();
        // for (int i = 0; i < templateArguments.size(); i++) {
        // attributesUnparsed.add(clangChildren.remove(0));
        // }
        //
        // // Parse Template Arguments
        // TemplateArgumentParser templateArgParser = new TemplateArgumentParser(getConverter());
        // List<TemplateArgument> templateArgumentsNodes = attributesUnparsed.stream()
        // .map(attrClang -> (TemplateArgument) templateArgParser.parse(attrClang))
        // .collect(Collectors.toList());

        List<ClavaNode> remainingChildren = parseChildren(clangChildren.stream());

        // List<ClavaNode> children = parseChildren(node);

        // Find number of template arguments
        // int templateArgsEndIndex = IntStream.range(0, children.size())
        // // Find first index that is not of a TemplateArgument
        // .filter(index -> !(children.get(index) instanceof TemplateArgument))
        // .findFirst()
        // .orElse(children.size());
        //
        // Preconditions.checkArgument(templateArguments.size() == templateArgsEndIndex, "Found '"
        // + templateArguments.size() + "' template names, but '" + templateArgsEndIndex + "' template nodes.");
        //
        // List<TemplateArgument> templateArgumentsNodes = CollectionUtils.cast(TemplateArgument.class,
        // children.subList(0, templateArgsEndIndex));

        // List<ClavaNode> remainingChildren = CollectionUtils.subList(children, templateArgsEndIndex);

        Type aliasedType = null;
        if (isTypeAlias) {
            Preconditions.checkArgument(!remainingChildren.isEmpty(), "Expected at least one node");

            aliasedType = toType(remainingChildren.get(0));
            remainingChildren = SpecsCollections.subList(remainingChildren, 1);
        }

        Type desugaredType = null;
        if (typeData.hasSugar()) {
            desugaredType = toType(remainingChildren.get(0));
            remainingChildren = SpecsCollections.subList(remainingChildren, 1);

        }

        Preconditions.checkArgument(remainingChildren.isEmpty(), "Did not expect more children:" + remainingChildren);

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.templateSpecializationType(templateName, templateArguments, typeData,
        // node.getInfo(), templateArgumentsNodes, aliasedType, desugaredType);
    }

}
