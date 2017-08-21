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

import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.type.ElaboratedType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.TypeWithKeyword.ElaboratedTypeKeyword;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class ElaboratedTypeParser extends AClangNodeParser<ElaboratedType> {

    public ElaboratedTypeParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected ElaboratedType parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'std::string' sugar
        // 'typename class allocator<float>::value_type' sugar

        TypeData typeData = parser.apply(ClangDataParsers::parseType);
        // Clean type
        // type = Types.cleanElaborated(type);

        StringParser typeParser = new StringParser(Types.cleanElaborated(typeData.getBareType()));
        ElaboratedTypeKeyword keyword = typeParser.apply(ClangGenericParsers::checkEnum,
                ElaboratedTypeKeyword.getHelper(), ElaboratedTypeKeyword.NONE);

        // ElaboratedTypeKeyword keyword = keywordTry.orElse(ElaboratedTypeKeyword.NONE);

        // For not ignoring children
        List<ClavaNode> children = parseChildren(node);
        checkNumChildren(children, 1);

        Type namedType = toType(children.get(0));

        return ClavaNodeFactory.elaboratedType(keyword, typeData, node.getInfo(), namedType);

    }

    /*
    private static ElaboratedTypeKeywordOld parseKeyword(String type) {
        if (type.startsWith("struct")) {
            return ElaboratedTypeKeywordOld.STRUCT;
        }
    
        throw new RuntimeException("Case not defined: " + type);
    }
    */

}
