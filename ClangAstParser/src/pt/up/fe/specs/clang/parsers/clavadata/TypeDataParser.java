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

package pt.up.fe.specs.clang.parsers.clavadata;

import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.parsers.ClavaDataParser;
import pt.up.fe.specs.clang.parsers.GeneralParsers;
import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.expr.enums.BuiltinKind;
import pt.up.fe.specs.clava.ast.type.data2.BuiltinTypeData;
import pt.up.fe.specs.clava.ast.type.data2.QualTypeDataV2;
import pt.up.fe.specs.clava.ast.type.data2.TypeDataV2;
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

    public static TypeDataV2 parseTypeData(LineStream lines, DataStore dataStore) {

        // Types do not have location
        ClavaData clavaData = ClavaDataParser.parseClavaData(lines, false, dataStore);

        return new TypeDataV2(clavaData);
    }

    public static BuiltinTypeData parseBuiltinTypeData(LineStream lines, DataStore dataStore) {

        TypeDataV2 data = parseTypeData(lines, dataStore);

        BuiltinKind kind = GeneralParsers.enumFromValue(BuiltinKind.getHelper(), lines);
        boolean isSugared = GeneralParsers.parseOneOrZero(lines);
        // Standard standard = config.get(ClavaOptions.STANDARD);

        return new BuiltinTypeData(kind, isSugared, data);
    }

    public static QualTypeDataV2 parseQualTypeData(LineStream lines, DataStore dataStore) {

        TypeDataV2 data = parseTypeData(lines, dataStore);

        // Standard standard = config.get(ClavaOptions.STANDARD);

        List<C99Qualifier> c99Qualifiers = GeneralParsers.enumListFromName(C99Qualifier.getHelper(), lines);
        AddressSpaceQualifierV2 addressSpaceQualifier = GeneralParsers.enumFromName(AddressSpaceQualifierV2.getHelper(),
                lines);
        long addressSpace = GeneralParsers.parseLong(lines);

        return new QualTypeDataV2(c99Qualifiers, addressSpaceQualifier, addressSpace, data);
        // BuiltinKind kind = GeneralParsers.enumFromValue(BuiltinKind.getEnumHelper(), lines);
        // boolean isSugared = GeneralParsers.parseOneOrZero(lines);

        // return new BuiltinTypeData(kind, isSugared, standard, data);
    }

}
