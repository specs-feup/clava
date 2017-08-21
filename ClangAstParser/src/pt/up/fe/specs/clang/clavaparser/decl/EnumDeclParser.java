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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.EnumConstantDecl;
import pt.up.fe.specs.clava.ast.decl.EnumDecl;
import pt.up.fe.specs.clava.ast.type.EnumType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class EnumDeclParser extends AClangNodeParser<EnumDecl> {

    public EnumDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public EnumDecl parse(ClangNode node, StringParser parser) {
        // Examples:
        // line:17:6 e_optimization_flag
        // line:16:12 class hi_world 'int'
        // line:16:6 hi_world 'short'

        // Drop location
        parser.apply(ClangGenericParsers::parseLocation);
        boolean isClass = parser.apply(string -> ClangGenericParsers.checkStringStarts(string, "class "));

        String name = parser.apply(StringParsers::parseWord);

        EnumType type = (EnumType) parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());

        // If the name is not defined in the declaration, use the name in the type
        if (name.isEmpty()) {
            name = type.getBareType();
        }

        List<EnumConstantDecl> enumConstants = SpecsCollections.cast(parseChildren(node), EnumConstantDecl.class);

        ClavaNodeInfo info = info(node);

        // Get integer type
        String integerTypeId = getClangRootData().getEnumToIntegerType().get(node.getExtendedId());
        Preconditions.checkNotNull(integerTypeId, "No integer type for enum at " + node.getLocation());
        Type integerType = getOriginalTypes().get(integerTypeId);
        Preconditions.checkNotNull(integerType, "No type for integer type for enum at " + node.getLocation());

        // Build EnumType
        // List<EnumType> enumTypes = type.stream()
        // .map(underT -> ClavaNodeFactory.enumType(name, info, underT))
        // .collect(Collectors.toList());

        // If underlying type is empty, create empty enum type
        // if (type.isEmpty()) {
        // throw new RuntimeException("Expected at least one type");
        // // enumTypes = Arrays.asList(ClavaNodeFactory.enumType(name, info));
        // }

        // if (!(type.get(0) instanceof EnumType)) {
        // throw new RuntimeException("Expected EnumType, found " + type.get(0));
        // }

        // List<EnumType> enumTypes = Arrays.asList((EnumType) type.get(0));
        // System.out.println("ENUMTYPE DECL TYPE:" + getTypesMap().get(enumTypes.get(0).getDeclInfo().getDeclId()));

        // System.out.println("DECL TYPE:" + enumTypes.get(0).getDeclInfo().getDeclType());

        return ClavaNodeFactory.enumDecl(isClass, name, integerType, type, info, enumConstants);
    }

}
