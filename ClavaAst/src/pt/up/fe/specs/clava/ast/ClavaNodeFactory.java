/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.ast;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.attr.FinalAttr;
import pt.up.fe.specs.clava.ast.attr.data.AttrData;
import pt.up.fe.specs.clava.ast.comment.BlockContentComment;
import pt.up.fe.specs.clava.ast.comment.Comment;
import pt.up.fe.specs.clava.ast.comment.DummyComment;
import pt.up.fe.specs.clava.ast.comment.FullComment;
import pt.up.fe.specs.clava.ast.comment.InlineCommandComment;
import pt.up.fe.specs.clava.ast.comment.InlineComment;
import pt.up.fe.specs.clava.ast.comment.InlineContentComment;
import pt.up.fe.specs.clava.ast.comment.MultiLineComment;
import pt.up.fe.specs.clava.ast.comment.ParagraphComment;
import pt.up.fe.specs.clava.ast.comment.TextComment;
import pt.up.fe.specs.clava.ast.decl.AccessSpecDecl;
import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.decl.CXXDestructorDecl;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.DummyDecl;
import pt.up.fe.specs.clava.ast.decl.EnumConstantDecl;
import pt.up.fe.specs.clava.ast.decl.EnumDecl;
import pt.up.fe.specs.clava.ast.decl.EnumDecl.EnumScopeType;
import pt.up.fe.specs.clava.ast.decl.FieldDecl;
import pt.up.fe.specs.clava.ast.decl.FriendDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionTemplateDecl;
import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.ast.decl.LinkageSpecDecl;
import pt.up.fe.specs.clava.ast.decl.LiteralDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.NamespaceAliasDecl;
import pt.up.fe.specs.clava.ast.decl.NamespaceDecl;
import pt.up.fe.specs.clava.ast.decl.NullDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateTypeParmDecl;
import pt.up.fe.specs.clava.ast.decl.TypeAliasDecl;
import pt.up.fe.specs.clava.ast.decl.TypedefDecl;
import pt.up.fe.specs.clava.ast.decl.UsingDecl;
import pt.up.fe.specs.clava.ast.decl.UsingDirectiveDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.decl.data.CXXMethodDeclData;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.FunctionDeclData;
import pt.up.fe.specs.clava.ast.decl.data.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.data.LanguageId;
import pt.up.fe.specs.clava.ast.decl.data.NestedNamedSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.RecordBase;
import pt.up.fe.specs.clava.ast.decl.data.RecordDeclData;
import pt.up.fe.specs.clava.ast.decl.data.StorageClass;
import pt.up.fe.specs.clava.ast.decl.data.VarDeclData;
import pt.up.fe.specs.clava.ast.expr.ArraySubscriptExpr;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.CStyleCastExpr;
import pt.up.fe.specs.clava.ast.expr.CXXBindTemporaryExpr;
import pt.up.fe.specs.clava.ast.expr.CXXBoolLiteralExpr;
import pt.up.fe.specs.clava.ast.expr.CXXConstCastExpr;
import pt.up.fe.specs.clava.ast.expr.CXXConstructExpr;
import pt.up.fe.specs.clava.ast.expr.CXXDefaultArgExpr;
import pt.up.fe.specs.clava.ast.expr.CXXDefaultInitExpr;
import pt.up.fe.specs.clava.ast.expr.CXXDeleteExpr;
import pt.up.fe.specs.clava.ast.expr.CXXFunctionalCastExpr;
import pt.up.fe.specs.clava.ast.expr.CXXMemberCallExpr;
import pt.up.fe.specs.clava.ast.expr.CXXNewExpr;
import pt.up.fe.specs.clava.ast.expr.CXXNullPtrLiteralExpr;
import pt.up.fe.specs.clava.ast.expr.CXXOperatorCallExpr;
import pt.up.fe.specs.clava.ast.expr.CXXReinterpretCastExpr;
import pt.up.fe.specs.clava.ast.expr.CXXStaticCastExpr;
import pt.up.fe.specs.clava.ast.expr.CXXStdInitializerListExpr;
import pt.up.fe.specs.clava.ast.expr.CXXTemporaryObjectExpr;
import pt.up.fe.specs.clava.ast.expr.CXXThisExpr;
import pt.up.fe.specs.clava.ast.expr.CXXThrowExpr;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.CharacterLiteral;
import pt.up.fe.specs.clava.ast.expr.CompoundAssignOperator;
import pt.up.fe.specs.clava.ast.expr.ConditionalOperator;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.DummyExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.ExprWithCleanups;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral.FloatKind;
import pt.up.fe.specs.clava.ast.expr.GNUNullExpr;
import pt.up.fe.specs.clava.ast.expr.ImplicitCastExpr;
import pt.up.fe.specs.clava.ast.expr.ImplicitValueInitExpr;
import pt.up.fe.specs.clava.ast.expr.InitListExpr;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.LiteralExpr;
import pt.up.fe.specs.clava.ast.expr.MaterializeTemporaryExpr;
import pt.up.fe.specs.clava.ast.expr.MemberExpr;
import pt.up.fe.specs.clava.ast.expr.NullExpr;
import pt.up.fe.specs.clava.ast.expr.OffsetOfExpr;
import pt.up.fe.specs.clava.ast.expr.ParenExpr;
import pt.up.fe.specs.clava.ast.expr.PredefinedExpr;
import pt.up.fe.specs.clava.ast.expr.PredefinedExpr.PredefinedIdType;
import pt.up.fe.specs.clava.ast.expr.StringLiteral;
import pt.up.fe.specs.clava.ast.expr.UnaryExprOrTypeTraitExpr;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator.UnaryOperatorPosition;
import pt.up.fe.specs.clava.ast.expr.UnresolvedLookupExpr;
import pt.up.fe.specs.clava.ast.expr.UserDefinedLiteral;
import pt.up.fe.specs.clava.ast.expr.data.CXXConstructExprData;
import pt.up.fe.specs.clava.ast.expr.data.CXXNamedCastExprData;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data.OffsetOfData;
import pt.up.fe.specs.clava.ast.expr.data.ValueKind;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.CXXCtorInitializer;
import pt.up.fe.specs.clava.ast.extra.NullNode;
import pt.up.fe.specs.clava.ast.extra.OriginalNamespace;
import pt.up.fe.specs.clava.ast.extra.TemplateArgument;
import pt.up.fe.specs.clava.ast.extra.TemplateArgumentExpr;
import pt.up.fe.specs.clava.ast.extra.TemplateArgumentType;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.Undefined;
import pt.up.fe.specs.clava.ast.extra.VariadicType;
import pt.up.fe.specs.clava.ast.omp.OMPParallelDirective;
import pt.up.fe.specs.clava.ast.pragma.GenericPragma;
import pt.up.fe.specs.clava.ast.stmt.BreakStmt;
import pt.up.fe.specs.clava.ast.stmt.CXXCatchStmt;
import pt.up.fe.specs.clava.ast.stmt.CXXForRangeStmt;
import pt.up.fe.specs.clava.ast.stmt.CXXTryStmt;
import pt.up.fe.specs.clava.ast.stmt.CapturedStmt;
import pt.up.fe.specs.clava.ast.stmt.CaseStmt;
import pt.up.fe.specs.clava.ast.stmt.ClangLabelStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.ContinueStmt;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.DefaultStmt;
import pt.up.fe.specs.clava.ast.stmt.DoStmt;
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
import pt.up.fe.specs.clava.ast.type.AttributedType;
import pt.up.fe.specs.clava.ast.type.AutoType;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.ConstantArrayType;
import pt.up.fe.specs.clava.ast.type.DecayedType;
import pt.up.fe.specs.clava.ast.type.DecltypeType;
import pt.up.fe.specs.clava.ast.type.DummyType;
import pt.up.fe.specs.clava.ast.type.ElaboratedType;
import pt.up.fe.specs.clava.ast.type.EnumType;
import pt.up.fe.specs.clava.ast.type.FunctionNoProtoType;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.IncompleteArrayType;
import pt.up.fe.specs.clava.ast.type.LValueReferenceType;
import pt.up.fe.specs.clava.ast.type.LiteralType;
import pt.up.fe.specs.clava.ast.type.NullType;
import pt.up.fe.specs.clava.ast.type.ParenType;
import pt.up.fe.specs.clava.ast.type.PointerType;
import pt.up.fe.specs.clava.ast.type.QualType;
import pt.up.fe.specs.clava.ast.type.RValueReferenceType;
import pt.up.fe.specs.clava.ast.type.RecordType;
import pt.up.fe.specs.clava.ast.type.SubstTemplateTypeParmType;
import pt.up.fe.specs.clava.ast.type.TemplateSpecializationType;
import pt.up.fe.specs.clava.ast.type.TemplateTypeParmType;
import pt.up.fe.specs.clava.ast.type.TemplateTypeParmType.TemplateParmData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.TypeWithKeyword.ElaboratedTypeKeyword;
import pt.up.fe.specs.clava.ast.type.TypedefType;
import pt.up.fe.specs.clava.ast.type.UnaryTransformType;
import pt.up.fe.specs.clava.ast.type.VariableArrayType;
import pt.up.fe.specs.clava.ast.type.data.ArrayTypeData;
import pt.up.fe.specs.clava.ast.type.data.FunctionProtoTypeData;
import pt.up.fe.specs.clava.ast.type.data.FunctionTypeData;
import pt.up.fe.specs.clava.ast.type.data.Qualifier;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.data.UnaryTransformTypeKind;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;
import pt.up.fe.specs.clava.language.AccessSpecifier;
import pt.up.fe.specs.clava.language.CXXCtorInitializerKind;
import pt.up.fe.specs.clava.language.CastKind;
import pt.up.fe.specs.clava.language.TLSKind;
import pt.up.fe.specs.clava.language.TagKind;
import pt.up.fe.specs.clava.language.TemplateTypeParmKind;
import pt.up.fe.specs.clava.language.UnaryExprOrTypeTrait;
import pt.up.fe.specs.clava.omp.OMPDirective;

