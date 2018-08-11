/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang.parsers.data;

import java.math.BigInteger;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParsers;

import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clang.parsers.NodeDataParser;
import pt.up.fe.specs.clava.ast.type.ArrayType;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.ConstantArrayType;
import pt.up.fe.specs.clava.ast.type.ElaboratedType;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.QualType;
import pt.up.fe.specs.clava.ast.type.TagType;
import pt.up.fe.specs.clava.ast.type.TemplateSpecializationType;
import pt.up.fe.specs.clava.ast.type.TemplateTypeParmType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.TypeWithKeyword;
import pt.up.fe.specs.clava.ast.type.TypedefType;
import pt.up.fe.specs.clava.ast.type.enums.AddressSpaceQualifierV2;
import pt.up.fe.specs.clava.ast.type.enums.ArraySizeModifier;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.ast.type.enums.C99Qualifier;
import pt.up.fe.specs.clava.ast.type.enums.CallingConvention;
import pt.up.fe.specs.clava.ast.type.enums.ElaboratedTypeKeyword;
import pt.up.fe.specs.clava.ast.type.enums.TypeDependency;
import pt.up.fe.specs.clava.language.ReferenceQualifier;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * ClavaData parsers for Type nodes.
 * 
 * 
 * @author JoaoBispo
 *
 */
public class TypeDataParser {

    public static DataStore parseTypeData(LineStream lines, ClangParserData dataStore) {

        // Types do not have location
        DataStore clavaData = NodeDataParser.parseNodeData(lines, false, dataStore);

        clavaData.add(Type.TYPE_AS_STRING, lines.nextLine());
        clavaData.add(Type.HAS_SUGAR, LineStreamParsers.oneOrZero(lines));
        clavaData.add(Type.TYPE_DEPENDENCY, LineStreamParsers.enumFromName(TypeDependency.class, lines));
        clavaData.add(Type.IS_VARIABLY_MODIFIED, LineStreamParsers.oneOrZero(lines));
        clavaData.add(Type.CONTAINS_UNEXPANDED_PARAMETER_PACK, LineStreamParsers.oneOrZero(lines));
        clavaData.add(Type.IS_FROM_AST, LineStreamParsers.oneOrZero(lines));

        dataStore.getClavaNodes().queueSetNode(clavaData, Type.UNQUALIFIED_DESUGARED_TYPE, lines.nextLine());

        return clavaData;
    }

    public static DataStore parseBuiltinTypeData(LineStream lines, ClangParserData dataStore) {

        DataStore data = parseTypeData(lines, dataStore);

        // data.add(BuiltinType.KIND_ORDINAL, GeneralParsers.parseInt(lines));
        // data.add(BuiltinType.KIND, GeneralParsers.enumFromValue(BuiltinKind.getHelper(), lines));
        data.add(BuiltinType.KIND, LineStreamParsers.enumFromName(BuiltinKind.class, lines));
        data.add(BuiltinType.KIND_LITERAL, lines.nextLine());

        return data;
    }

    public static DataStore parsePointerTypeData(LineStream lines, ClangParserData dataStore) {

        DataStore data = parseTypeData(lines, dataStore);

        // dataStore.getClavaNodes().queueSetNode(data, PointerType.POINTEE_TYPE, lines.nextLine());

        return data;
    }

    public static DataStore parseQualTypeData(LineStream lines, ClangParserData dataStore) {

        DataStore data = parseTypeData(lines, dataStore);

        data.add(QualType.C99_QUALIFIERS, LineStreamParsers.enumListFromName(C99Qualifier.getHelper(), lines));
        data.add(QualType.ADDRESS_SPACE_QUALIFIER, LineStreamParsers.enumFromName(AddressSpaceQualifierV2.getHelper(),
                lines));
        data.add(QualType.ADDRESS_SPACE, LineStreamParsers.longInt(lines));

        dataStore.getClavaNodes().queueSetNode(data, QualType.UNQUALIFIED_TYPE, lines.nextLine());

        return data;

    }

    public static DataStore parseFunctionTypeData(LineStream lines, ClangParserData parserData) {

        DataStore data = parseTypeData(lines, parserData);

        data.add(FunctionType.IS_CONST, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionType.IS_VOLATILE, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionType.IS_RESTRICT, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionType.NO_RETURN, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionType.PRODUCES_RESULT, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionType.HAS_REG_PARM, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionType.REG_PARM, LineStreamParsers.longInt(lines));
        data.add(FunctionType.CALLING_CONVENTION, LineStreamParsers.enumFromName(CallingConvention.getHelper(), lines));

        return data;
    }

