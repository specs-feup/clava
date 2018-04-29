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

import pt.up.fe.specs.clang.parsers.GeneralParsers;
import pt.up.fe.specs.clang.parsers.NodeDataParser;
import pt.up.fe.specs.clava.ast.expr.enums.BuiltinKind;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.QualType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.AddressSpaceQualifierV2;
import pt.up.fe.specs.clava.ast.type.enums.C99Qualifier;
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

        return clavaData;
    }

    public static DataStore parseBuiltinTypeData(LineStream lines, DataStore dataStore) {

        DataStore data = parseTypeData(lines, dataStore);

        data.add(BuiltinType.KIND_ORDINAL, GeneralParsers.parseInt(lines));
        data.add(BuiltinType.KIND, GeneralParsers.enumFromValue(BuiltinKind.getHelper(), lines));

        return data;
    }

    public static DataStore parseQualTypeData(LineStream lines, DataStore dataStore) {

        DataStore data = parseTypeData(lines, dataStore);

        data.add(QualType.C99_QUALIFIERS, GeneralParsers.enumListFromName(C99Qualifier.getHelper(), lines));
        data.add(QualType.ADDRESS_SPACE_QUALIFIER, GeneralParsers.enumFromName(AddressSpaceQualifierV2.getHelper(),
                lines));

        return data;

    }

}
