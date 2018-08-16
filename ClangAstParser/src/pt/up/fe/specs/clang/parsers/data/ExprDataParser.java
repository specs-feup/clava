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
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CXXBoolLiteralExpr;
import pt.up.fe.specs.clava.ast.expr.CXXConstructExpr;
import pt.up.fe.specs.clava.ast.expr.CXXMemberCallExpr;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.CastExpr;
import pt.up.fe.specs.clava.ast.expr.CharacterLiteral;
import pt.up.fe.specs.clava.ast.expr.CompoundLiteralExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.InitListExpr;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.Literal;
import pt.up.fe.specs.clava.ast.expr.MaterializeTemporaryExpr;
import pt.up.fe.specs.clava.ast.expr.MemberExpr;
import pt.up.fe.specs.clava.ast.expr.OverloadExpr;
import pt.up.fe.specs.clava.ast.expr.UnresolvedLookupExpr;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.CharacterKind;
import pt.up.fe.specs.clava.ast.expr.enums.ConstructionKind;
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

    public static DataStore parseExprData(LineStream lines, ClangParserData dataStore) {

        DataStore data = NodeDataParser.parseNodeData(lines, dataStore);
        // TODO: ClavaNodes.getType, should be in ClavaContext?
        // data.add(Expr.TYPE, dataStore.getClavaNodes().getType(lines.nextLine()));
        // dataStore.getClavaNodes().queueSetNode(data, Expr.TYPE, lines.nextLine());
        dataStore.getClavaNodes().queueSetOptionalNode(data, Expr.TYPE, lines.nextLine());

        data.add(Expr.VALUE_KIND, LineStreamParsers.enumFromInt(ValueKind.getEnumHelper(), lines));
        data.add(Expr.OBJECT_KIND, LineStreamParsers.enumFromInt(ObjectKind.getEnumHelper(), lines));

        return data;
    }

    public static DataStore parseCastExprData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseExprData(lines, dataStore);

        data.add(CastExpr.CAST_KIND, LineStreamParsers.enumFromName(CastKind.getHelper(), lines));

        return data;
    }

    public static DataStore parseLiteralData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseExprData(lines, dataStore);

        data.add(Literal.SOURCE_LITERAL, ClavaDataParsers.literalSource(lines));

        return data;
    }

    public static DataStore parseCharacterLiteralData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseLiteralData(lines, dataStore);

        data.add(CharacterLiteral.VALUE, LineStreamParsers.longInt(lines));
        data.add(CharacterLiteral.KIND, LineStreamParsers.enumFromInt(CharacterKind.getEnumHelper(), lines));

        return data;
    }

    public static DataStore parseIntegerLiteralData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseLiteralData(lines, dataStore);

        data.add(IntegerLiteral.VALUE, new BigInteger(lines.nextLine()));

        return data;
    }

    public static DataStore parseFloatingLiteralData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseLiteralData(lines, dataStore);

        data.add(FloatingLiteral.VALUE, Double.parseDouble(lines.nextLine()));

        return data;
    }

    public static DataStore parseStringLiteralData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseLiteralData(lines, dataStore);

        // data.add(StringLiteral.STRING, ClavaDataParsers.literalSource(lines));

        return data;
    }

    public static DataStore parseCXXBoolLiteralExprData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseLiteralData(lines, dataStore);

        data.add(CXXBoolLiteralExpr.VALUE, LineStreamParsers.oneOrZero(lines.nextLine()));

        return data;
    }

    public static DataStore parseCompoundLiteralExprData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseLiteralData(lines, dataStore);

        data.add(CompoundLiteralExpr.IS_FILE_SCOPE, LineStreamParsers.oneOrZero(lines.nextLine()));

        return data;
    }

    public static DataStore parseInitListExprData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseExprData(lines, dataStore);

        // data.add(InitListExpr.ARRAY_FILLER, dataStore.getClavaNodes().getExpr(lines.nextLine()));
        dataStore.getClavaNodes().queueSetOptionalNode(data, InitListExpr.ARRAY_FILLER, lines.nextLine());
        // data.add(InitListExpr.INITIALIZED_FIELD_IN_UNION, (FieldDecl) ClavaNodes.getDecl(dataStore,
        // lines.nextLine()));
        data.add(InitListExpr.IS_EXPLICIT, LineStreamParsers.oneOrZero(lines));
        data.add(InitListExpr.IS_STRING_LITERAL_INIT, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseDeclRefExprData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseExprData(lines, dataStore);

        data.add(DeclRefExpr.QUALIFIER, lines.nextLine());
        data.add(DeclRefExpr.TEMPLATE_ARGUMENTS, LineStreamParsers.stringList(lines,
                ClavaDataParsers::literalSource));
        // data.add(DeclRefExpr.DECL_NAME, lines.nextLine());
        // data.add(DeclRefExpr.DECL_ID, lines.nextLine());
        // data.add(DeclRefExpr.DECL, ClavaNodes.getValueDecl(dataStore, data.get(DeclRefExpr.DECL_ID)));
        dataStore.getClavaNodes().queueSetNode(data, DeclRefExpr.DECL, lines.nextLine());

        return data;
    }

    public static DataStore parseOverloadExprData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseExprData(lines, dataStore);

        data.add(OverloadExpr.QUALIFIER, lines.nextLine());

        return data;
    }

    public static DataStore parseCXXConstructExprData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseExprData(lines, dataStore);

        data.add(CXXConstructExpr.IS_ELIDABLE, LineStreamParsers.oneOrZero(lines));
        data.add(CXXConstructExpr.IS_DEFAULT_ARGUMENT, LineStreamParsers.oneOrZero(lines));
        data.add(CXXConstructExpr.REQUIRES_ZERO_INITIALIZATION, LineStreamParsers.oneOrZero(lines));
        data.add(CXXConstructExpr.IS_LIST_INITIALIZATION, LineStreamParsers.oneOrZero(lines));
        data.add(CXXConstructExpr.IS_STD_LIST_INITIALIZATION, LineStreamParsers.oneOrZero(lines));
        data.add(CXXConstructExpr.CONSTRUCTION_KIND, LineStreamParsers.enumFromName(ConstructionKind.class, lines));

        return data;
    }

    public static DataStore parseMemberExprData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseExprData(lines, dataStore);

        data.add(MemberExpr.IS_ARROW, LineStreamParsers.oneOrZero(lines));
        data.add(MemberExpr.MEMBER_NAME, lines.nextLine());

        return data;
    }

    public static DataStore parseMaterializeTemporaryExprData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseExprData(lines, dataStore);

        dataStore.getClavaNodes().queueSetOptionalNode(data, MaterializeTemporaryExpr.EXTENDING_DECL, lines.nextLine());

        return data;
    }

    public static DataStore parseBinaryOperatorData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseExprData(lines, dataStore);

        data.add(BinaryOperator.OP, LineStreamParsers.enumFromName(BinaryOperatorKind.class, lines));

        return data;
    }

    public static DataStore parseUnresolvedLookupExprData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseOverloadExprData(lines, dataStore);

        data.add(UnresolvedLookupExpr.REQUIRES_ADL, LineStreamParsers.oneOrZero(lines));
        data.add(UnresolvedLookupExpr.NAME, lines.nextLine());
        dataStore.getClavaNodes().queueSetNodeList(data, UnresolvedLookupExpr.UNRESOLVED_DECLS,
                LineStreamParsers.stringList(lines));

        return data;
    }

    public static DataStore parseCallExprData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseExprData(lines, dataStore);

        dataStore.getClavaNodes().queueSetOptionalNode(data, CallExpr.DIRECT_CALLEE, lines.nextLine());

        return data;
    }

    public static DataStore parseCXXMemberCallExprData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseCallExprData(lines, dataStore);

        dataStore.getClavaNodes().queueSetNode(data, CXXMemberCallExpr.METHOD_DECL, lines.nextLine());

        return data;
    }
}