    public static DataStore parseFunctionProtoTypeData(LineStream lines, ClangParserData parserData) {

        DataStore data = parseFunctionTypeData(lines, parserData);

        data.add(FunctionProtoType.NUM_PARAMETERS, LineStreamParsers.integer(lines));

        data.add(FunctionProtoType.HAS_TRAILING_RETURNS, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionProtoType.IS_VARIADIC, LineStreamParsers.oneOrZero(lines));
        // data.add(FunctionProtoType.IS_CONST, LineStreamParsers.oneOrZero(lines));
        // data.add(FunctionProtoType.IS_VOLATILE, LineStreamParsers.oneOrZero(lines));
        // data.add(FunctionProtoType.IS_RESTRICT, LineStreamParsers.oneOrZero(lines));

        data.add(FunctionProtoType.REFERENCE_QUALIFIER,
                LineStreamParsers.enumFromName(ReferenceQualifier.class, lines));

        data.add(FunctionProtoType.EXCEPTION_SPECIFICATION, ClavaDataParsers.exceptionSpecification(lines, parserData));

        /*
        
        
        
        public final static DataKey<Expr> NOEXCEPT_EXPR = KeyFactory.object("noexceptExpr", Expr.class);
        */

        return data;
    }

    public static DataStore parseArrayTypeData(LineStream lines, ClangParserData parserData) {

        DataStore data = parseTypeData(lines, parserData);

        data.add(ArrayType.ARRAY_SIZE_MODIFIER, LineStreamParsers.enumFromName(ArraySizeModifier.class, lines));
        data.add(ArrayType.INDEX_TYPE_QUALIFIERS, LineStreamParsers.enumListFromName(C99Qualifier.getHelper(), lines));

        return data;

    }

    public static DataStore parseConstantArrayTypeData(LineStream lines, ClangParserData parserData) {

        DataStore data = parseArrayTypeData(lines, parserData);

        data.add(ConstantArrayType.ARRAY_SIZE, new BigInteger(lines.nextLine()));

        return data;

    }

    public static DataStore parseVariableArrayTypeData(LineStream lines, ClangParserData parserData) {

        DataStore data = parseArrayTypeData(lines, parserData);

        // data.add(VariableArrayType.SIZE_EXPR, ClavaNodes.getExpr(parserData, lines.nextLine()));

        return data;

    }

    public static DataStore parseTagTypeData(LineStream lines, ClangParserData parserData) {

        DataStore data = parseTypeData(lines, parserData);

        parserData.getClavaNodes().queueSetNode(data, TagType.DECL, lines.nextLine());

        return data;
    }

    public static DataStore parseTypeWithKeywordData(LineStream lines, ClangParserData parserData) {

        DataStore data = parseTypeData(lines, parserData);

        data.add(TypeWithKeyword.ELABORATED_TYPE_KEYWORD,
                LineStreamParsers.enumFromName(ElaboratedTypeKeyword.class, lines));

        return data;
    }

    public static DataStore parseElaboratedTypeData(LineStream lines, ClangParserData parserData) {

        DataStore data = parseTypeWithKeywordData(lines, parserData);

        data.add(ElaboratedType.QUALIFIER, lines.nextLine());

        return data;
    }

    public static DataStore parseTemplateTypeParmTypeData(LineStream lines, ClangParserData parserData) {

        DataStore data = parseTypeData(lines, parserData);

        data.add(TemplateTypeParmType.DEPTH, LineStreamParsers.integer(lines));
        data.add(TemplateTypeParmType.INDEX, LineStreamParsers.integer(lines));
        data.add(TemplateTypeParmType.IS_PACKED, LineStreamParsers.oneOrZero(lines));
        parserData.getClavaNodes().queueSetNode(data, TemplateTypeParmType.DECL, lines.nextLine());

        return data;
    }

    public static DataStore parseTemplateSpecializationTypeData(LineStream lines, ClangParserData parserData) {

        DataStore data = parseTypeData(lines, parserData);

        data.add(TemplateSpecializationType.IS_TYPE_ALIAS, LineStreamParsers.oneOrZero(lines));
        data.add(TemplateSpecializationType.TEMPLATE_NAME, lines.nextLine());
        data.add(TemplateSpecializationType.TEMPLATE_ARGUMENTS, ClavaDataParsers.templateArguments(lines, parserData));

        return data;
    }

    public static DataStore parseTypedefTypeData(LineStream lines, ClangParserData parserData) {

        DataStore data = parseTypeData(lines, parserData);

        parserData.getClavaNodes().queueSetNode(data, TypedefType.DECL, lines.nextLine());

        return data;
    }

}
