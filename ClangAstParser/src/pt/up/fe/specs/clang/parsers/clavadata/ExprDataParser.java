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

import java.math.BigInteger;

import pt.up.fe.specs.clang.parsers.ClavaDataParser;
import pt.up.fe.specs.clang.parsers.GeneralParsers;
import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.expr.data2.CastExprData;
import pt.up.fe.specs.clava.ast.expr.data2.CharacterLiteralData;
import pt.up.fe.specs.clava.ast.expr.data2.ExprDataV2;
import pt.up.fe.specs.clava.ast.expr.data2.IntegerLiteralData;
import pt.up.fe.specs.clava.ast.expr.data2.LiteralData;
import pt.up.fe.specs.clava.ast.expr.enums.CharacterKind;
import pt.up.fe.specs.clava.ast.expr.enums.ObjectKind;
import pt.up.fe.specs.clava.ast.expr.enums.ValueKind;
import pt.up.fe.specs.clava.language.CastKind;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * ClavaData parsers for Expr nodes.
 * 
 * 
 * @author JoaoBispo
 *
 */
public class ExprDataParser {

    public static ExprDataV2 parseExprData(LineStream lines) {

        ClavaData clavaData = ClavaDataParser.parseClavaData(lines);

        String typeId = lines.nextLine();
        ValueKind valueKind = GeneralParsers.enumFromInt(ValueKind.getEnumHelper(), lines);
        ObjectKind objectKind = GeneralParsers.enumFromInt(ObjectKind.getEnumHelper(), lines);

        return new ExprDataV2(typeId, valueKind, objectKind, clavaData);
    }

    public static CastExprData parseCastExprData(LineStream lines) {
        ExprDataV2 data = parseExprData(lines);

        CastKind castKind = GeneralParsers.enumFromName(CastKind.getHelper(), lines);

        return new CastExprData(castKind, data);
    }

    public static LiteralData parseLiteralData(LineStream lines) {
        ExprDataV2 data = parseExprData(lines);

        String sourceLiteral = lines.nextLine();

        return new LiteralData(sourceLiteral, data);
    }

    public static CharacterLiteralData parseCharacterLiteralData(LineStream lines) {
        LiteralData data = parseLiteralData(lines);

        long value = GeneralParsers.parseLong(lines);
        CharacterKind kind = GeneralParsers.enumFromInt(CharacterKind.getEnumHelper(), lines);

        return new CharacterLiteralData(value, kind, data);
    }

    public static IntegerLiteralData parseIntegerLiteralData(LineStream lines) {
        LiteralData data = parseLiteralData(lines);

        BigInteger value = new BigInteger(lines.nextLine());

        return new IntegerLiteralData(value, data);
    }

}
