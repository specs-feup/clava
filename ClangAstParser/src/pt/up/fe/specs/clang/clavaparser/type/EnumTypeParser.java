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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ast.type.EnumType;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;
import pt.up.fe.specs.util.stringparser.StringParser;

public class EnumTypeParser extends AClangNodeParser<EnumType> {

    public EnumTypeParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected EnumType parse(ClangNode node, StringParser parser) {

        TypeData typeData = parser.apply(ClangDataParsers::parseType);

        // Only one child, that must be parsed manually
        Preconditions.checkArgument(node.getChildren().size() == 1, "Expected a single child");

        DeclRef declInfo = parseDeclRef(node.getChild(0));

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.enumType(declInfo, typeData, node.getInfo());
    }

}
