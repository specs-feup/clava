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

package pt.up.fe.specs.clang.clavaparser.type;

import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.UnaryTransformType;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.enums.UnaryTransformTypeKindOld;
import pt.up.fe.specs.util.stringparser.StringParser;

public class UnaryTransformTypeParser extends AClangNodeParser<UnaryTransformType> {

    public UnaryTransformTypeParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected UnaryTransformType parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // '__underlying_type(enum Routing::ESpecificInfo)' sugar underlying_type
        // __underlying_type(enum Routing::Direction)' sugar underlying_type

        TypeData typeData = parser.apply(ClangDataParsers::parseType);
        UnaryTransformTypeKindOld kind = parser.apply(ClangGenericParsers::parseEnum, UnaryTransformTypeKindOld.getHelper());

        // Children
        List<ClavaNode> children = parseChildren(node);

        // If has sugar, first type is base type
        Type baseType = typeData.hasSugar() ? toType(children.remove(0)) : null;

        // Check if has underlying type
        Type underlyingType = kind == UnaryTransformTypeKindOld.ENUM_UNDERLYING_TYPE ? toType(children.remove(0)) : null;

        Preconditions.checkArgument(children.isEmpty(), "Expected chilren to be empty:" + children);

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.unaryTransformType(kind, typeData, node.getInfo(), baseType, underlyingType);
    }

}
