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
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clang.streamparser.data.ExceptionSpecifierInfo;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data.FunctionProtoTypeData;
import pt.up.fe.specs.clava.ast.type.data.FunctionTypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;

public class FunctionProtoTypeParser extends AClangNodeParser<FunctionProtoType> {

    public FunctionProtoTypeParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected FunctionProtoType parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'std::size_t (void) const' const cdecl
        // 'void (int) __attribute__((noreturn))' noreturn cdecl
        TypeData typeData = parser.apply(ClangDataParsers::parseType);
        FunctionProtoTypeData functionProtoTypeData = parser.apply(ClangDataParsers::parseFunctionProtoType);

        ExceptionSpecifierInfo exceptionInfo = getStdErr().get(StreamKeys.FUNCTION_PROTOTYPE_EXCEPTION)
                .get(node.getExtendedId());

        if (exceptionInfo != null) {
            functionProtoTypeData.setSpecifier(exceptionInfo.getSpecifier());
            functionProtoTypeData.setNoexceptExpr(exceptionInfo.getNoexceptExpr());
        }

        FunctionTypeData functionTypeData = parser.apply(ClangDataParsers::parseFunctionType);

        // TODO: Build parser for FunctionType, use it here and in FunctionNoProtoType
        // boolean hasNoReturn = parser.apply(ClangGenericParsers::checkWord, "noreturn");
        // FunctionTypeData ftData = new FunctionTypeData(hasNoReturn, isConst, callingConv);

        List<ClavaNode> children = parseChildren(node);
        checkAtLeast(children, 1);

        Type returnType = toType(children.get(0));
        List<Type> arguments = toType(SpecsCollections.subList(children, 1));

        return ClavaNodeFactory.functionProtoType(functionProtoTypeData, functionTypeData, typeData, node.getInfo(),
                returnType, arguments);
    }

}
