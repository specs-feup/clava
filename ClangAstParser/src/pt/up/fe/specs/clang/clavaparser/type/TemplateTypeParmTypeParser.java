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

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.type.TemplateTypeParmType;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;
import pt.up.fe.specs.util.stringparser.StringParser;

public class TemplateTypeParmTypeParser extends AClangNodeParser<TemplateTypeParmType> {

    public TemplateTypeParmTypeParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected TemplateTypeParmType parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // '_CharT' dependent depth 0 index 0
        // '_Args' dependent contains_unexpanded_pack depth 0 index 0 pack

        TypeData typeData = parser.apply(ClangDataParsers::parseType);

        parser.apply(ClangGenericParsers::ensureWord, "depth");
        int depth = parser.apply(ClangGenericParsers::parseInt);
        parser.apply(ClangGenericParsers::ensureWord, "index");
        int index = parser.apply(ClangGenericParsers::parseInt);

        boolean isPacked = parser.apply(ClangGenericParsers::checkWord, "pack");

        // TemplateParmData tData = new TemplateParmData(depth, index, isPacked);

        // One child, that is a DeclInfo
        checkNumChildren(node.getChildren(), 1);
        DeclRef declInfo = parseDeclRef(node.getChild(0));

        throw new RuntimeException("deprecated, node " + node.getExtendedId());
        // return ClavaNodeFactory.templateTypeParmType(tData, declInfo, typeData, node.getInfo());
    }

}
