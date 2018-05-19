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

package pt.up.fe.specs.clava.context;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.LiteralNode;
import pt.up.fe.specs.clava.ast.attr.DummyAttr;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.DummyDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.DummyExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.Literal;
import pt.up.fe.specs.clava.ast.expr.LiteralExpr;
import pt.up.fe.specs.clava.ast.expr.NullExpr;
import pt.up.fe.specs.clava.ast.expr.legacy.FloatingLiteralLegacy.FloatKind;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.DummyType;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.LiteralType;
import pt.up.fe.specs.clava.ast.type.NullType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.util.SpecsCollections;

public class ClavaFactory {

    private static final String TYPE_ID_PREFIX = "type_";
    private static final String EXPR_ID_PREFIX = "expr_";
    private static final String DECL_ID_PREFIX = "decl_";
    private static final String EXTRA_ID_PREFIX = "extra_";
    private static final String STMT_ID_PREFIX = "stmt_";
    private static final String ATTR_ID_PREFIX = "attr_";

    private final ClavaContext context;
    private final DataStore baseData;

    public ClavaFactory(ClavaContext context) {
        this(context, null);
    }

    public ClavaFactory(ClavaContext context, DataStore baseData) {
        this.context = context;
        this.baseData = baseData;
    }

    public DataStore newDataStore(String idPrefix) {
        DataStore data = DataStore.newInstance("ClavaFactory Node");

        // Add base node, if present
        if (baseData != null) {
            data.addAll(baseData);
        }

        // Set context
        data.set(ClavaNode.CONTEXT, context);
        // Set id
        data.set(ClavaNode.ID, context.get(ClavaContext.ID_GENERATOR).next(idPrefix));

        return data;
    }

    private DataStore newTypeDataStore() {
        return newDataStore(TYPE_ID_PREFIX);
    }

    private DataStore newExprDataStore() {
        return newDataStore(EXPR_ID_PREFIX);
    }

    private DataStore newExtraDataStore() {
        return newDataStore(EXTRA_ID_PREFIX);
    }

    private DataStore newDeclDataStore() {
        return newDataStore(DECL_ID_PREFIX);
    }

    private DataStore newStmtDataStore() {
        return newDataStore(STMT_ID_PREFIX);
    }

    private DataStore newAttrDataStore() {
        return newDataStore(ATTR_ID_PREFIX);
    }

    /// EXTRA

    public App app(List<TranslationUnit> tUnits) {
        DataStore data = newExtraDataStore();
        return new App(data, tUnits);
    }

    /// TYPES

    public FunctionProtoType functionProtoType(Type returnType, Type... argTypes) {
        return functionProtoType(returnType, Arrays.asList(argTypes));
    }

    public FunctionProtoType functionProtoType(Type returnType, Collection<Type> argTypes) {
        DataStore data = newTypeDataStore().put(FunctionProtoType.NUM_PARAMETERS, argTypes.size());
        return new FunctionProtoType(data, SpecsCollections.concat(returnType, argTypes));
    }

    public NullType nullType() {
        DataStore data = newTypeDataStore();
        return new NullType(data, Collections.emptyList());
    }

    public BuiltinType builtinType(String literalKind) {
        BuiltinKind kind = BuiltinKind.newInstance(literalKind);
        BuiltinType type = builtinType(kind);
        type.setKindLiteral(literalKind);

        return type;
    }

    public BuiltinType builtinType(BuiltinKind kind) {
        DataStore data = newTypeDataStore().put(BuiltinType.KIND, kind);
        return new BuiltinType(data, Collections.emptyList());
    }

    public DummyType dummyType(String dummyContent) {
        DataStore data = newTypeDataStore()
                .put(DummyType.DUMMY_CONTENT, dummyContent);

        return new DummyType(data, Collections.emptyList());
    }

    public LiteralType literalType(String code) {
        DataStore data = newTypeDataStore()
                .put(LiteralNode.LITERAL_CODE, code);

        return new LiteralType(data, Collections.emptyList());
    }

    /// EXPRS

    public DummyExpr dummyExpr(String dummyContent) {
        DataStore data = newExprDataStore()
                .put(DummyExpr.DUMMY_CONTENT, dummyContent);

        return new DummyExpr(data, Collections.emptyList());
    }

    public NullExpr nullExpr() {
        DataStore data = newExprDataStore()
                .put(Expr.TYPE, nullType());

        return new NullExpr(data, Collections.emptyList());
    }

    public IntegerLiteral integerLiteral(int integer) {
        DataStore data = newExprDataStore()
                .put(Literal.SOURCE_LITERAL, Integer.toString(integer))
                .put(IntegerLiteral.VALUE, BigInteger.valueOf(integer))
                .put(Expr.TYPE, builtinType(BuiltinKind.Int));

        return new IntegerLiteral(data, Collections.emptyList());
    }

    public FloatingLiteral floatingLiteral(FloatKind floatKind, double value) {
        DataStore data = newExprDataStore()
                .put(Literal.SOURCE_LITERAL, Double.toString(value))
                .put(FloatingLiteral.VALUE, value)
                .put(Expr.TYPE, builtinType(floatKind.getBuiltinKind()));

        return new FloatingLiteral(data, Collections.emptyList());
    }

    public LiteralExpr literalExpr(String code, Type type) {
        DataStore data = newExprDataStore()
                .put(LiteralNode.LITERAL_CODE, code)
                .put(Expr.TYPE, type);

        return new LiteralExpr(data, Collections.emptyList());
    }

    /// DECLS

    public DeclStmt declStmt(Decl... decls) {
        return declStmt(Arrays.asList(decls));
    }

    public DeclStmt declStmt(List<Decl> decls) {
        DataStore data = newDeclDataStore();
        return new DeclStmt(data, decls);
    }

    public DeclStmt declStmt(RecordDecl recordDecl, List<VarDecl> varDecls) {
        DataStore data = newDeclDataStore();
        return new DeclStmt(data, SpecsCollections.concat(recordDecl, varDecls));
    }

    public DummyDecl dummyDecl(String dummyContent) {
        DataStore data = newDeclDataStore()
                .put(DummyDecl.DUMMY_CONTENT, dummyContent);

        return new DummyDecl(data, Collections.emptyList());
    }

    public DummyDecl dummyDecl(ClavaNode node) {
        return (DummyDecl) dummyDecl(node.getClass().getSimpleName())
                .setLocation(node.getLocation());
    }

    /// STMTS

    public ReturnStmt returnStmt(Expr retValue) {
        return new ReturnStmt(newStmtDataStore(), Arrays.asList(retValue));
    }

    public ReturnStmt returnStmt() {
        return new ReturnStmt(newStmtDataStore(), Collections.emptyList());
    }

    /**
     * Creates an ExprStmt with semicolon.
     * 
     * @param expr
     * @return
     */
    public ExprStmt exprStmt(Expr expr) {
        DataStore exprStmtData = newStmtDataStore()
                .put(ExprStmt.HAS_SEMICOLON, true)
                .put(ClavaNode.LOCATION, expr.getLocation());

        return new ExprStmt(exprStmtData, Arrays.asList(expr));
    }

    /// ATTRIBUTES

    public DummyAttr dummyAttr(String dummyContent) {
        DataStore data = newAttrDataStore()
                .put(DummyDecl.DUMMY_CONTENT, dummyContent);

        return new DummyAttr(data, Collections.emptyList());
    }

}