/**
 * Contains methods to create Clava nodes.
 *
 * @author JoaoBispo
 *
 */
public class ClavaNodeFactory {

    /*
     * 'extra' nodes
     */

    public static Undefined undefined(String name, String content, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {

        return new Undefined(name, content, info, children);
    }

    public static App app(Collection<TranslationUnit> translationUnits) {
        return new App(translationUnits);
    }

    public static TranslationUnit translationUnit(String filename, String path, Collection<Decl> declarations) {
        return new TranslationUnit(filename, path, declarations);
    }

    public static IncludeDecl include(String include, boolean isAngled) {
        return new IncludeDecl(include, isAngled);
    }

    public static IncludeDecl include(Include include, String filepath) {
        return new IncludeDecl(include, ClavaNodeInfo.undefinedInfo(filepath));
    }

    public static NullNode nullNode(ClavaNodeInfo info) {
        return new NullNode(info);
    }

    public static OriginalNamespace originalNamespace(String namespace, ClavaNodeInfo nodeInfo) {
        return new OriginalNamespace(namespace, nodeInfo);
    }

    public static CXXCtorInitializer cxxCtorInitializer(BareDeclData anyMemberData, Type initType,
            CXXCtorInitializerKind kind, ClavaNodeInfo nodeInfo, Expr defaultInit) {

        return new CXXCtorInitializer(anyMemberData, initType, kind, nodeInfo, defaultInit);
    }

    public static TemplateArgumentType templateArgumentType(List<String> type, ClavaNodeInfo nodeInfo) {
        return new TemplateArgumentType(type, nodeInfo);
    }

    public static TemplateArgumentExpr templateArgumentExpr(Expr expr, ClavaNodeInfo nodeInfo) {
        return new TemplateArgumentExpr(expr, nodeInfo);
    }

    /*
     * 'comment' nodes
     */

    public static DummyComment dummyComment(String content, ClavaNodeInfo info, List<? extends Comment> children) {
        return new DummyComment(content, info, children);
    }

    public static TextComment textComment(String text, ClavaNodeInfo nodeInfo) {
        return new TextComment(text, nodeInfo);
    }

    public static ParagraphComment paragraphComment(ClavaNodeInfo nodeInfo, List<InlineContentComment> text) {
        return new ParagraphComment(nodeInfo, text);
    }

    public static FullComment fullComment(ClavaNodeInfo nodeInfo, List<BlockContentComment> block) {
        return new FullComment(nodeInfo, block);
    }

    public static InlineComment inlineComment(String text, boolean isStmtComment, ClavaNodeInfo nodeInfo) {
        return new InlineComment(text, isStmtComment, nodeInfo);
    }

    public static MultiLineComment multiLineComment(List<String> lines, ClavaNodeInfo nodeInfo) {
        return new MultiLineComment(lines, nodeInfo);
    }

    public static InlineCommandComment inlineCommandComment(String text, ClavaNodeInfo nodeInfo) {
        return new InlineCommandComment(text, nodeInfo);
    }

    /*
     * 'pragma' nodes
     */
    public static GenericPragma genericPragmaStmt(List<String> content, ClavaNodeInfo info) {
        return new GenericPragma(content, info);
    }

    /*
     * 'decl' nodes
     */

    public static LiteralDecl literalDecl(String code) {
        return new LiteralDecl(code);
    }

    // public static LiteralDecl literalDecl(String code, ClavaNodeInfo info) {
    // return new LiteralDecl(code, info);
    // }

    public static DummyDecl dummyDecl(String content, ClavaNodeInfo info, List<? extends ClavaNode> children) {
        return new DummyDecl(content, info, children);
    }

    public static DummyDecl dummyDecl(ClavaNode node) {
        return new DummyDecl(node.toContentString(), node.getInfo(), node.getChildren());
    }

    public static LinkageSpecDecl linkageSpecialDecl(LanguageId linkageType, DeclData declData, ClavaNodeInfo info,
            List<ClavaNode> declarations) {

        return new LinkageSpecDecl(linkageType, declData, info, declarations);
    }

    public static TypedefDecl typedefDecl(String typedefSource, boolean isModulePrivate, String name, Type type,
            DeclData declData, ClavaNodeInfo info, List<? extends ClavaNode> children) {
        return new TypedefDecl(typedefSource, isModulePrivate, name, type, declData, info, children);
    }

    public static VarDecl varDecl(String varName, Type type) {
        boolean isUsed = true;
        boolean isImplicit = false;
        boolean isNrvo = false;

        VarDeclData varDeclData = new VarDeclData(StorageClass.NONE, TLSKind.NONE, false, isNrvo,
                InitializationStyle.NO_INIT);
        DeclData declData = new DeclData(false, isImplicit, isUsed, false, false, false);
        return ClavaNodeFactory.varDecl(varDeclData, varName, type, declData, null, null);
    }

    /*
    public static VarDecl varDecl(String varName, Expr initExpr) {
    
        boolean isUsed = true;
        boolean isImplicit = false;
        boolean isNrvo = false;
    
        Type initType = (Type) init.getTypeImpl().getNode();
    
        DataStore config = CxxWeaver.getCxxWeaver().getConfig();
    
        // Check if C or C++
        Standard standard = config.get(ClavaOptions.STANDARD);
    
        Type type = getVarDeclType(standard, initType);
    
        VarDeclData varDeclData = new VarDeclData(StorageClass.NONE, TLSKind.NONE, false, isNrvo,
                InitializationStyle.CINIT);
        DeclData declData = new DeclData(false, isImplicit, isUsed, false, false, false);
        VarDecl varDecl = ClavaNodeFactory.varDecl(varDeclData, varName, type, declData, initExpr.getInfo(), initExpr);
    }*/

    public static VarDecl varDecl(VarDeclData data, String varName, Type type, DeclData declData, ClavaNodeInfo info,
            Expr initExpr) {

        return new VarDecl(data, varName, type, declData, info, initExpr);
    }

    public static EnumDecl enumDecl(EnumScopeType enumScopeType, String name, boolean isModulePrivate, Type integerType,
            EnumType type, DeclData declData, ClavaNodeInfo info, Collection<? extends EnumConstantDecl> children) {
        return new EnumDecl(enumScopeType, name, isModulePrivate, integerType, type, declData, info, children);
    }

    public static EnumConstantDecl enumConstantDecl(String value, Type type, DeclData declData, ClavaNodeInfo info) {
        return new EnumConstantDecl(value, type, declData, info);
    }

    public static EnumConstantDecl enumConstantDecl(String value, Type type, DeclData declData, ClavaNodeInfo info,
            Expr init) {
        return new EnumConstantDecl(value, type, declData, info, init);
    }

    public static NamespaceDecl namespaceDecl(boolean isInline, BareDeclData originalNamespace,
            String namespace, DeclData declData, ClavaNodeInfo info, List<? extends ClavaNode> namespaceDecls) {

        return new NamespaceDecl(isInline, originalNamespace, namespace, declData, info, namespaceDecls);
    }

    /**
     * Constructor for a function declaration (without a definition).
     *
     * @param functionName
     * @param inputs
     * @param returnType
     * @param attributes
     * @param info
     * @return
     */
    // public static FunctionDecl functionDecl(String functionName, List<ParmVarDecl> inputs,
    // Type functionType, FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info) {
    // return new FunctionDecl(functionName, inputs, functionType, functionDeclData, declData, info);
    // }

    /**
     * Constructor for a function definition.
     *
     * TODO: Replace List<Stmt> definition with a single Stmt and check if CompoundStmt or TryStmt
     *
     * @param functionName
     * @param inputs
     * @param returnType
     * @param attributes
     * @param info
     * @param definition
     * @return
     */
    public static FunctionDecl functionDecl(String functionName, List<ParmVarDecl> inputs,
            Type functionType, FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            Stmt definition) {

        return new FunctionDecl(functionName, inputs, functionType, functionDeclData, declData, info, definition);
    }

    public static FunctionDecl dummyFunctionDecl(String functionName) {
        return new FunctionDecl(functionName, Collections.emptyList(),
                dummyType("dummy function type", ClavaNodeInfo.undefinedInfo(), Collections.emptyList()),
                new FunctionDeclData(), new DeclData(), ClavaNodeInfo.undefinedInfo(),
                null);
    }

    public static ParmVarDecl parmVarDecl(String varName, Type type) {
        return parmVarDecl(false, new VarDeclData(), varName, type, new DeclData(), null, null);
    }

    public static ParmVarDecl parmVarDecl(boolean hasInheritedDefaultArg, VarDeclData data, String varName, Type type,
            DeclData declData, ClavaNodeInfo info, Expr initExpr) {

        return new ParmVarDecl(hasInheritedDefaultArg, data, varName, type, declData, info, initExpr);
    }

    public static RecordDecl recordDecl(RecordDeclData recordDeclData, Type type, DeclData declData, ClavaNodeInfo info,
            List<? extends Decl> decls) {
        return new RecordDecl(recordDeclData, type, declData, info, decls);
    }

    public static FieldDecl fieldDecl(boolean isMutable, boolean isModulePrivate, String declName, Type type,
            DeclData declData,
            ClavaNodeInfo info, Expr bitwidth, Expr inClassInitializer) {

        return new FieldDecl(isMutable, isModulePrivate, declName, type, declData, info, bitwidth, inClassInitializer);
    }

    public static CXXRecordDecl cxxRecordDecl(List<RecordBase> recordBases, RecordDeclData recordDeclData, Type type,
            DeclData declData,
            ClavaNodeInfo info, List<? extends Decl> children) {

        return new CXXRecordDecl(recordBases, recordDeclData, type, declData, info, children);
    }

    // public static CXXConstructorDecl cxxConstructorDecl(CXXMethodDeclData methodData,
    // List<CXXCtorInitializer> defaultInits, String declName, List<ParmVarDecl> inputs,
    // Type functionType, FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info) {
    //
    // return new CXXConstructorDecl(methodData, defaultInits, declName, inputs, functionType, functionDeclData,
    // declData, info);
    // }

    public static CXXConstructorDecl cxxConstructorDecl(CXXMethodDeclData methodData,
            List<CXXCtorInitializer> defaultInits, String declName, List<ParmVarDecl> inputs,
            Type functionType, FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            Stmt definition) {

        return new CXXConstructorDecl(methodData, defaultInits, declName, inputs, functionType, functionDeclData,
                declData, info, definition);
    }

    public static CXXMethodDecl cxxMethodDecl(CXXMethodDeclData methodData, String declName,
            Type functionType, FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            List<ParmVarDecl> inputs, Stmt definition) {

        return new CXXMethodDecl(methodData, declName, functionType, functionDeclData, declData, info, inputs,
                definition);
    }

    public static AccessSpecDecl accessSpecDecl(AccessSpecifier accessSpecifier, DeclData declData,
            ClavaNodeInfo info) {
        return new AccessSpecDecl(accessSpecifier, declData, info);
    }

    public static CXXDestructorDecl cxxDestructorDecl(CXXMethodDeclData methodData, String declName,
            Type functionType, FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            List<ParmVarDecl> inputs, Stmt definition) {

        return new CXXDestructorDecl(methodData, declName, functionType, functionDeclData, declData, info, inputs,
                definition);
    }

    public static FriendDecl friendDecl(DeclData declData, ClavaNodeInfo info, ClavaNode friendNode) {
        return new FriendDecl(declData, info, friendNode);
    }

    public static TypeAliasDecl typeAliasDecl(String declName, Type type, DeclData declData, ClavaNodeInfo info,
            Type aliasedType) {

        return new TypeAliasDecl(declName, type, declData, info, aliasedType);
    }

    public static UsingDirectiveDecl usingDirectiveDecl(String declName, DeclData declData, ClavaNodeInfo info) {
        return new UsingDirectiveDecl(declName, declData, info);
    }

    public static UsingDecl usingDecl(NestedNamedSpecifier qualifier, String declName, DeclData declData,
            ClavaNodeInfo info) {
        return new UsingDecl(qualifier, declName, declData, info);
    }

    public static TemplateTypeParmDecl templateTypeParmDecl(TemplateTypeParmKind kind, boolean isParameterPack,
            String name, DeclData declData, ClavaNodeInfo info, TemplateArgument defaultArgument) {

        return new TemplateTypeParmDecl(kind, isParameterPack, name, declData, info, defaultArgument);
    }

    public static FunctionTemplateDecl functionTemplateDecl(String declName, List<Decl> specializations,
            DeclData declData, ClavaNodeInfo info, List<TemplateTypeParmDecl> templateParameters, Decl templateDecl) {

        return new FunctionTemplateDecl(declName, specializations, declData, info, templateParameters, templateDecl);
    }

    public static NullDecl nullDecl(ClavaNodeInfo info) {
        return new NullDecl(info);
    }

    public static NamespaceAliasDecl namespaceAliasDecl(String nestedPrefix, DeclRef declInfo, String declName,
            DeclData declData,
            ClavaNodeInfo info) {

        return new NamespaceAliasDecl(nestedPrefix, declInfo, declName, declData, info);
    }

    /*
     * 'type' nodes
     */

    public static DummyType dummyType(String content, ClavaNodeInfo info, List<? extends ClavaNode> children) {
        return new DummyType(content, info, children);
    }

    public static DummyType dummyType(ClavaNode node) {
        return new DummyType(node.toContentString(), node.getInfo(), node.getChildren());
    }

    // public static BuiltinTypeOld builtinType(List<Type> type, ClavaNodeInfo info) {
    // return new BuiltinTypeOld(type, info);
    // }

    public static BuiltinType builtinType(String type) {
        return ClavaNodeFactory.builtinType(new TypeData(type.toLowerCase()),
                ClavaNodeInfo.undefinedInfo());
    }

    public static BuiltinType builtinType(TypeData typeData, ClavaNodeInfo info) {
        return new BuiltinType(typeData, info);
    }

    public static TypedefType typedefType(DeclRef declInfo, TypeData typeData, ClavaNodeInfo info,
            Type classType) {
        return new TypedefType(declInfo, typeData, info, classType);
    }

    public static QualType qualType(List<Qualifier> qualifiers, TypeData typeData, ClavaNodeInfo info,
            Type qualifiedType) {

        return new QualType(qualifiers, typeData, info, qualifiedType);
    }

    public static PointerType pointerType(TypeData typeData, ClavaNodeInfo info, Type pointeeType) {
        return new PointerType(typeData, info, pointeeType);
    }

    public static EnumType enumType(DeclRef declInfo, TypeData typeData, ClavaNodeInfo info) {
        return new EnumType(declInfo, typeData, info);
    }

    public static FunctionProtoType functionProtoType(FunctionProtoTypeData functionProtoTypeData,
            FunctionTypeData functionTypeData, TypeData type, ClavaNodeInfo info, Type returnType,
            Collection<? extends Type> arguments) {

        return new FunctionProtoType(functionProtoTypeData, functionTypeData, type, info, returnType, arguments);
    }

    public static FunctionNoProtoType functionNoProtoType(FunctionTypeData functionTypeData, TypeData typeData,
            ClavaNodeInfo info, Type returnType) {
        return new FunctionNoProtoType(functionTypeData, typeData, info, returnType);
    }

    public static NullType nullType(ClavaNodeInfo info) {
        return new NullType(info);
    }

    public static RecordType recordType(String recordName, DeclRef declInfo, TagKind tagKind,
            TypeData typeData, ClavaNodeInfo info) {
        return new RecordType(recordName, declInfo, tagKind, typeData, info);
    }

    public static LValueReferenceType lValueReferenceType(TypeData typeData, ClavaNodeInfo info, Type referencee) {
        return new LValueReferenceType(typeData, info, referencee);
    }

    public static RValueReferenceType rValueReferenceType(TypeData typeData, ClavaNodeInfo info, Type referencee) {
        return new RValueReferenceType(typeData, info, referencee);
    }

    public static ElaboratedType elaboratedType(ElaboratedTypeKeyword keyword, TypeData typeData,
            ClavaNodeInfo info, Type namedType) {
        return new ElaboratedType(keyword, typeData, info, namedType);
    }

    public static TemplateSpecializationType templateSpecializationType(String templateName,
            List<String> templateArgsNames, TypeData typeData, ClavaNodeInfo info,
            List<TemplateArgument> templateArgs, Type aliasedType, Type desugaredType) {

        return new TemplateSpecializationType(templateName, templateArgsNames, typeData, info,
                templateArgs, aliasedType, desugaredType);
    }

    public static ParenType parenType(TypeData typeData, ClavaNodeInfo info, Type innerType) {
        return new ParenType(typeData, info, innerType);
    }

    public static TemplateTypeParmType templateTypeParmType(TemplateParmData templateParmData, DeclRef declInfo,
            TypeData typeData, ClavaNodeInfo info) {

        return new TemplateTypeParmType(templateParmData, declInfo, typeData, info);
    }

    public static DecayedType decayedType(TypeData typeData, ClavaNodeInfo info, Type originalType, Type adjustedType) {
        return new DecayedType(typeData, info, originalType, adjustedType);
    }

    public static ConstantArrayType constantArrayType(int constant, ArrayTypeData arrayTypeData, TypeData typeData,
            ClavaNodeInfo info,
            Type elementType) {
        return new ConstantArrayType(constant, arrayTypeData, typeData, info, elementType);
    }

    public static IncompleteArrayType incompleteArrayType(ArrayTypeData arrayTypeData, TypeData typeData,
            ClavaNodeInfo info, Type elementType) {
        return new IncompleteArrayType(arrayTypeData, typeData, info, elementType);
    }

    public static AttributedType attributedType(TypeData typeData, ClavaNodeInfo info, Type modifiedType,
            Type equivalentType) {
        return new AttributedType(typeData, info, modifiedType, equivalentType);
    }

    public static AutoType autoType(TypeData typeData, ClavaNodeInfo info, Type deducedType) {
        return new AutoType(typeData, info, deducedType);
    }

    public static DecltypeType decltypeType(TypeData typeData, ClavaNodeInfo info, Expr expr,
            Type underlyingType) {
        return new DecltypeType(typeData, info, expr, underlyingType);
    }

    public static VariadicType variadicType(ClavaNodeInfo info) {
        return new VariadicType(info);
    }

    public static UnaryTransformType unaryTransformType(UnaryTransformTypeKind kind, TypeData data, ClavaNodeInfo info,
            Type baseType, Type underlyingType) {

        return new UnaryTransformType(kind, data, info, baseType, underlyingType);
    }

    public static VariableArrayType variableArrayType(ArrayTypeData arrayTypeData, TypeData typeData,
            ClavaNodeInfo info, Type elementType,
            Expr sizeExpr) {
        return new VariableArrayType(arrayTypeData, typeData, info, elementType, sizeExpr);
    }

    /*
    * 'stmt' nodes
    */

    public static DummyStmt dummyStmt(String content, ClavaNodeInfo info, List<? extends ClavaNode> children) {
        return new DummyStmt(content, info, children);
    }

    public static LiteralType literalType(String literalType) {
        return new LiteralType(literalType);
    }

    public static SubstTemplateTypeParmType substTemplateTypeParmType(TypeData typeData, ClavaNodeInfo info,
            TemplateTypeParmType replaceParameter, Type replacementType) {
        return new SubstTemplateTypeParmType(typeData, info, replaceParameter, replacementType);
    }

    /**
     * Helper method that receives a ClavaNode.
     *
     * @param node
     * @return
     */
    public static DummyStmt dummyStmt(ClavaNode node) {
        return new DummyStmt(node.toContentString(), node.getInfo(), node.getChildren());
    }

    public static CompoundStmt compoundStmt(ClavaNodeInfo info, Collection<? extends Stmt> children) {
        return new CompoundStmt(info, children);
    }

    public static SwitchStmt switchStmt(ClavaNodeInfo info, Expr cond, Stmt body) {
        return new SwitchStmt(info, cond, body);
    }

    public static CaseStmt caseStmt(ClavaNodeInfo info, Expr lhs, Stmt subStmt) {
        return new CaseStmt(info, lhs, subStmt);
    }

    public static CaseStmt caseStmt(ClavaNodeInfo info, Expr lhs, Expr rhs, Stmt subStmt) {
        return new CaseStmt(info, lhs, rhs, subStmt);
    }

    public static DefaultStmt defaultStmt(ClavaNodeInfo info, Stmt subStmt) {
        return new DefaultStmt(info, subStmt);
    }

    public static ReturnStmt returnStmt(ClavaNodeInfo info, Expr retValue) {
        return new ReturnStmt(info, retValue);
    }

    public static ReturnStmt returnStmt(ClavaNodeInfo info) {
        return new ReturnStmt(info);
    }

    public static DeclStmt declStmt(ClavaNodeInfo info, List<NamedDecl> decls) {
        return new DeclStmt(info, decls);
    }

    public static DeclStmt declStmt(ClavaNodeInfo info, RecordDecl recordDecl, List<VarDecl> varDecls) {
        return new DeclStmt(info, recordDecl, varDecls);
    }

    public static DeclStmt declStmtWithoutSemicolon(ClavaNodeInfo info, NamedDecl decl) {
        return new DeclStmt(false, info, decl);
    }

    public static ExprStmt exprStmt(Expr expr) {
        return new ExprStmt(expr.getInfo(), expr);
    }

    public static ExprStmt exprStmtWithoutSemicolon(Expr expr) {
        return new ExprStmt(false, expr.getInfo(), expr);
    }

    public static ExprStmt exprStmtAssign(Expr lhs, Expr rhs, Type returnType) {
        ExprData exprData = new ExprData(returnType);

        BinaryOperator op = binaryOperator(BinaryOperatorKind.ASSIGN, exprData, null, lhs, rhs);

        return exprStmt(op);
    }

    public static ContinueStmt continueStmt(ClavaNodeInfo info) {
        return new ContinueStmt(info);
    }

    public static BreakStmt breakStmt(ClavaNodeInfo info) {
        return new BreakStmt(info);
    }

    public static IfStmt ifStmt(ClavaNodeInfo info, ClavaNode condition, CompoundStmt thenStmt) {
        return new IfStmt(info, condition, thenStmt);
    }

    public static IfStmt ifStmt(ClavaNodeInfo info, ClavaNode condition, CompoundStmt thenStmt, CompoundStmt elseStmt) {
        return new IfStmt(info, condition, thenStmt, elseStmt);
    }

    public static WhileStmt whileStmt(ClavaNodeInfo info, ClavaNode condition, CompoundStmt thenStmt) {
        return new WhileStmt(info, condition, thenStmt);
    }

    public static ForStmt forStmt(ClavaNodeInfo info, Stmt init, Stmt cond, Stmt inc, CompoundStmt body) {
        return new ForStmt(info, init, cond, inc, body);
    }

    public static LiteralStmt literalStmt(String literalCode) {
        return new LiteralStmt(literalCode);
    }

    // public static LiteralStmt literalStmt(String literalCode, ClavaNodeInfo info) {
    // return new LiteralStmt(literalCode, info);
    // }

    public static CXXForRangeStmt cxxForRangeStmt(ClavaNodeInfo info, DeclStmt range, DeclStmt beginEnd, Expr cond,
            Expr inc, DeclStmt loopVar, Stmt body) {
        return new CXXForRangeStmt(info, range, beginEnd, cond, inc, loopVar, body);
    }

    public static CapturedStmt capturedStmt(ClavaNodeInfo info, Collection<? extends Stmt> children) {
        return new CapturedStmt(info, children);
    }

    public static ClangLabelStmt clangLabelStmt(String label, ClavaNodeInfo info, Stmt subStmt) {
        return new ClangLabelStmt(label, info, subStmt);
    }

    public static LabelStmt labelStmt(String label, ClavaNodeInfo info) {
        return new LabelStmt(label, info);
    }

    public static WrapperStmt wrapperStmt(ClavaNodeInfo info, ClavaNode wrappedNode) {
        return new WrapperStmt(info, wrappedNode);
    }

    public static CXXCatchStmt cxxCatchStmt(ClavaNodeInfo info, Decl exception, CompoundStmt catchBody) {
        return new CXXCatchStmt(info, exception, catchBody);
    }

    public static CXXTryStmt cxxTryStmt(ClavaNodeInfo info, CompoundStmt tryBody, List<CXXCatchStmt> handlers) {
        return new CXXTryStmt(info, tryBody, handlers);
    }

    public static DoStmt doStmt(ClavaNodeInfo info, CompoundStmt body, Expr condition) {
        return new DoStmt(info, body, condition);
    }

    public static NullStmt nullStmt(ClavaNodeInfo info) {
        return new NullStmt(info);
    }

    /*
     * 'expr' nodes
     */

    public static LiteralExpr literalExpr(String literalCode, Type type) {
        return new LiteralExpr(literalCode, type);
    }

    // public static LiteralExpr literalExpr(String literalCode, Type type, ClavaNodeInfo info) {
    // return new LiteralExpr(literalCode, type, info);
    // }

    public static DummyExpr dummyExpr(ClavaNode node) {
        return new DummyExpr(node.toContentString(), node.getInfo(), node.getChildren());
    }

    public static DummyExpr dummyExpr(String content, ClavaNodeInfo info, List<? extends ClavaNode> children) {
        return new DummyExpr(content, info, children);
    }

    public static ImplicitCastExpr exprImplicitCast(CastKind castKind, ExprData exprData,
            ClavaNodeInfo info, Expr subExpr) {

        return new ImplicitCastExpr(castKind, exprData, info, subExpr);
    }

    public static ImplicitValueInitExpr implicitValueInitExpr(ExprData exprData, ClavaNodeInfo info) {
        return new ImplicitValueInitExpr(exprData, info);
    }

    public static IntegerLiteral integerLiteral(String literal, ExprData exprData, ClavaNodeInfo info) {

        return new IntegerLiteral(literal, exprData, info);
    }

    public static CXXFunctionalCastExpr cxxFunctionalCastExpr(String targetType, CastKind castKind,
            ExprData exprData, ClavaNodeInfo info, Expr subExpr) {

        return new CXXFunctionalCastExpr(targetType, castKind, exprData, info, subExpr);
    }

    /**
     *
     * @param type
     * @param opcode
     * @param isPrefix
     * @param valueKind
     *            can be null
     * @param info
     * @param subExpr
     * @return
     */
    public static UnaryOperator unaryOperator(UnaryOperatorKind opcode, UnaryOperatorPosition position,
            ExprData exprData, ClavaNodeInfo info, Expr subExpr) {

        return new UnaryOperator(opcode, position, exprData, info, subExpr);
    }

    public static DeclRefExpr declRefExpr(String refName, Type type) {
        return declRefExpr(refName, ValueKind.getDefault(), type, null);
    }

    public static DeclRefExpr declRefExpr(String refName, ValueKind valueKind, Type type, ClavaNodeInfo info) {
        ExprData exprData = new ExprData(type, valueKind);

        String qualifier = "";

        BareDeclData declData = BareDeclData.newInstance(refName);

        return declRefExpr(qualifier, Collections.emptyList(), declData, null, exprData, info);
    }

    /**
     *
     * @param hasTemplateArgs
     * @param type
     * @param valueKind
     *            can be null
     * @param refType
     * @param declAddress
     * @param refName
     * @param type2
     * @param info
     * @return
     */
    public static DeclRefExpr declRefExpr(String qualifier, List<String> templateArguments, BareDeclData declData,
            BareDeclData foundDeclData, ExprData exprData,
            ClavaNodeInfo info) {

        return new DeclRefExpr(qualifier, templateArguments, declData, foundDeclData, exprData, info);
    }

    public static ExprWithCleanups exprWithCleanups(ExprData exprData, ClavaNodeInfo info,
            Expr subExpr) {
        return new ExprWithCleanups(exprData, info, subExpr);
    }

    public static CXXConstructExpr cxxConstructExpr(CXXConstructExprData constructorData, ExprData exprData,
            ClavaNodeInfo info, Collection<? extends Expr> args) {

        return new CXXConstructExpr(constructorData, exprData, info, args);
    }

    public static MaterializeTemporaryExpr materializeTemporaryExpr(ExprData exprData, BareDeclData extendingDecl,
            ClavaNodeInfo info, Expr temporaryExpr) {

        return new MaterializeTemporaryExpr(exprData, extendingDecl, info, temporaryExpr);
    }

    public static CXXBindTemporaryExpr cXXBindTemporaryExpr(String temporaryAddress, ExprData exprData,
            ClavaNodeInfo info, Expr subExpr) {

        return new CXXBindTemporaryExpr(temporaryAddress, exprData, info, subExpr);
    }

    public static CXXTemporaryObjectExpr cxxTemporaryObjectExpr(CXXConstructExprData constructorData, ExprData exprData,
            ClavaNodeInfo info,
            Collection<? extends Expr> args) {

        return new CXXTemporaryObjectExpr(constructorData, exprData, info, args);
    }

    public static CXXDefaultArgExpr cxxDefaultArgExpr(ExprData exprData, ClavaNodeInfo info) {
        return new CXXDefaultArgExpr(exprData, info);
    }

    public static StringLiteral stringLiteral(String string, ExprData exprData, ClavaNodeInfo info) {
        return new StringLiteral(string, exprData, info);
    }

    public static CallExpr callExpr(Expr function, Type type, List<? extends Expr> args) {
        ExprData exprData = new ExprData(type);

        return callExpr(exprData, null, function, args);
    }

    public static CallExpr callExpr(ExprData exprData, ClavaNodeInfo info, Expr function, List<? extends Expr> args) {

        return new CallExpr(exprData, info, function, args);
    }

    public static CXXMemberCallExpr cxxMemberCallExpr(ExprData exprData, ClavaNodeInfo info, MemberExpr function,
            List<? extends Expr> args) {

        return new CXXMemberCallExpr(exprData, info, function, args);
    }

    public static MemberExpr memberExpr(String memberName, boolean isArrow,
            ExprData exprData, ClavaNodeInfo info, Expr base) {

        return new MemberExpr(memberName, isArrow, exprData, info, base);
    }

    public static FloatingLiteral floatingLiteral(FloatKind floatKind, String number, ExprData exprData,
            ClavaNodeInfo info) {

        return new FloatingLiteral(floatKind, number, exprData, info);
    }

    public static BinaryOperator binaryOperator(BinaryOperatorKind op, ExprData exprData, ClavaNodeInfo info, Expr lhs,
            Expr rhs) {

        return new BinaryOperator(op, exprData, info, lhs, rhs);
    }

    public static ParenExpr parenExpr(ExprData exprData, ClavaNodeInfo info, Expr subExpr) {
        return new ParenExpr(exprData, info, subExpr);
    }

    public static ArraySubscriptExpr arraySubscriptExpr(ExprData exprData, ClavaNodeInfo info,
            Expr lhs, Expr rhs) {
        return new ArraySubscriptExpr(exprData, info, lhs, rhs);
    }

    public static CStyleCastExpr cStyleCastExpr(CastKind castKind, ExprData exprData, ClavaNodeInfo info,
            Expr subExpr) {
        return new CStyleCastExpr(castKind, exprData, info, subExpr);
    }

    public static PredefinedExpr predefinedExpr(PredefinedIdType id, ExprData exprData, ClavaNodeInfo info,
            Expr subExpr) {

        return new PredefinedExpr(id, exprData, info, subExpr);
    }

    public static CXXBoolLiteralExpr cxxBoolLiteralExpr(boolean value, ExprData exprData, ClavaNodeInfo info) {
        return new CXXBoolLiteralExpr(value, exprData, info);
    }

    public static CXXNewExpr cxxNewExpr(boolean isGlobal, boolean isArray, BareDeclData newOperator, ExprData exprData,
            ClavaNodeInfo info, Expr arraySize, Expr constructorExpr, DeclRefExpr nothrow) {

        return new CXXNewExpr(isGlobal, isArray, newOperator, exprData, info, arraySize, constructorExpr, nothrow);
    }

    public static ConditionalOperator conditionalOperator(ExprData exprData, ClavaNodeInfo info,
            Expr condition, Expr trueExpr, Expr falseExpr) {

        return new ConditionalOperator(exprData, info, condition, trueExpr, falseExpr);
    }

    public static CXXOperatorCallExpr cxxOperatorCallExpr(ExprData exprData, ClavaNodeInfo info, Expr function,
            List<? extends Expr> args) {

        return new CXXOperatorCallExpr(exprData, info, function, args);
    }

    public static CharacterLiteral characterLiteral(long charValue, ExprData exprData, ClavaNodeInfo info) {
        return new CharacterLiteral(charValue, exprData, info);
    }

    public static CXXDefaultInitExpr cxxDefaultInitExpr(ExprData exprData, ClavaNodeInfo info) {
        return new CXXDefaultInitExpr(exprData, info);
    }

    public static CXXNullPtrLiteralExpr cxxNullPtrLiteralExpr(ExprData exprData, ClavaNodeInfo info) {
        return new CXXNullPtrLiteralExpr(exprData, info);
    }

    public static CXXThisExpr cxxThisExpr(ExprData exprData, ClavaNodeInfo info) {
        return new CXXThisExpr(exprData, info);
    }

    public static UnaryExprOrTypeTraitExpr unaryExprOrTypeTraitExpr(UnaryExprOrTypeTrait uettKind, Type argType,
            ExprData exprData, ClavaNodeInfo info, Expr argumentExpression) {

        return new UnaryExprOrTypeTraitExpr(uettKind, argType, exprData, info, argumentExpression);
    }

    public static CompoundAssignOperator compoundAssignOperator(Type lhsType, Type resultType,
            BinaryOperatorKind op, ExprData exprData, ClavaNodeInfo info, Expr lhs, Expr rhs) {
        return new CompoundAssignOperator(lhsType, resultType, op, exprData, info, lhs, rhs);
    }

    public static CXXDeleteExpr cxxDeleteExpr(boolean isGlobal, boolean isArray, BareDeclData operatorDelete,
            ExprData exprData, ClavaNodeInfo info, Expr argument) {
        return new CXXDeleteExpr(isGlobal, isArray, operatorDelete, exprData, info, argument);
    }

    public static InitListExpr initListExpr(boolean hasInitializedFieldInUnion, Expr arrayFiller,
            BareDeclData fieldData,
            ExprData exprData, ClavaNodeInfo info, Collection<? extends Expr> initExprs) {

        return new InitListExpr(hasInitializedFieldInUnion, arrayFiller, fieldData, exprData, info, initExprs);
    }

    public static CXXStdInitializerListExpr cxxStdInitializerListExpr(ExprData exprData, ClavaNodeInfo info,
            Expr subExpr) {
        return new CXXStdInitializerListExpr(exprData, info, subExpr);
    }

    public static UnresolvedLookupExpr unresolvedLookupExpr(boolean requiresAdl, String name, List<String> decls,
            ExprData exprData,
            ClavaNodeInfo info) {
        return new UnresolvedLookupExpr(requiresAdl, name, decls, exprData, info);
    }

    public static CXXStaticCastExpr cxxStaticCastExpr(CXXNamedCastExprData cxxNamedCastExprdata, ExprData exprData,
            ClavaNodeInfo info, Expr subExpr) {

        return new CXXStaticCastExpr(cxxNamedCastExprdata, exprData, info, subExpr);
    }

    public static CXXConstCastExpr cxxConstCastExpr(CXXNamedCastExprData cxxNamedCastExprdata, ExprData exprData,
            ClavaNodeInfo info,
            Expr subExpr) {

        return new CXXConstCastExpr(cxxNamedCastExprdata, exprData, info, subExpr);
    }

    public static CXXReinterpretCastExpr cxxReinterpretCastExpr(CXXNamedCastExprData cxxNamedCastExprdata,
            ExprData exprData, ClavaNodeInfo info, Expr subExpr) {

        return new CXXReinterpretCastExpr(cxxNamedCastExprdata, exprData, info, subExpr);
    }

    public static OffsetOfExpr offsetOfExpr(OffsetOfData offsetOfData, ExprData exprData, ClavaNodeInfo info) {
        return new OffsetOfExpr(offsetOfData, exprData, info);
    }

    public static UserDefinedLiteral userDefinedLiteral(ExprData exprData, ClavaNodeInfo info, Expr callee,
            Expr cookedLiteral) {

        return new UserDefinedLiteral(exprData, info, callee, cookedLiteral);
    }

    public static CXXThrowExpr cxxThrowExpr(ExprData exprData, ClavaNodeInfo info, Expr throwExpr) {
        return new CXXThrowExpr(exprData, info, throwExpr);
    }

    public static GNUNullExpr gnuNullExpr(ExprData exprData, ClavaNodeInfo info) {
        return new GNUNullExpr(exprData, info);
    }

    public static NullExpr nullExpr() {
        return new NullExpr();
    }

    /*
     * OpenMP Nodes
     */
    public static OMPParallelDirective ompParallelDirective(OMPDirective directive, ClavaNodeInfo info,
            Stmt associatedStmt) {

        return new OMPParallelDirective(directive, info, associatedStmt);
    }

    // public static OmpPragma ompPragma(OmpDirectiveKind directiveKind, Map<OmpClauseKind, OmpClause> clauses,
    // ClavaNodeInfo info) {
    // return new OmpPragma(directiveKind, clauses, info);
    // }

    /*
     * Attribute nodes
     */
    public static FinalAttr finalAttr(AttrData attrData, ClavaNodeInfo nodeInfo) {
        return new FinalAttr(attrData, nodeInfo);
    }

    public static CompoundStmt compoundStmt(ClavaNodeInfo info, String code) {
        return compoundStmt(info, Arrays.asList(literalStmt(code)));
    }

}