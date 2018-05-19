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

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clang.parsers.GeneralParsers;
import pt.up.fe.specs.clang.parsers.NodeDataParser;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.QualType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.AddressSpaceQualifierV2;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.ast.type.enums.C99Qualifier;
import pt.up.fe.specs.clava.ast.type.enums.CallingConvention;
import pt.up.fe.specs.clava.ast.type.enums.ExceptionSpecificationType;
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

    public static DataStore parseTypeData(LineStream lines, DataStore dataStore) {

        // Types do not have location
        DataStore clavaData = NodeDataParser.parseNodeData(lines, false, dataStore);

        clavaData.add(Type.TYPE_AS_STRING, lines.nextLine());
        clavaData.add(Type.HAS_SUGAR, GeneralParsers.parseOneOrZero(lines));
        clavaData.add(Type.TYPE_DEPENDENCY, GeneralParsers.enumFromName(TypeDependency.class, lines));
        clavaData.add(Type.IS_VARIABLY_MODIFIED, GeneralParsers.parseOneOrZero(lines));
        clavaData.add(Type.CONTAINS_UNEXPANDED_PARAMETER_PACK, GeneralParsers.parseOneOrZero(lines));
        clavaData.add(Type.IS_FROM_AST, GeneralParsers.parseOneOrZero(lines));

        return clavaData;
    }

    public static DataStore parseBuiltinTypeData(LineStream lines, DataStore dataStore) {

        DataStore data = parseTypeData(lines, dataStore);

        // data.add(BuiltinType.KIND_ORDINAL, GeneralParsers.parseInt(lines));
        // data.add(BuiltinType.KIND, GeneralParsers.enumFromValue(BuiltinKind.getHelper(), lines));
        data.add(BuiltinType.KIND, GeneralParsers.enumFromName(BuiltinKind.class, lines));
        data.add(BuiltinType.KIND_LITERAL, lines.nextLine());

        return data;
    }

    public static DataStore parseQualTypeData(LineStream lines, DataStore dataStore) {

        DataStore data = parseTypeData(lines, dataStore);

        data.add(QualType.C99_QUALIFIERS, GeneralParsers.enumListFromName(C99Qualifier.getHelper(), lines));
        data.add(QualType.ADDRESS_SPACE_QUALIFIER, GeneralParsers.enumFromName(AddressSpaceQualifierV2.getHelper(),
                lines));
        data.add(QualType.ADDRESS_SPACE, GeneralParsers.parseLong(lines));

        return data;

    }

    public static DataStore parseFunctionTypeData(LineStream lines, DataStore parserData) {

        DataStore data = parseTypeData(lines, parserData);

        data.add(FunctionType.NO_RETURN, GeneralParsers.parseOneOrZero(lines));
        data.add(FunctionType.PRODUCES_RESULT, GeneralParsers.parseOneOrZero(lines));
        data.add(FunctionType.HAS_REG_PARM, GeneralParsers.parseOneOrZero(lines));
        data.add(FunctionType.REG_PARM, GeneralParsers.parseLong(lines));
        data.add(FunctionType.CALLING_CONVENTION, GeneralParsers.enumFromName(CallingConvention.getHelper(), lines));

        return data;
    }

    public static DataStore parseFunctionProtoTypeData(LineStream lines, DataStore parserData) {

        DataStore data = parseFunctionTypeData(lines, parserData);

        data.add(FunctionProtoType.NUM_PARAMETERS, GeneralParsers.parseInt(lines));

        data.add(FunctionProtoType.HAS_TRAILING_RETURNS, GeneralParsers.parseOneOrZero(lines));
        data.add(FunctionProtoType.IS_VARIADIC, GeneralParsers.parseOneOrZero(lines));
        data.add(FunctionProtoType.IS_CONST, GeneralParsers.parseOneOrZero(lines));
        data.add(FunctionProtoType.IS_VOLATILE, GeneralParsers.parseOneOrZero(lines));
        data.add(FunctionProtoType.IS_RESTRICT, GeneralParsers.parseOneOrZero(lines));

        data.add(FunctionProtoType.REFERENCE_QUALIFIER, GeneralParsers.enumFromName(ReferenceQualifier.class, lines));

        data.add(FunctionProtoType.EXCEPTION_SPECIFICATION_TYPE,
                GeneralParsers.enumFromName(ExceptionSpecificationType.class, lines));

        data.add(FunctionProtoType.NOEXCEPT_EXPR, ClavaNodes.getExpr(parserData, lines.nextLine()));
        /*
        
        
        
        public final static DataKey<Expr> NOEXCEPT_EXPR = KeyFactory.object("noexceptExpr", Expr.class);
        */

        return data;
    }

}
