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

import java.util.Collections;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.data2.BuiltinTypeData;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.util.stringparser.StringParser;

public class BuiltinTypeParser extends AClangNodeParser<BuiltinType> {

    // "Cache" value
    private final Standard standard;

    public BuiltinTypeParser(ClangConverterTable converter) {
        super(converter);

        standard = getClangRootData().getConfig().get(ClavaOptions.STANDARD);
    }

    @Override
    public BuiltinType parse(ClangNode node, StringParser parser) {
        // Examples:
        // 'unsigned int'
        // 'int'

        // TODO: Temporary
        BuiltinTypeData data = getDataTry(BuiltinTypeData.class, node).orElse(null);

        TypeData typeData = parser.apply(ClangDataParsers::parseType);
        typeData = new TypeData(parseType(typeData.getBareType()), typeData);
        // String parsedType = parseType(type);

        checkNoChildren(node);

        if (data != null) {
            return new BuiltinType(data, Collections.emptyList());
        }

        return ClavaNodeFactory.builtinType(typeData, node.getInfo());

    }

    private String parseType(String type) {

        // C++ uses bool and not _Bool
        if (type.equals("_Bool") && standard.isCxx()) {
            return "bool";
        }

        return type;
    }

}
