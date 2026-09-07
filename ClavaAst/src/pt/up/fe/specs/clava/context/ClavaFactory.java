/**
 * Copyright 2018 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.context;

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
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.decl.enums.LanguageId;
import pt.up.fe.specs.clava.ast.expr.*;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.FloatKind;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorPosition;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TagDeclVars;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.lara.LaraMarkerPragma;
import pt.up.fe.specs.clava.ast.lara.LaraTagPragma;
import pt.up.fe.specs.clava.ast.omp.*;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind;
import pt.up.fe.specs.clava.ast.pragma.GenericPragma;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.ast.stmt.*;
import pt.up.fe.specs.clava.ast.type.*;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.ast.type.enums.C99Qualifier;
import pt.up.fe.specs.clava.ast.type.enums.ElaboratedTypeKeyword;
import pt.up.fe.specs.clava.language.AccessSpecifier;
import pt.up.fe.specs.clava.language.CastKind;
import pt.up.fe.specs.clava.language.TagKind;
import pt.up.fe.specs.clava.language.UnaryExprOrTypeTrait;
import pt.up.fe.specs.clava.utils.ClassesService;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.classmap.ClassMap;

import java.io.File;
import java.math.BigInteger;
import java.util.*;

/**
 * Factory methods for ClavaNodes that use the DataStore format.
 *
 * <p>
 * This class provides minimal methods for building new nodes, further specialization should be done using the .set()
 * method of the respective node.
 *
 * @author JoaoBispo
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

    public DataStore newDataStore(Class<? extends ClavaNode> nodeClass) {

        // Get the correct prefix for the given class
        String idPrefix = PREFIX_MAP.get(nodeClass);

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

    /// EXTRA

    public App app(List<TranslationUnit> tUnits) {
        return node(App.class, tUnits);
    }

    public TranslationUnit translationUnit(File sourceFile, Collection<Decl> declarations) {
        DataStore data = newDataStore(TranslationUnit.class);
        TranslationUnit.setDataStore(sourceFile, data);

        return new TranslationUnit(data, declarations);
    }

    public TagDeclVars tagDeclVars(List<? extends NamedDecl> children) {
        return node(TagDeclVars.class, children);
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
        return (FunctionProtoType) node(FunctionProtoType.class, Collections.singletonList(returnType))
                .set(FunctionProtoType.NUM_PARAMETERS, argTypes.size())
                .set(FunctionProtoType.RETURN_TYPE, returnType)
                .set(FunctionProtoType.PARAMETERS_TYPES, new ArrayList<>(argTypes));
    }

    public NullType nullType() {
        return node(NullType.class, Collections.emptyList());
    }

    public BuiltinType builtinType(String literalKind) {
        BuiltinKind kind = BuiltinKind.newInstance(literalKind);
        BuiltinType type = builtinType(kind);
        type.setKindLiteral(literalKind);

        return type;
    }

    public BuiltinType builtinType(BuiltinKind kind) {
        return (BuiltinType) node(BuiltinType.class, Collections.emptyList())
                .set(BuiltinType.KIND, kind);
    }

    public DummyType dummyType(String dummyContent) {
        return (DummyType) node(DummyType.class, Collections.emptyList())
                .set(DummyType.DUMMY_CONTENT, dummyContent);
    }

    public LiteralType literalType(String code) {
        return (LiteralType) node(LiteralType.class, Collections.emptyList())
                .set(LiteralNode.LITERAL_CODE, code);
    }

    public ConstantArrayType constantArrayType(Type elementType, int size) {
        return constantArrayType(elementType, BigInteger.valueOf(size));
    }

    public ConstantArrayType constantArrayType(Type elementType, BigInteger size) {
        return (ConstantArrayType) node(ConstantArrayType.class, Collections.emptyList())
                .set(ConstantArrayType.ARRAY_SIZE, size)
                .set(ConstantArrayType.ELEMENT_TYPE, elementType);
    }

    public VariableArrayType variableArrayType(Type elementType, Expr sizeExpr) {
        return (VariableArrayType) node(VariableArrayType.class, Collections.emptyList())
                .set(VariableArrayType.ELEMENT_TYPE, elementType)
                .set(VariableArrayType.SIZE_EXPR, sizeExpr);
    }

    public IncompleteArrayType incompleteArrayType(Type elementType) {
        return (IncompleteArrayType) node(IncompleteArrayType.class, Collections.emptyList())
                .set(ArrayType.ELEMENT_TYPE, elementType);
    }

    public PointerType pointerType(Type pointeeType) {
        return (PointerType) node(PointerType.class, Collections.emptyList())
                .set(PointerType.POINTEE_TYPE, pointeeType);
    }

    public RecordType recordType(TagDecl recordDecl) {
        return (RecordType) node(RecordType.class, Collections.emptyList())
                .set(RecordType.DECL, recordDecl);
    }

    public QualType qualType(Type unqualifiedType, C99Qualifier... qualifiers) {
        SpecsCheck.checkArgument(!(unqualifiedType instanceof QualType),
                () -> "Unqualified type is qualified: " + unqualifiedType);
        SpecsCheck.checkArgument(qualifiers.length > 0, () -> "Must have at least one qualifier");

        // Ensure only one of each
        var qualifiersList = qualifiers.length == 1 ? Arrays.asList(qualifiers)
                : new ArrayList<>(new HashSet<>(Arrays.asList(qualifiers)));

        return (QualType) node(QualType.class, Collections.emptyList())
                .set(QualType.UNQUALIFIED_TYPE, unqualifiedType)
                .set(QualType.C99_QUALIFIERS, qualifiersList);
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

    public PointerToMemberExpr pointerToMemberExpr(String memberName, Type memberType, Expr baseExpr, boolean isArrow) {
        return (PointerToMemberExpr) node(PointerToMemberExpr.class, Arrays.asList(baseExpr))
                .set(MemberExpr.MEMBER_NAME, memberName)
                .set(Expr.TYPE, Optional.of(memberType))
                .set(MemberExpr.IS_ARROW, isArrow);
    }

    public DummyExpr dummyExpr(String dummyContent) {
        return (DummyExpr) node(DummyExpr.class, Collections.emptyList())
                .set(DummyExpr.DUMMY_CONTENT, dummyContent)
                .set(Expr.TYPE, Optional.of(dummyType("dummy type")));
    }

    public NullExpr nullExpr() {
        return (NullExpr) node(NullExpr.class, Collections.emptyList())
                .set(Expr.TYPE, Optional.of(nullType()));
    }

    public IntegerLiteral integerLiteral(long integer) {
        return integerLiteral(BigInteger.valueOf(integer));
    }

    public IntegerLiteral integerLiteral(BigInteger integer) {
        return (IntegerLiteral) node(IntegerLiteral.class, Collections.emptyList())
                .set(Literal.SOURCE_LITERAL, integer.toString())
                .set(IntegerLiteral.VALUE, integer)
                .set(Expr.TYPE, Optional.of(builtinType(BuiltinKind.Int)));
    }

    public FloatingLiteral floatingLiteral(FloatKind floatKind, double value) {
        return (FloatingLiteral) node(FloatingLiteral.class, Collections.emptyList())
                .set(Literal.SOURCE_LITERAL, Double.toString(value))
                .set(FloatingLiteral.VALUE, value)
                .set(Expr.TYPE, Optional.of(builtinType(floatKind.getBuiltinKind())));
    }

    public LiteralExpr literalExpr(String code, Type type) {
        return (LiteralExpr) node(LiteralExpr.class, Collections.emptyList())
                .set(LiteralNode.LITERAL_CODE, code)
                .set(Expr.TYPE, Optional.of(type));
    }

    public DeclRefExpr declRefExpr(ValueDecl decl) {
        DeclRefExpr declRefExpr = declRefExpr(decl.get(ValueDecl.DECL_NAME), decl.getType());
        declRefExpr.set(DeclRefExpr.DECL, decl);

        return declRefExpr;
    }

    public DeclRefExpr declRefExpr(String declName, Type type) {
        DataStore data = newDataStore(DeclRefExpr.class)
                .put(Expr.TYPE, Optional.of(type));

        DeclRefExpr declRefExpr = new DeclRefExpr(data, Collections.emptyList());
        declRefExpr.setName(declName);

        return declRefExpr;
    }

    public CallExpr callExpr(Expr function, Type type, List<? extends Expr> args) {
        return (CallExpr) node(CallExpr.class, SpecsCollections.concat(function, args))
                .set(Expr.TYPE, Optional.of(type));
    }

    public CallExpr callExpr(FunctionDecl function, List<? extends Expr> args) {
        DeclRefExpr declRef = declRefExpr(function.getDeclName(), function.getReturnType());
        var callExpr = callExpr(declRef, function.getType(), args);

        // Set callee
        callExpr.set(CallExpr.DIRECT_CALLEE, Optional.of(function));

        return callExpr;
    }

    public CXXFunctionalCastExpr cxxFunctionalCastExpr(CastExpr expr, Expr subExpr) {
        DataStore data = expr.getFactoryWithNode().newDataStore(CXXFunctionalCastExpr.class);

        return new CXXFunctionalCastExpr(data, Arrays.asList(subExpr));
    }

    public BinaryOperator binaryOperator(BinaryOperatorKind op, Type type, Expr lhs, Expr rhs) {
        return (BinaryOperator) node(BinaryOperator.class, Arrays.asList(lhs, rhs))
                .set(Expr.TYPE, Optional.of(type))
                .set(BinaryOperator.OP, op);
    }

    public CompoundAssignOperator compoundAssignOperator(BinaryOperatorKind op, Type type, Expr lhs, Expr rhs) {
        if (!op.isCompoundAssign()) {
            throw new IllegalArgumentException(
                    "`op` should be a compound assignment op kind, is actually " + op.getOpString());
        }

        return (CompoundAssignOperator) node(CompoundAssignOperator.class, Arrays.asList(lhs, rhs))
                .set(Expr.TYPE, Optional.of(type))
                .set(BinaryOperator.OP, op);
    }

    public UnaryOperator unaryOperator(UnaryOperatorKind op, Type type, Expr subExpr) {
        var unaryOperator = (UnaryOperator) node(UnaryOperator.class, Arrays.asList(subExpr))
                .set(Expr.TYPE, Optional.of(type))
                .set(UnaryOperator.OP, op);

        if (op.equals(UnaryOperatorKind.PostInc) || op.equals(UnaryOperatorKind.PostDec)) {
            unaryOperator.set(UnaryOperator.POSITION, UnaryOperatorPosition.POSTFIX);
        }

        return unaryOperator;
    }

    public ConditionalOperator conditionalOperator(Type type, Expr condition, Expr trueExpr, Expr falseExpr) {
        return (ConditionalOperator) node(ConditionalOperator.class, Arrays.asList(condition, trueExpr, falseExpr))
                .set(Expr.TYPE, Optional.of(type));
    }

    public CStyleCastExpr cStyleCastExpr(Type type, Expr expr) {
        return (CStyleCastExpr) node(CStyleCastExpr.class, Arrays.asList(expr))
                .set(Expr.TYPE, Optional.of(type))
                .set(CastExpr.CAST_KIND, CastKind.NoOp);
    }

    public ParenExpr parenExpr(Expr expr) {
        return node(ParenExpr.class, Arrays.asList(expr));
    }

    public CXXConstructExpr cxxConstructExpr(Type type, List<Expr> constructorArguments) {
        return (CXXConstructExpr) node(CXXConstructExpr.class, constructorArguments)
                .set(Expr.TYPE, Optional.of(type));
    }

    public ArraySubscriptExpr arraySubscriptExpr(Expr base, List<Expr> subscripts) {
        if (subscripts.isEmpty()) {
            throw new RuntimeException("To create an array subscript we need at least one subscript");
        }


        if (!(base.getType() instanceof ArrayType)) {
            throw new RuntimeException("Base expression must be an array type (e.g., constArrayType)");
        }

        var arrayType = (ArrayType) base.getType();


        if (subscripts.size() > arrayType.getArrayDims().size()) {
            throw new RuntimeException("Number of subscripts (" + subscripts.size() + ") greater than the number of dimensions of the array type (" + arrayType.getArrayDims().size() + ")");
        }


        // Lhs are either other ArraySubscriptExpr or DeclRefExpr
        // Rhs always contains the index
        // Indexes appear from inside to outside

        // Always set the element type of the array type
        var subscriptType = arrayType.getElementType();

        DataStore data = newDataStore(ArraySubscriptExpr.class)
                .set(Expr.TYPE, Optional.of(subscriptType));

        // Create first level
        var currentArraySubscript = new ArraySubscriptExpr(data, Arrays.asList(base, subscripts.get(0)));


        // Add remaining levels
        for (int i = 1; i < subscripts.size(); i++) {
            subscriptType = ((ArrayType) subscriptType).getElementType();
            var newData = newDataStore(ArraySubscriptExpr.class)
                    .set(Expr.TYPE, Optional.of(subscriptType));

            var newChildren = Arrays.asList(currentArraySubscript, subscripts.get(i));

            currentArraySubscript = new ArraySubscriptExpr(newData, newChildren);
        }

        return currentArraySubscript;
    }

    public InitListExpr initListExpr(List<Expr> values) {
        if (values.isEmpty()) {
            throw new RuntimeException("To create an initList expression we need at least one value");
        }

        return (InitListExpr) node(InitListExpr.class, values)
                .set(Expr.TYPE, Optional.of(constantArrayType(values.get(0).getType(), values.size())))
                .set(InitListExpr.IS_EXPLICIT, true);
    }

    public UnaryExprOrTypeTraitExpr sizeof(Type typeArg) {
        return (UnaryExprOrTypeTraitExpr) node(UnaryExprOrTypeTraitExpr.class, Collections.emptyList())
                .set(UnaryExprOrTypeTraitExpr.KIND, UnaryExprOrTypeTrait.SizeOf)
                .set(UnaryExprOrTypeTraitExpr.IS_ARGUMENT_TYPE, true)
                .set(UnaryExprOrTypeTraitExpr.ARG_TYPE, Optional.of(typeArg));
    }

    public UnaryExprOrTypeTraitExpr sizeof(Expr typeExpr) {
        return (UnaryExprOrTypeTraitExpr) node(UnaryExprOrTypeTraitExpr.class, List.of(typeExpr))
                .set(UnaryExprOrTypeTraitExpr.KIND, UnaryExprOrTypeTrait.SizeOf)
                .set(UnaryExprOrTypeTraitExpr.IS_ARGUMENT_TYPE, false);
    }

    /// DECLS

    public NullDecl nullDecl() {
        return node(NullDecl.class, Collections.emptyList());
    }

    public DummyDecl dummyDecl(String dummyContent) {
        return (DummyDecl) node(DummyDecl.class, Collections.emptyList())
                .set(DummyDecl.DUMMY_CONTENT, dummyContent);
    }

    public DummyDecl dummyDecl(ClavaNode node) {
        return (DummyDecl) dummyDecl(node.getClass().getSimpleName())
                .setLocation(node.getLocation());
    }

    public DummyNamedDecl dummyNamedDecl(String declName) {
        return (DummyNamedDecl) node(DummyNamedDecl.class, Collections.emptyList())
                .set(DummyDecl.DUMMY_CONTENT, declName)
                .set(NamedDecl.DECL_NAME, declName);
    }

    public DummyValueDecl dummyValueDecl(String declName, Type type) {
        return (DummyValueDecl) node(DummyValueDecl.class, Collections.emptyList())
                .set(DummyDecl.DUMMY_CONTENT, declName)
                .set(NamedDecl.DECL_NAME, declName)
                .set(ValueDecl.TYPE, type);
    }

    public FunctionDecl functionDecl(String declName, Type type) {
        return (FunctionDecl) node(FunctionDecl.class, Collections.emptyList())
                .set(NamedDecl.DECL_NAME, declName)
                .set(ValueDecl.TYPE, type);
    }

    public VarDecl varDecl(String declName, Type type) {
        return (VarDecl) node(VarDecl.class, Collections.emptyList())
                .set(NamedDecl.DECL_NAME, declName)
                .set(ValueDecl.TYPE, type);
    }

    public VarDecl varDecl(String declName, Expr initExpr) {
        VarDecl varDecl = varDecl(declName, initExpr.getType());
        varDecl.setInit(initExpr);
        return varDecl;
    }

    public ParmVarDecl parmVarDecl(String declName, Type type) {
        return (ParmVarDecl) node(ParmVarDecl.class, Collections.emptyList())
                .set(NamedDecl.DECL_NAME, declName)
                .set(ValueDecl.TYPE, type);
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

    public CXXRecordDecl cxxRecordDecl(String declName, Collection<FieldDecl> fields) {

        DataStore data = newDataStore(CXXRecordDecl.class)
                .put(RecordDecl.DECL_NAME, declName)
                .put(RecordDecl.TAG_KIND, TagKind.CLASS)
                .put(CXXRecordDecl.RECORD_BASES, new ArrayList<>());

        CXXRecordDecl decl = new CXXRecordDecl(data, fields);
        decl.set(RecordDecl.TYPE_FOR_DECL, Optional.of(recordType(decl)));
        decl.set(RecordDecl.IS_COMPLETE_DEFINITION);

        return decl;
    }

    public FieldDecl fieldDecl(String fieldName, Type fieldType) {
        return (FieldDecl) node(FieldDecl.class, Arrays.asList(nullExpr(), nullExpr()))
                .set(FieldDecl.DECL_NAME, fieldName)
                .set(FieldDecl.TYPE, fieldType);
    }

    public IncludeDecl includeDecl(Include include, String filepath) {
        DataStore data = newDataStore(IncludeDecl.class);

        data.set(IncludeDecl.INCLUDE, include);

        var locFilepath = filepath != null ? filepath : "<no filepath>";
        var line = include.getLine();

        // Build location for include
        var location = new SourceRange(locFilepath, line, 0, line, include.getInclude().length());
        data.set(IncludeDecl.LOCATION, location);

        return new IncludeDecl(data, Collections.emptyList());
    }

    public IncludeDecl includeDecl(String include, boolean isAngled) {
        return includeDecl(new Include(include, isAngled), null);
    }

    public IncludeDecl includeDecl(Include include) {
        return includeDecl(include, null);
    }

    public CIncludeDecl cIncludeDecl(Include include, String filepath) {
        DataStore data = newDataStore(CIncludeDecl.class);

        data.set(IncludeDecl.INCLUDE, include);
        if (filepath != null) {
            data.set(IncludeDecl.LOCATION, new SourceRange(filepath, -1, -1, -1, -1));
        }

        return new CIncludeDecl(data, Collections.emptyList());
    }

    public CIncludeDecl cIncludeDecl(String include, boolean isAngled) {
        return cIncludeDecl(new Include(include, isAngled), null);
    }

    public LiteralDecl literalDecl(String code) {
        return (LiteralDecl) node(LiteralDecl.class, Collections.emptyList())
                .set(LiteralDecl.LITERAL_CODE, code);
    }

    public LinkageSpecDecl linkageSpecDecl(LanguageId language, Decl... decls) {
        return linkageSpecDecl(language, Arrays.asList(decls));
    }

    public LinkageSpecDecl linkageSpecDecl(LanguageId language, List<? extends Decl> decls) {
        return (LinkageSpecDecl) node(LinkageSpecDecl.class, decls)
                .set(LinkageSpecDecl.LINKAGE_TYPE, language);
    }

    public AccessSpecDecl accessSpecDecl(AccessSpecifier accessSpecifier) {
        return (AccessSpecDecl) node(AccessSpecDecl.class, Collections.emptyList())
                .set(AccessSpecDecl.ACCESS_SPECIFIER, accessSpecifier);
    }

    public LabelDecl labelDecl(String declName) {
        return (LabelDecl) node(LabelDecl.class, Collections.emptyList())
                .set(LabelDecl.DECL_NAME, declName);
    }

    /// STMTS

    public LabelStmt labelStmt(LabelDecl labelDecl) {
        return labelStmt(labelDecl, null);
    }

    public LabelStmt labelStmt(LabelDecl label, Stmt subStmt) {
        DataStore data = newDataStore(LabelStmt.class)
                .set(LabelStmt.LABEL, label);


        var labelStmt = new LabelStmt(data, SpecsCollections.ofNullable(subStmt));

        // Update labelDecl to refer to this labelStmt
        label.setOptional(LabelDecl.LABEL_STMT, labelStmt);

        return labelStmt;
    }

    public GotoStmt gotoStmt(LabelDecl label) {
        return (GotoStmt) node(GotoStmt.class, Collections.emptyList())
                .set(GotoStmt.LABEL, label);
    }

    public EmptyStmt emptyStmt() {
        return node(EmptyStmt.class, Collections.emptyList());
    }

    public WrapperStmt wrapperStmt(ClavaNode node) {
        return node(WrapperStmt.class, Arrays.asList(node));
    }

    public NullStmt nullStmt() {
        return node(NullStmt.class, Collections.emptyList());
    }

    public DeclStmt declStmt(Decl... decls) {
        return declStmt(Arrays.asList(decls));
    }

    public DeclStmt declStmt(List<Decl> decls) {
        return node(DeclStmt.class, decls);
    }

    public DeclStmt declStmt(RecordDecl recordDecl, List<VarDecl> varDecls) {
        return node(DeclStmt.class, SpecsCollections.concat(recordDecl, varDecls));
    }

    public ReturnStmt returnStmt(Expr retValue) {
        return node(ReturnStmt.class, Arrays.asList(retValue));
    }

    public ReturnStmt returnStmt() {
        return node(ReturnStmt.class, Collections.emptyList());
    }

    public LiteralStmt literalStmt(String literalCode) {
        return (LiteralStmt) node(LiteralStmt.class, Collections.emptyList())
                .set(LiteralStmt.LITERAL_CODE, literalCode);
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

    /**
     * Creates an ExprStmt with semicolon.
     *
     * @param expr
     * @return
     */
    public ExprStmt exprStmt(Expr expr) {
        return (ExprStmt) node(ExprStmt.class, Arrays.asList(expr))
                .set(ClavaNode.LOCATION, expr.getLocation());
    }

    public ExprStmt exprStmtAssignment(Expr lhs, Expr rhs) {

        // Create assignment
        BinaryOperator assign = binaryOperator(BinaryOperatorKind.Assign, rhs.getExprType(), lhs,
                rhs);

        return exprStmt(assign);
    }

    /**
     * @param condition
     * @param thenBody  can be null (i.e., empty body)
     * @param elseBody  can be null (i.e., no else)
     * @return
     */
    public IfStmt ifStmt(Expr condition, CompoundStmt thenBody, CompoundStmt elseBody) {

        Objects.requireNonNull(condition, () -> "Condition of IfStmt must exist");

        // If null, create empty CompoundStmt
        ClavaNode thenStmt = thenBody != null ? thenBody : compoundStmt();

        // If null, create NullStmt
        ClavaNode elseStmt = elseBody != null ? elseBody : nullStmt();

        return (IfStmt) node(IfStmt.class, Arrays.asList(nullDecl(), condition, thenStmt, elseStmt));
    }

    public ForStmt forStmt(Stmt init, Stmt cond, Stmt inc, CompoundStmt body) {
        return node(ForStmt.class, Arrays.asList(init, cond, inc, body, nullDecl()));
    }

    public WhileStmt whileStmt(Stmt cond, CompoundStmt body) {
        return node(WhileStmt.class, Arrays.asList(nullDecl(), cond, body));
    }

    public BreakStmt breakStmt() {
        return node(BreakStmt.class, Collections.emptyList());
    }

    public CaseStmt caseStmt(Expr caseExpr) {
        return node(CaseStmt.class, Arrays.asList(caseExpr, nullExpr()));
    }

    public DefaultStmt defaultStmt() {
        return node(DefaultStmt.class, Collections.emptyList());
    }

    public SwitchStmt switchStmt(Expr condition, Stmt body) {
        return node(SwitchStmt.class, Arrays.asList(condition, body));
    }

    public DummyStmt dummyStmt(ClavaNode node) {
        return (DummyStmt) node(DummyStmt.class, node.getChildren())
                .set(DummyNode.DUMMY_CONTENT, node.toString());
    }

    public DummyStmt dummyStmt(String dummyContent) {
        return (DummyStmt) node(DummyStmt.class, Collections.emptyList())
                .set(DummyNode.DUMMY_CONTENT, dummyContent);
    }

    /// ATTRIBUTES

    public DummyAttr dummyAttr(String dummyContent) {
        return (DummyAttr) node(DummyAttr.class, Collections.emptyList())
                .set(DummyNode.DUMMY_CONTENT, dummyContent);
    }

    /// PRAGMAS

    public GenericPragma genericPragma(List<String> content) {
        return (GenericPragma) node(GenericPragma.class, Collections.emptyList())
                .set(GenericPragma.CONTENT, new ArrayList<>(content));
    }

    public SimpleOmpPragma simpleOmpPragma(OmpDirectiveKind kind) {
        return (SimpleOmpPragma) node(SimpleOmpPragma.class, Collections.emptyList())
                .set(OmpPragma.DIRECTIVE_KIND, kind);
    }

    public OmpClausePragma ompClausePragma(OmpDirectiveKind kind) {
        return ompClausePragma(kind, new LinkedHashMap<>());
    }

    public OmpClausePragma ompClausePragma(OmpDirectiveKind kind, Map<OmpClauseKind, List<OmpClause>> clauses) {
        return (OmpClausePragma) node(OmpClausePragma.class, Collections.emptyList())
                .set(OmpPragma.DIRECTIVE_KIND, kind)
                .set(OmpClausePragma.CLAUSES, clauses);
    }

    public OmpLiteralPragma ompLiteralPragma(OmpDirectiveKind kind, String customContent) {
        return (OmpLiteralPragma) node(OmpLiteralPragma.class, Collections.emptyList())
                .set(OmpPragma.DIRECTIVE_KIND, kind)
                .set(OmpLiteralPragma.CUSTOM_CONTENT, customContent);
    }

    public LaraMarkerPragma laraMarkerPragma(String markedId) {
        return (LaraMarkerPragma) node(LaraMarkerPragma.class, Collections.emptyList())
                .set(LaraMarkerPragma.MARKER_ID, markedId);
    }

    public LaraTagPragma laraTagPragma(String tagId) {
        return (LaraTagPragma) node(LaraTagPragma.class, Collections.emptyList())
                .set(LaraTagPragma.TAG_ID, tagId);
    }

    /// COMMENTS

    public InlineComment inlineComment(String text, boolean isStmtComment) {
        return (InlineComment) node(InlineComment.class, Collections.emptyList())
                .set(InlineComment.TEXT, text)
                .set(InlineComment.IS_STMT_COMMENT, isStmtComment);
    }

    public MultiLineComment multiLineComment(List<String> lines) {
        return (MultiLineComment) node(MultiLineComment.class, Collections.emptyList())
                .set(MultiLineComment.LINES, new ArrayList<>(lines));
    }

    public TypedefType typedefType(TypedefDecl typedefDecl) {
        return (TypedefType) node(TypedefType.class, Collections.emptyList())
                .set(TypedefType.DECL, typedefDecl);
    }

    public TypedefDecl typedefDecl(Type node, String identifier) {
        return (TypedefDecl) node(TypedefDecl.class, Collections.emptyList())
                .set(NamedDecl.DECL_NAME, identifier)
                .set(TypedefDecl.UNDERLYING_TYPE, node);
    }

    public ElaboratedType elaboratedType(ElaboratedTypeKeyword keyword, Type namedType) {
        return (ElaboratedType) node(ElaboratedType.class, Collections.emptyList())
                .set(TypeWithKeyword.ELABORATED_TYPE_KEYWORD, keyword)
                .set(ElaboratedType.NAMED_TYPE, namedType);
    }

}
