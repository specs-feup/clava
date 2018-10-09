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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.LiteralNode;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.attr.DummyAttr;
import pt.up.fe.specs.clava.ast.comment.Comment;
import pt.up.fe.specs.clava.ast.comment.InlineComment;
import pt.up.fe.specs.clava.ast.comment.MultiLineComment;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.DummyDecl;
import pt.up.fe.specs.clava.ast.decl.DummyNamedDecl;
import pt.up.fe.specs.clava.ast.decl.DummyValueDecl;
import pt.up.fe.specs.clava.ast.decl.FieldDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.ast.decl.LinkageSpecDecl;
import pt.up.fe.specs.clava.ast.decl.LiteralDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.NullDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.TagDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.enums.LanguageId;
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
import pt.up.fe.specs.clava.ast.expr.MemberExpr;
import pt.up.fe.specs.clava.ast.expr.NullExpr;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.FloatKind;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorPosition;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.omp.OmpClausePragma;
import pt.up.fe.specs.clava.ast.omp.OmpDirectiveKind;
import pt.up.fe.specs.clava.ast.omp.OmpLiteralPragma;
import pt.up.fe.specs.clava.ast.omp.OmpPragma;
import pt.up.fe.specs.clava.ast.omp.SimpleOmpPragma;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind;
import pt.up.fe.specs.clava.ast.pragma.GenericPragma;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.ast.stmt.BreakStmt;
import pt.up.fe.specs.clava.ast.stmt.CaseStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.DummyStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.LabelStmt;
import pt.up.fe.specs.clava.ast.stmt.LiteralStmt;
import pt.up.fe.specs.clava.ast.stmt.NullStmt;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.SwitchStmt;
import pt.up.fe.specs.clava.ast.stmt.WhileStmt;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.ConstantArrayType;
import pt.up.fe.specs.clava.ast.type.DummyType;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.LiteralType;
import pt.up.fe.specs.clava.ast.type.NullType;
import pt.up.fe.specs.clava.ast.type.PointerType;
import pt.up.fe.specs.clava.ast.type.RecordType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.VariableArrayType;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.language.CastKind;
import pt.up.fe.specs.clava.language.TagKind;
import pt.up.fe.specs.clava.utils.ClassesService;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.classmap.ClassMap;

