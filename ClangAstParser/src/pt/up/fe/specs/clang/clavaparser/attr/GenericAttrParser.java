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

package pt.up.fe.specs.clang.clavaparser.attr;

import java.util.Collections;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clava.ast.attr.GenericAttribute;
import pt.up.fe.specs.clava.ast.attr.data.AttrData;
import pt.up.fe.specs.clava.ast.lang.AttributeKind;
import pt.up.fe.specs.util.stringparser.StringParser;

public class GenericAttrParser extends AClangNodeParser<GenericAttribute> {

    public GenericAttrParser(ClangConverterTable converter) {
        super(converter, true);
    }

    @Override
    protected GenericAttribute parse(ClangNode node, StringParser parser) {

        // Examples:
        //

        // Never has content?

        AttrData attrData = new AttrData();

        // The remaining string represents an attribute
        // Expects attribute string to end with Attr
        // parser.apply(ClangGenericParsers::checkStringEndsStrict, "Attr");

        // The string should be the name of the attribute kind, with the first character in lower case
        String remainingString = parser.toString();
        String attrName = remainingString.substring(0, 1).toUpperCase() + remainingString.substring(1);
        AttributeKind kind = AttributeKind.getHelper().fromName(attrName);
        // AttributeKind kind = parser.apply(ClangGenericParsers::parseEnum, AttributeKind.getHelper());
        System.out.println("CHILDREN:" + node.getChildren());
        checkNoChildren(node);

        return new GenericAttribute(kind, attrData, node.getInfo(), Collections.emptyList());
    }

}
