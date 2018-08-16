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

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.LiteralNode;
import pt.up.fe.specs.clava.ast.attr.DummyAttr;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.DummyDecl;
import pt.up.fe.specs.clava.ast.decl.DummyNamedDecl;
import pt.up.fe.specs.clava.ast.decl.DummyValueDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.NullDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CStyleCastExpr;
import pt.up.fe.specs.clava.ast.expr.CXXFunctionalCastExpr;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.CastExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.DummyExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.Literal;
import pt.up.fe.specs.clava.ast.expr.LiteralExpr;
import pt.up.fe.specs.clava.ast.expr.NullExpr;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.legacy.FloatingLiteralLegacy.FloatKind;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.BreakStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.LiteralStmt;
import pt.up.fe.specs.clava.ast.stmt.NullStmt;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WhileStmt;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.ConstantArrayType;
import pt.up.fe.specs.clava.ast.type.DummyType;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.LiteralType;
import pt.up.fe.specs.clava.ast.type.NullType;
import pt.up.fe.specs.clava.ast.type.PointerType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.VariableArrayType;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.language.CastKind;
import pt.up.fe.specs.util.SpecsCollections;

/**
 * Factory methods for ClavaNodes that use the DataStore format.
 * 
 * <p>
 * This class provides minimal methods for building new nodes, further specialization should be done using the .set()
 * method of the respective node.
 * 
 * <p>
 * IMPORTANT: Type nodes (e.g., BuiltinType) are immutable, the .set() method returns a copy of the node with changes in
 * the respective parameter. Types nodes are immutable since they can be shared among several nodes. If type nodes were
 * mutable, the .set() method could provoke unintended changes.
 * 
 * @author JoaoBispo
 *
 */
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
        return newDataStore(DECL_ID_PREFIX)
                .set(Decl.ATTRIBUTES, new ArrayList<>());
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

    public TranslationUnit translationUnit(File sourceFile, Collection<Decl> declarations) {
        DataStore data = newExtraDataStore();
        TranslationUnit.setDataStore(sourceFile, data);

        return new TranslationUnit(data, declarations);
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

    public ConstantArrayType constantArrayType(Type elementType, int size) {
        return constantArrayType(elementType, BigInteger.valueOf(size));
    }

    public ConstantArrayType constantArrayType(Type elementType, BigInteger size) {
        DataStore data = newTypeDataStore()
                .put(ConstantArrayType.ARRAY_SIZE, size);
        // .put(ArrayType.INDEX_TYPE_QUALIFIERS, new ArrayList<>());

        return new ConstantArrayType(data, Arrays.asList(elementType));
    }

    public VariableArrayType variableArrayType(Type elementType, Expr sizeExpr) {
        DataStore data = newTypeDataStore();
        // .put(ArrayType.INDEX_TYPE_QUALIFIERS, new ArrayList<>());

        return new VariableArrayType(data, Arrays.asList(elementType, sizeExpr));
    }

    public PointerType pointerType(Type pointeeType) {
        DataStore data = newTypeDataStore();
        // .put(ArrayType.INDEX_TYPE_QUALIFIERS, new ArrayList<>());

        return new PointerType(data, Arrays.asList(pointeeType));
    }

    /// EXPRS

    public DummyExpr dummyExpr(String dummyContent) {
        DataStore data = newExprDataStore()
                .put(DummyExpr.DUMMY_CONTENT, dummyContent)
                .put(Expr.TYPE, Optional.of(dummyType("dummy type")));

        return new DummyExpr(data, Collections.emptyList());
    }

    public NullExpr nullExpr() {
        DataStore data = newExprDataStore()
                .put(Expr.TYPE, Optional.of(nullType()));

        return new NullExpr(data, Collections.emptyList());
    }

    public IntegerLiteral integerLiteral(int integer) {
        DataStore data = newExprDataStore()
                .put(Literal.SOURCE_LITERAL, Integer.toString(integer))
                .put(IntegerLiteral.VALUE, BigInteger.valueOf(integer))
                .put(Expr.TYPE, Optional.of(builtinType(BuiltinKind.Int)));

        return new IntegerLiteral(data, Collections.emptyList());
    }

    public FloatingLiteral floatingLiteral(FloatKind floatKind, double value) {
        DataStore data = newExprDataStore()
                .put(Literal.SOURCE_LITERAL, Double.toString(value))
                .put(FloatingLiteral.VALUE, value)
                .put(Expr.TYPE, Optional.of(builtinType(floatKind.getBuiltinKind())));

        return new FloatingLiteral(data, Collections.emptyList());
    }

    public LiteralExpr literalExpr(String code, Type type) {
        DataStore data = newExprDataStore()
                .put(LiteralNode.LITERAL_CODE, code)
                .put(Expr.TYPE, Optional.of(type));

        return new LiteralExpr(data, Collections.emptyList());
    }

    public DeclRefExpr declRefExpr(String declName, Type type) {
        DataStore data = newExprDataStore()
                .put(Expr.TYPE, Optional.of(type));
        // .put(DeclRefExpr.DECL_NAME, declName);

        DeclRefExpr declRefExpr = new DeclRefExpr(data, Collections.emptyList());
        declRefExpr.setName(declName);

        return declRefExpr;
        // return new DeclRefExpr(data, Arrays.asList(dummyNamedDecl(declName)));
    }
    // public static DeclRefExpr declRefExpr(String refName, ValueKind valueKind, Type type, ClavaNodeInfo info) {
    // ExprData exprData = new ExprData(type, valueKind);
    //
    // String qualifier = "";
    //
    // BareDeclData declData = BareDeclData.newInstance(refName);
    //
    // return declRefExpr(qualifier, Collections.emptyList(), declData, null, exprData, info);
    // }

    public CallExpr callExpr(Expr function, Type type, List<? extends Expr> args) {
        DataStore data = newExprDataStore()
                .put(Expr.TYPE, Optional.of(type));

        return new CallExpr(data, SpecsCollections.concat(function, args));
    }

    public CXXFunctionalCastExpr cxxFunctionalCastExpr(CastExpr expr, Expr subExpr) {
        DataStore data = expr.getFactoryWithNode().newExprDataStore();

        return new CXXFunctionalCastExpr(data, Arrays.asList(subExpr));
    }

    public BinaryOperator binaryOperator(BinaryOperatorKind op, Type type, Expr lhs, Expr rhs) {
        DataStore data = newExprDataStore()
                .put(Expr.TYPE, Optional.of(type));
        data.set(BinaryOperator.OP, op);

        return new BinaryOperator(data, Arrays.asList(lhs, rhs));
    }

    public CStyleCastExpr cStyleCastExpr(Type type, Expr expr) {
        DataStore data = newExprDataStore()
                .put(Expr.TYPE, Optional.of(type));

        data.set(CastExpr.CAST_KIND, CastKind.NO_OP);

        return new CStyleCastExpr(data, Arrays.asList(expr));
    }

    /// DECLS

    public NullDecl nullDecl() {
        return new NullDecl(newDeclDataStore(), Collections.emptyList());
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

    public DummyNamedDecl dummyNamedDecl(String declName) {
        DataStore data = newDeclDataStore()
                .put(DummyDecl.DUMMY_CONTENT, declName)
                .put(NamedDecl.DECL_NAME, declName);

        return new DummyNamedDecl(data, Collections.emptyList());
    }

    public DummyValueDecl dummyValueDecl(String declName, Type type) {
        DataStore data = newDeclDataStore()
                .put(DummyDecl.DUMMY_CONTENT, declName)
                .put(NamedDecl.DECL_NAME, declName)
                .put(ValueDecl.TYPE, type);

        return new DummyValueDecl(data, Collections.emptyList());
        // return (DummyDecl) dummyDecl(node.getClass().getSimpleName())
        // .setLocation(node.getLocation());
    }

    public FunctionDecl functionDecl(String declName, Type type) {
        DataStore data = newDeclDataStore()
                .put(NamedDecl.DECL_NAME, declName)
                .put(ValueDecl.TYPE, type);

        return new FunctionDecl(data, Collections.emptyList());
        // return (DummyDecl) dummyDecl(node.getClass().getSimpleName())
        // .setLocation(node.getLocation());
    }

    public VarDecl varDecl(String declName, Type type) {
        DataStore data = newDeclDataStore()
                .put(NamedDecl.DECL_NAME, declName)
                .put(ValueDecl.TYPE, type);

        return new VarDecl(data, Collections.emptyList());
    }

    public VarDecl varDecl(String declName, Expr initExpr) {
        VarDecl varDecl = varDecl(declName, initExpr.getType());
        varDecl.setInit(initExpr);
        return varDecl;
    }

    public ParmVarDecl parmVarDecl(String declName, Type type) {
        DataStore data = newDeclDataStore()
                .put(NamedDecl.DECL_NAME, declName)
                .put(ValueDecl.TYPE, type);

        return new ParmVarDecl(data, Collections.emptyList());
    }

    /// STMTS

    public NullStmt nullStmt() {
        return new NullStmt(newStmtDataStore(), Collections.emptyList());
    }

    public DeclStmt declStmt(Decl... decls) {
        return declStmt(Arrays.asList(decls));
    }

    public DeclStmt declStmt(List<Decl> decls) {
        DataStore data = newStmtDataStore();
        return new DeclStmt(data, decls);
    }

    public DeclStmt declStmt(RecordDecl recordDecl, List<VarDecl> varDecls) {
        DataStore data = newStmtDataStore();
        return new DeclStmt(data, SpecsCollections.concat(recordDecl, varDecls));
    }

    public ReturnStmt returnStmt(Expr retValue) {
        return new ReturnStmt(newStmtDataStore(), Arrays.asList(retValue));
    }

    public ReturnStmt returnStmt() {
        return new ReturnStmt(newStmtDataStore(), Collections.emptyList());
    }

    public LiteralStmt literalStmt(String literalCode) {
        return new LiteralStmt(newStmtDataStore().put(LiteralStmt.LITERAL_CODE, literalCode), Collections.emptyList());
    }

    public CompoundStmt compoundStmt(String statement) {
        return compoundStmt(literalStmt(statement));
    }

    public CompoundStmt compoundStmt(Stmt... children) {
        return compoundStmt(Arrays.asList(children));
    }

    public CompoundStmt compoundStmt(Collection<Stmt> children) {
        return new CompoundStmt(newStmtDataStore(), children);
    }

    // public CompoundStmt compoundStmt(boolean isNaked, Collection<? extends ClavaNode> children) {
    // return new CompoundStmt(newStmtDataStore().put(CompoundStmt.IS_NAKED, isNaked), children);
    // }

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

    public IfStmt ifStmt(Expr condition, CompoundStmt thenBody) {
        return ifStmt(condition, thenBody, nullStmt());
    }

    public IfStmt ifStmt(Expr condition, CompoundStmt thenBody, CompoundStmt elseBody) {
        return ifStmt(condition, thenBody, (ClavaNode) elseBody);
    }

    private IfStmt ifStmt(Expr condition, CompoundStmt thenBody, ClavaNode elseBody) {
        DataStore ifStmtData = newStmtDataStore();

        return new IfStmt(ifStmtData, Arrays.asList(nullDecl(), condition, thenBody, elseBody));
    }

    public ForStmt forStmt(Stmt init, Stmt cond, Stmt inc, CompoundStmt body) {
        DataStore forStmtData = newStmtDataStore();

        return new ForStmt(forStmtData, Arrays.asList(init, cond, inc, body));
    }

    public WhileStmt whileStmt(Stmt cond, CompoundStmt body) {
        DataStore whileStmtData = newStmtDataStore();

        return new WhileStmt(whileStmtData, Arrays.asList(cond, body));
    }

    public BreakStmt breakStmt() {
        DataStore breakStmtData = newStmtDataStore();

        return new BreakStmt(breakStmtData, Collections.emptyList());
    }

    /// ATTRIBUTES

    public DummyAttr dummyAttr(String dummyContent) {
        DataStore data = newAttrDataStore()
                .put(DummyDecl.DUMMY_CONTENT, dummyContent);

        return new DummyAttr(data, Collections.emptyList());
    }

}