/**
 * Factory methods for ClavaNodes that use the DataStore format.
 * 
 * <p>
 * This class provides minimal methods for building new nodes, further specialization should be done using the .set()
 * method of the respective node.
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
    private static final String PRAGMA_ID_PREFIX = "pragma_";
    private static final String COMMENT_ID_PREFIX = "comment_";

    private static final ClassMap<ClavaNode, String> PREFIX_MAP;
    static {
        PREFIX_MAP = new ClassMap<>(EXTRA_ID_PREFIX);
        PREFIX_MAP.put(Type.class, TYPE_ID_PREFIX);
        PREFIX_MAP.put(Expr.class, EXPR_ID_PREFIX);
        PREFIX_MAP.put(Decl.class, DECL_ID_PREFIX);
        PREFIX_MAP.put(Stmt.class, STMT_ID_PREFIX);
        PREFIX_MAP.put(Attribute.class, ATTR_ID_PREFIX);
        PREFIX_MAP.put(Pragma.class, PRAGMA_ID_PREFIX);
        PREFIX_MAP.put(Comment.class, COMMENT_ID_PREFIX);
    }

    private final ClavaContext context;
    private final DataStore baseData;
    private final ClassesService classesService;

    public ClavaFactory(ClavaContext context) {
        this(context, null);
    }

    public ClavaFactory(ClavaContext context, DataStore baseData) {
        this.context = context;
        this.baseData = baseData;
        this.classesService = new ClassesService();
    }

    // public DataStore newDataStore(String idPrefix) {
    public DataStore newDataStore(Class<? extends ClavaNode> nodeClass) {

        // Get the correct prefix for the given class
        String idPrefix = PREFIX_MAP.get(nodeClass);

        // DataStore data = DataStore.newInstance("ClavaFactory Node");
        DataStore data = DataStore.newInstance(StoreDefinitions.fromInterface(nodeClass), true);

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

    // protected DataStore newTypeDataStore() {
    // return newDataStore(TYPE_ID_PREFIX);
    // }
    //
    // protected DataStore newExprDataStore() {
    // return newDataStore(EXPR_ID_PREFIX);
    // }
    //
    // protected DataStore newExtraDataStore() {
    // return newDataStore(EXTRA_ID_PREFIX);
    // }
    //
    // protected DataStore newDeclDataStore() {
    // return newDataStore(DECL_ID_PREFIX)
    // .set(Decl.ATTRIBUTES, new ArrayList<>());
    // }
    //
    // protected DataStore newStmtDataStore() {
    // return newDataStore(STMT_ID_PREFIX);
    // }
    //
    // protected DataStore newAttrDataStore() {
    // return newDataStore(ATTR_ID_PREFIX);
    // }
    //
    // protected DataStore newPragmaDataStore() {
    // return newDataStore(PRAGMA_ID_PREFIX);
    // }
    //
    // protected DataStore newCommentDataStore() {
    // return newDataStore(COMMENT_ID_PREFIX);
    // }

    /// EXTRA

    public App app(List<TranslationUnit> tUnits) {
        DataStore data = newDataStore(App.class);
        return new App(data, tUnits);
    }

    public TranslationUnit translationUnit(File sourceFile, Collection<Decl> declarations) {
        DataStore data = newDataStore(TranslationUnit.class);
        TranslationUnit.setDataStore(sourceFile, data);

        return new TranslationUnit(data, declarations);
    }

    public <T extends ClavaNode> T node(Class<T> nodeClass, ClavaNode... children) {
        return node(nodeClass, Arrays.asList(children));
    }

    public <T extends ClavaNode> T node(Class<T> nodeClass, List<? extends ClavaNode> children) {

        DataStore data = newDataStore(nodeClass);

        return nodeClass.cast(classesService.getClavaNodeBuilder(nodeClass).apply(data, children));
    }

    /// TYPES

    public FunctionProtoType functionProtoType(Type returnType, Type... argTypes) {
        return functionProtoType(returnType, Arrays.asList(argTypes));
    }

    public FunctionProtoType functionProtoType(Type returnType, Collection<Type> argTypes) {
        DataStore data = newDataStore(FunctionProtoType.class)
                .put(FunctionProtoType.NUM_PARAMETERS, argTypes.size())
                .put(FunctionProtoType.RETURN_TYPE, returnType)
                .put(FunctionProtoType.PARAMETERS_TYPES, new ArrayList<>(argTypes));

        return new FunctionProtoType(data, SpecsCollections.concat(returnType, Collections.emptyList()));
        // return new FunctionProtoType(data, SpecsCollections.concat(returnType, argTypes));
    }

    public NullType nullType() {
        DataStore data = newDataStore(NullType.class);
        return new NullType(data, Collections.emptyList());
    }

    public BuiltinType builtinType(String literalKind) {
        BuiltinKind kind = BuiltinKind.newInstance(literalKind);
        BuiltinType type = builtinType(kind);
        type.setKindLiteral(literalKind);

        return type;
    }

    public BuiltinType builtinType(BuiltinKind kind) {
        DataStore data = newDataStore(BuiltinType.class).put(BuiltinType.KIND, kind);
        return new BuiltinType(data, Collections.emptyList());
    }

    public DummyType dummyType(String dummyContent) {
        DataStore data = newDataStore(DummyType.class)
                .put(DummyType.DUMMY_CONTENT, dummyContent);

        return new DummyType(data, Collections.emptyList());
    }

    public LiteralType literalType(String code) {
        DataStore data = newDataStore(LiteralType.class)
                .put(LiteralNode.LITERAL_CODE, code);

        return new LiteralType(data, Collections.emptyList());
    }

    public ConstantArrayType constantArrayType(Type elementType, int size) {
        return constantArrayType(elementType, BigInteger.valueOf(size));
    }

    public ConstantArrayType constantArrayType(Type elementType, BigInteger size) {
        DataStore data = newDataStore(ConstantArrayType.class)
                .put(ConstantArrayType.ARRAY_SIZE, size)
                .put(ConstantArrayType.ELEMENT_TYPE, elementType);
        // .put(ArrayType.INDEX_TYPE_QUALIFIERS, new ArrayList<>());

        // return new ConstantArrayType(data, Arrays.asList(elementType));
        return new ConstantArrayType(data, Collections.emptyList());
    }

    public VariableArrayType variableArrayType(Type elementType, Expr sizeExpr) {
        DataStore data = newDataStore(VariableArrayType.class)
                .put(VariableArrayType.ELEMENT_TYPE, elementType)
                .put(VariableArrayType.SIZE_EXPR, sizeExpr);
        // .put(ArrayType.INDEX_TYPE_QUALIFIERS, new ArrayList<>());

        // return new VariableArrayType(data, Arrays.asList(elementType, sizeExpr));
        return new VariableArrayType(data, Collections.emptyList());
    }

    public PointerType pointerType(Type pointeeType) {
        DataStore data = newDataStore(PointerType.class)
                .put(PointerType.POINTEE_TYPE, pointeeType);
        // .put(ArrayType.INDEX_TYPE_QUALIFIERS, new ArrayList<>());

        // return new PointerType(data, Arrays.asList(pointeeType));
        return new PointerType(data, Collections.emptyList());
    }

    public RecordType recordType(TagDecl recordDecl) {
        DataStore data = newDataStore(RecordType.class)
                .put(RecordType.DECL, recordDecl);

        return new RecordType(data, Collections.emptyList());
    }

    /// EXPRS

    public MemberExpr memberExpr(String memberName, Type memberType, Expr baseExpr) {
        DataStore data = newDataStore(MemberExpr.class)
                .put(MemberExpr.MEMBER_NAME, memberName)
                .put(Expr.TYPE, Optional.of(memberType));

        if (baseExpr.getType() instanceof PointerType) {
            data.put(MemberExpr.IS_ARROW, true);
        }

        return new MemberExpr(data, Arrays.asList(baseExpr));
    }

    public DummyExpr dummyExpr(String dummyContent) {
        DataStore data = newDataStore(DummyExpr.class)
                .put(DummyExpr.DUMMY_CONTENT, dummyContent)
                .put(Expr.TYPE, Optional.of(dummyType("dummy type")));

        return new DummyExpr(data, Collections.emptyList());
    }

    public NullExpr nullExpr() {
        DataStore data = newDataStore(NullExpr.class)
                .put(Expr.TYPE, Optional.of(nullType()));

        return new NullExpr(data, Collections.emptyList());
    }

    public IntegerLiteral integerLiteral(int integer) {
        DataStore data = newDataStore(IntegerLiteral.class)
                .put(Literal.SOURCE_LITERAL, Integer.toString(integer))
                .put(IntegerLiteral.VALUE, BigInteger.valueOf(integer))
                .put(Expr.TYPE, Optional.of(builtinType(BuiltinKind.Int)));

        return new IntegerLiteral(data, Collections.emptyList());
    }

    public FloatingLiteral floatingLiteral(FloatKind floatKind, double value) {
        DataStore data = newDataStore(FloatingLiteral.class)
                .put(Literal.SOURCE_LITERAL, Double.toString(value))
                .put(FloatingLiteral.VALUE, value)
                .put(Expr.TYPE, Optional.of(builtinType(floatKind.getBuiltinKind())));

        return new FloatingLiteral(data, Collections.emptyList());
    }

    public LiteralExpr literalExpr(String code, Type type) {
        DataStore data = newDataStore(LiteralExpr.class)
                .put(LiteralNode.LITERAL_CODE, code)
                .put(Expr.TYPE, Optional.of(type));

        return new LiteralExpr(data, Collections.emptyList());
    }

    public DeclRefExpr declRefExpr(ValueDecl decl) {
        DeclRefExpr declRefExpr = declRefExpr(decl.get(ValueDecl.DECL_NAME), decl.getType());
        declRefExpr.set(DeclRefExpr.DECL, decl);

        return declRefExpr;
    }

    public DeclRefExpr declRefExpr(String declName, Type type) {
        DataStore data = newDataStore(DeclRefExpr.class)
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
        DataStore data = newDataStore(CallExpr.class)
                .put(Expr.TYPE, Optional.of(type));

        return new CallExpr(data, SpecsCollections.concat(function, args));
    }

    public CXXFunctionalCastExpr cxxFunctionalCastExpr(CastExpr expr, Expr subExpr) {
        DataStore data = expr.getFactoryWithNode().newDataStore(CXXFunctionalCastExpr.class);

        return new CXXFunctionalCastExpr(data, Arrays.asList(subExpr));
    }

    public BinaryOperator binaryOperator(BinaryOperatorKind op, Type type, Expr lhs, Expr rhs) {
        DataStore data = newDataStore(BinaryOperator.class)
                .put(Expr.TYPE, Optional.of(type));
        data.set(BinaryOperator.OP, op);

        return new BinaryOperator(data, Arrays.asList(lhs, rhs));
    }

    public UnaryOperator unaryOperator(UnaryOperatorKind op, Type type, Expr subExpr) {
        DataStore data = newDataStore(UnaryOperator.class)
                .put(Expr.TYPE, Optional.of(type))
                .put(UnaryOperator.OP, op);

        if (op.equals(UnaryOperatorKind.POST_INC) || op.equals(UnaryOperatorKind.POST_DEC))
            data.put(UnaryOperator.POSITION, UnaryOperatorPosition.POSTFIX);

        return new UnaryOperator(data, Arrays.asList(subExpr));
    }

    public CStyleCastExpr cStyleCastExpr(Type type, Expr expr) {
        DataStore data = newDataStore(CStyleCastExpr.class)
                .put(Expr.TYPE, Optional.of(type));

        data.set(CastExpr.CAST_KIND, CastKind.NO_OP);

        return new CStyleCastExpr(data, Arrays.asList(expr));
    }

    /// DECLS

    public NullDecl nullDecl() {
        return new NullDecl(newDataStore(NullDecl.class), Collections.emptyList());
    }

    public DummyDecl dummyDecl(String dummyContent) {
        DataStore data = newDataStore(DummyDecl.class)
                .put(DummyDecl.DUMMY_CONTENT, dummyContent);

        return new DummyDecl(data, Collections.emptyList());
    }

    public DummyDecl dummyDecl(ClavaNode node) {
        return (DummyDecl) dummyDecl(node.getClass().getSimpleName())
                .setLocation(node.getLocation());
    }

    public DummyNamedDecl dummyNamedDecl(String declName) {
        DataStore data = newDataStore(DummyNamedDecl.class)
                .put(DummyDecl.DUMMY_CONTENT, declName)
                .put(NamedDecl.DECL_NAME, declName);

        return new DummyNamedDecl(data, Collections.emptyList());
    }

    public DummyValueDecl dummyValueDecl(String declName, Type type) {
        DataStore data = newDataStore(DummyValueDecl.class)
                .put(DummyDecl.DUMMY_CONTENT, declName)
                .put(NamedDecl.DECL_NAME, declName)
                .put(ValueDecl.TYPE, type);

        return new DummyValueDecl(data, Collections.emptyList());
        // return (DummyDecl) dummyDecl(node.getClass().getSimpleName())
        // .setLocation(node.getLocation());
    }

    public FunctionDecl functionDecl(String declName, Type type) {
        DataStore data = newDataStore(FunctionDecl.class)
                .put(NamedDecl.DECL_NAME, declName)
                .put(ValueDecl.TYPE, type);

        return new FunctionDecl(data, Collections.emptyList());
        // return (DummyDecl) dummyDecl(node.getClass().getSimpleName())
        // .setLocation(node.getLocation());
    }

    public VarDecl varDecl(String declName, Type type) {
        DataStore data = newDataStore(VarDecl.class)
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
        DataStore data = newDataStore(ParmVarDecl.class)
                .put(NamedDecl.DECL_NAME, declName)
                .put(ValueDecl.TYPE, type);

        return new ParmVarDecl(data, Collections.emptyList());
    }

    public RecordDecl recordDecl(String declName, TagKind kind, Collection<FieldDecl> fields) {
        DataStore data = newDataStore(RecordDecl.class)
                .put(RecordDecl.DECL_NAME, declName)
                .put(RecordDecl.TAG_KIND, kind);

        RecordDecl decl = new RecordDecl(data, fields);
        decl.set(RecordDecl.TYPE_FOR_DECL, Optional.of(recordType(decl)));
        decl.set(RecordDecl.IS_COMPLETE_DEFINITION);

        return decl;
    }

    public FieldDecl fieldDecl(String fieldName, Type fieldType) {
        DataStore data = newDataStore(FieldDecl.class)
                .put(FieldDecl.DECL_NAME, fieldName)
                .put(FieldDecl.TYPE, fieldType);

        return new FieldDecl(data, Arrays.asList(nullExpr(), nullExpr()));
    }

    public IncludeDecl includeDecl(Include include, String filepath) {
        DataStore data = newDataStore(IncludeDecl.class);

        data.set(IncludeDecl.INCLUDE, include);
        if (filepath != null) {
            data.set(IncludeDecl.LOCATION, new SourceRange(filepath, -1, -1, -1, -1));
        }

        return new IncludeDecl(data, Collections.emptyList());
    }

    public IncludeDecl includeDecl(String include, boolean isAngled) {
        return includeDecl(new Include(include, isAngled), null);
    }

    public IncludeDecl includeDecl(Include include) {
        return includeDecl(include, null);
    }

    public LiteralDecl literalDecl(String code) {
        DataStore data = newDataStore(LiteralDecl.class);

        data.set(LiteralDecl.LITERAL_CODE, code);

        return new LiteralDecl(data, Collections.emptyList());
    }

    public LinkageSpecDecl linkageSpecDecl(LanguageId language, Decl... decls) {
        return linkageSpecDecl(language, Arrays.asList(decls));
    }

    public LinkageSpecDecl linkageSpecDecl(LanguageId language, List<? extends Decl> decls) {
        DataStore data = newDataStore(LinkageSpecDecl.class);

        data.set(LinkageSpecDecl.LINKAGE_TYPE, language);

        return new LinkageSpecDecl(data, decls);
    }

    /// STMTS

    public LabelStmt labelStmt(String label) {
        return labelStmt(label, null);
    }

    public LabelStmt labelStmt(String label, Stmt subStmt) {
        DataStore data = newDataStore(LabelStmt.class)
                .set(LabelStmt.LABEL, label);

        return new LabelStmt(data, SpecsCollections.ofNullable(subStmt));
    }

    public WrapperStmt wrapperStmt(ClavaNode node) {
        DataStore data = newDataStore(WrapperStmt.class);

        return new WrapperStmt(data, Arrays.asList(node));
    }

    public NullStmt nullStmt() {
        return new NullStmt(newDataStore(NullStmt.class), Collections.emptyList());
    }

    public DeclStmt declStmt(Decl... decls) {
        return declStmt(Arrays.asList(decls));
    }

    public DeclStmt declStmt(List<Decl> decls) {
        DataStore data = newDataStore(DeclStmt.class);
        return new DeclStmt(data, decls);
    }

    public DeclStmt declStmt(RecordDecl recordDecl, List<VarDecl> varDecls) {
        DataStore data = newDataStore(DeclStmt.class);
        return new DeclStmt(data, SpecsCollections.concat(recordDecl, varDecls));
    }

    public ReturnStmt returnStmt(Expr retValue) {
        return new ReturnStmt(newDataStore(ReturnStmt.class), Arrays.asList(retValue));
    }

    public ReturnStmt returnStmt() {
        return new ReturnStmt(newDataStore(ReturnStmt.class), Collections.emptyList());
    }

    public LiteralStmt literalStmt(String literalCode) {
        return new LiteralStmt(newDataStore(LiteralStmt.class).put(LiteralStmt.LITERAL_CODE, literalCode),
                Collections.emptyList());
    }

    public CompoundStmt compoundStmt(String statement) {
        return compoundStmt(literalStmt(statement));
    }

    public CompoundStmt compoundStmt(Stmt... children) {
        return compoundStmt(Arrays.asList(children));
    }

    public CompoundStmt compoundStmt(Collection<Stmt> children) {
        return new CompoundStmt(newDataStore(CompoundStmt.class), children);
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
        DataStore exprStmtData = newDataStore(ExprStmt.class)
                .put(ExprStmt.HAS_SEMICOLON, true)
                .put(ClavaNode.LOCATION, expr.getLocation());

        return new ExprStmt(exprStmtData, Arrays.asList(expr));
    }

    public ExprStmt exprStmtAssignment(Expr lhs, Expr rhs) {
        // Create assignment
        BinaryOperator assign = binaryOperator(BinaryOperatorKind.ASSIGN, rhs.getExprType(), lhs,
                rhs);

        return exprStmt(assign);
    }

    public IfStmt ifStmt(Expr condition, CompoundStmt thenBody) {
        return ifStmt(condition, thenBody, nullStmt());
    }

    public IfStmt ifStmt(Expr condition, CompoundStmt thenBody, CompoundStmt elseBody) {
        return ifStmt(condition, thenBody, (ClavaNode) elseBody);
    }

    private IfStmt ifStmt(Expr condition, CompoundStmt thenBody, ClavaNode elseBody) {
        DataStore ifStmtData = newDataStore(IfStmt.class);

        return new IfStmt(ifStmtData, Arrays.asList(nullDecl(), condition, thenBody, elseBody));
    }

    public ForStmt forStmt(Stmt init, Stmt cond, Stmt inc, CompoundStmt body) {
        DataStore forStmtData = newDataStore(ForStmt.class);

        return new ForStmt(forStmtData, Arrays.asList(init, cond, inc, body));
    }

    public WhileStmt whileStmt(Stmt cond, CompoundStmt body) {
        DataStore whileStmtData = newDataStore(WhileStmt.class);

        return new WhileStmt(whileStmtData, Arrays.asList(cond, body));
    }

    public BreakStmt breakStmt() {
        DataStore breakStmtData = newDataStore(BreakStmt.class);

        return new BreakStmt(breakStmtData, Collections.emptyList());
    }

    public CaseStmt caseStmt(Expr caseExpr, Stmt subStmt) {
        DataStore caseStmtData = newDataStore(CaseStmt.class);

        return new CaseStmt(caseStmtData, Arrays.asList(caseExpr, subStmt));
    }

    public SwitchStmt switchStmt(Expr condition, Stmt body) {
        DataStore data = newDataStore(SwitchStmt.class);

        return new SwitchStmt(data, Arrays.asList(condition, body));
    }

    public DummyStmt dummyStmt(ClavaNode node) {
        DataStore data = newDataStore(DummyStmt.class)
                .put(DummyNode.DUMMY_CONTENT, node.toString());

        return new DummyStmt(data, node.getChildren());
    }

    public DummyStmt dummyStmt(String dummyContent) {
        DataStore data = newDataStore(DummyStmt.class)
                .put(DummyNode.DUMMY_CONTENT, dummyContent);

        return new DummyStmt(data, Collections.emptyList());
    }

    /// ATTRIBUTES

    public DummyAttr dummyAttr(String dummyContent) {
        DataStore data = newDataStore(DummyAttr.class)
                .put(DummyNode.DUMMY_CONTENT, dummyContent);

        return new DummyAttr(data, Collections.emptyList());
    }

    /// PRAGMAS

    public GenericPragma genericPragma(List<String> content) {
        // content = content instanceof ArrayList ? content : new ArrayList<>(content);

        DataStore data = newDataStore(GenericPragma.class)
                .set(GenericPragma.CONTENT, new ArrayList<>(content));

        return new GenericPragma(data, Collections.emptyList());
    }

    public SimpleOmpPragma simpleOmpPragma(OmpDirectiveKind kind) {
        DataStore data = newDataStore(SimpleOmpPragma.class)
                .set(OmpPragma.DIRECTIVE_KIND, kind);

        return new SimpleOmpPragma(data, Collections.emptyList());
    }

    public OmpClausePragma ompClausePragma(OmpDirectiveKind kind) {
        return ompClausePragma(kind, new LinkedHashMap<>());
    }

    public OmpClausePragma ompClausePragma(OmpDirectiveKind kind, Map<OmpClauseKind, List<OmpClause>> clauses) {
        DataStore data = newDataStore(OmpClausePragma.class)
                .set(OmpPragma.DIRECTIVE_KIND, kind)
                .set(OmpClausePragma.CLAUSES, clauses);

        return new OmpClausePragma(data, Collections.emptyList());
    }

    public OmpLiteralPragma ompLiteralPragma(OmpDirectiveKind kind, String customContent) {
        DataStore data = newDataStore(OmpLiteralPragma.class)
                .set(OmpPragma.DIRECTIVE_KIND, kind)
                .set(OmpLiteralPragma.CUSTOM_CONTENT, customContent);

        return new OmpLiteralPragma(data, Collections.emptyList());
    }

    /// COMMENTS

    public InlineComment inlineComment(String text, boolean isStmtComment) {
        DataStore data = newDataStore(InlineComment.class)
                .set(InlineComment.TEXT, text)
                .set(InlineComment.IS_STMT_COMMENT, isStmtComment);

        return new InlineComment(data, Collections.emptyList());
    }

    public MultiLineComment multiLineComment(List<String> lines) {
        DataStore data = newDataStore(MultiLineComment.class)
                .set(MultiLineComment.LINES, new ArrayList<>(lines));

        return new MultiLineComment(data, Collections.emptyList());
    }

}
