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

package pt.up.fe.specs.clang.clavaparser;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode;
import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode.ClangRootData;
import pt.up.fe.specs.clang.clava.parser.DelayedParsing;
import pt.up.fe.specs.clang.clavaparser.comment.FullCommentParser;
import pt.up.fe.specs.clang.clavaparser.comment.InlineCommandCommentParser;
import pt.up.fe.specs.clang.clavaparser.comment.ParagraphCommentParser;
import pt.up.fe.specs.clang.clavaparser.comment.TextCommentParser;
import pt.up.fe.specs.clang.clavaparser.decl.AccessSpecDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.CXXConstructorDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.CXXDestructorDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.CXXMethodDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.CXXRecordDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.EnumConstantDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.EnumDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.FieldDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.FriendDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.FunctionDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.LinkageSpecDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.NamespaceAliasDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.NamespaceDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.ParmVarDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.RecordDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.TemplateDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.TemplateTypeParmDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.TypeAliasDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.TypedefDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.UsingDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.UsingDirectiveDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.VarDeclParser;
import pt.up.fe.specs.clang.clavaparser.expr.ArraySubscriptExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.BinaryOperatorParser;
import pt.up.fe.specs.clang.clavaparser.expr.BreakStmtParser;
import pt.up.fe.specs.clang.clavaparser.expr.CStyleCastExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXBindTemporaryExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXBoolLiteralExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXConstructExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXDefaultArgExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXDefaultInitExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXDeleteExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXFunctionalCastExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXMemberCallExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXNamedCastExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXNewExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXNullPtrLiteralExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXOperatorCallExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXStdInitializerListExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXTemporaryObjectExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXThisExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CXXThrowExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CallExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.CharacterLiteralParser;
import pt.up.fe.specs.clang.clavaparser.expr.CompoundAssignOperatorParser;
import pt.up.fe.specs.clang.clavaparser.expr.ConditionalOperatorParser;
import pt.up.fe.specs.clang.clavaparser.expr.DeclRefExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.ExprWithCleanupsParser;
import pt.up.fe.specs.clang.clavaparser.expr.FloatingLiteralParser;
import pt.up.fe.specs.clang.clavaparser.expr.GNUNullExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.ImplicitCastExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.InitListExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.IntegerLiteralParser;
import pt.up.fe.specs.clang.clavaparser.expr.MaterializeTemporaryExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.MemberExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.OffsetOfExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.ParenExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.StringLiteralParser;
import pt.up.fe.specs.clang.clavaparser.expr.UnaryExprOrTypeTraitExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.UnaryOperatorParser;
import pt.up.fe.specs.clang.clavaparser.expr.UnresolvedLookupExprParser;
import pt.up.fe.specs.clang.clavaparser.expr.UserDefinedLiteralParser;
import pt.up.fe.specs.clang.clavaparser.extra.AlwaysInlineAttrParser;
import pt.up.fe.specs.clang.clavaparser.extra.ClangTypesParser;
import pt.up.fe.specs.clang.clavaparser.extra.NullNodeParser;
import pt.up.fe.specs.clang.clavaparser.extra.RootParser;
import pt.up.fe.specs.clang.clavaparser.extra.TemplateArgumentParser;
import pt.up.fe.specs.clang.clavaparser.omp.OMPParallelDirectiveParser;
import pt.up.fe.specs.clang.clavaparser.stmt.CXXCatchStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.CXXForRangeStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.CXXTryStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.CapturedStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.CaseStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.CompoundStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.ContinueStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.DeclStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.DefaultStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.DoStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.ForStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.IfStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.LabelStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.NullStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.ReturnStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.SwitchStmtParser;
import pt.up.fe.specs.clang.clavaparser.stmt.WhileStmtParser;
import pt.up.fe.specs.clang.clavaparser.type.AttributedTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.AutoTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.BuiltinTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.ConstantArrayTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.DecayedTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.DecltypeTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.ElaboratedTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.EnumTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.FunctionNoProtoTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.FunctionProtoTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.IncompleteArrayTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.LValueReferenceTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.ParenTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.PointerTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.QualTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.RValueReferenceTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.RecordTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.SubstTemplateTypeParmTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.TemplateSpecializationTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.TemplateTypeParmTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.TypedefTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.UnaryTransformTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.VariableArrayTypeParser;
import pt.up.fe.specs.clang.clavaparser.type.VariadicTypeParser;
import pt.up.fe.specs.clang.clavaparser.utils.ClangAstProcessor;
import pt.up.fe.specs.clang.textparser.TextParser;
import pt.up.fe.specs.clang.transforms.AdaptBoolCasts;
import pt.up.fe.specs.clang.transforms.AdaptBoolTypes;
import pt.up.fe.specs.clang.transforms.CreateDeclStmts;
import pt.up.fe.specs.clang.transforms.RecoverStdMacros;
import pt.up.fe.specs.clang.transforms.RemoveBoolOperatorCalls;
import pt.up.fe.specs.clang.transforms.RemoveClangComments;
import pt.up.fe.specs.clang.transforms.RemoveDefaultInitializers;
import pt.up.fe.specs.clang.transforms.RemoveExtraNodes;
import pt.up.fe.specs.clang.transforms.RemoveImplicitConstructors;
import pt.up.fe.specs.clang.transforms.ReplaceClangLabelStmt;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaRule;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.type.TemplateSpecializationType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Parses a ClangAst into ClavaNodes.
 *
 * @author JoaoBispo
 *
 */
public class ClavaParser implements AutoCloseable {

    private final static Collection<ClavaRule> POST_PARSING_RULES = Arrays.asList(
            new RemoveExtraNodes(),
            new RemoveClangComments(),
            new CreateDeclStmts(),
            new AdaptBoolTypes(),
            new AdaptBoolCasts(),
            new RemoveBoolOperatorCalls(),
            new ReplaceClangLabelStmt(),
            new RemoveDefaultInitializers(),
            new RemoveImplicitConstructors(),
            new RecoverStdMacros());
    // new CreateOmpPragmas());
    // new RemoveOutOfPlaceInits());

    private final ClangRootNode clangAst;
    private final ClangConverterTable converter;
    private final boolean sourceTree;

    public ClavaParser(ClangRootNode clangAst) {
        this(clangAst, true);
    }

    /**
     *
     * @param sourceTree
     *            if true, performs post-processing transformations that makes the tree resemble more the source code.
     */
    public ClavaParser(ClangRootNode clangAst, boolean sourceTree) {
        converter = buildConverter_3_8(clangAst.getClangRootData());
        this.sourceTree = sourceTree;
        this.clangAst = clangAst;
    }

    /**
     * Creates a ClangConverterTable for Clang 3.8
     *
     * @param clangRootData
     *
     * @return
     */
    private static ClangConverterTable buildConverter_3_8(ClangRootData clangRootData) {
        ClangConverterTable converter = new ClangConverterTable(clangRootData);

        /* extra */
        converter.put("Root", RootParser::new);
        converter.put("NULL", NullNodeParser::new);
        // converter.put("original Namespace", OriginalNamespaceParser::new);
        // converter.put("CXXCtorInitializer", CXXCtorInitializerParser::new);
        // converter.put("CXXCtorInitializer Field",
        // conv -> new CXXCtorInitializerParser(conv, CXXCtorInitializerType.FIELD));
        // converter.put("TemplateArgument", TemplateArgumentParser::new);
        converter.put("VariadicType", VariadicTypeParser::new);
        converter.put("AlwaysInlineAttr", AlwaysInlineAttrParser::new);
        converter.put("TemplateArgument", TemplateArgumentParser::new);

        /* decl */
        converter.put("LinkageSpecDecl", LinkageSpecDeclParser::new);
        converter.put("TypedefDecl", TypedefDeclParser::new);
        converter.put("VarDecl", VarDeclParser::new);
        converter.put("EnumDecl", EnumDeclParser::new);
        converter.put("EnumConstantDecl", EnumConstantDeclParser::new);
        converter.put("FunctionDecl", FunctionDeclParser::new);
        converter.put("ParmVarDecl", ParmVarDeclParser::new);
        converter.put("CXXRecordDecl", CXXRecordDeclParser::new);
        converter.put("RecordDecl", RecordDeclParser::new);
        converter.put("FieldDecl", FieldDeclParser::new);
        converter.put("NamespaceDecl", NamespaceDeclParser::new);
        converter.put("CXXConstructorDecl", CXXConstructorDeclParser::new);
        converter.put("CXXMethodDecl", CXXMethodDeclParser::new);
        converter.put("AccessSpecDecl", AccessSpecDeclParser::new);
        converter.put("CXXDestructorDecl", CXXDestructorDeclParser::new);
        converter.put("FriendDecl", FriendDeclParser::new);
        converter.put("TypeAliasDecl", TypeAliasDeclParser::new);
        converter.put("UsingDirectiveDecl", UsingDirectiveDeclParser::new);
        converter.put("UsingDecl", UsingDeclParser::new);
        converter.put("FunctionTemplateDecl", TemplateDeclParser::new);
        converter.put("TemplateTypeParmDecl", TemplateTypeParmDeclParser::new);
        converter.put("NamespaceAliasDecl", NamespaceAliasDeclParser::new);

        /* stmt */
        converter.put("CompoundStmt", CompoundStmtParser::new);
        converter.put("SwitchStmt", SwitchStmtParser::new);
        converter.put("CaseStmt", CaseStmtParser::new);
        converter.put("DefaultStmt", DefaultStmtParser::new);
        converter.put("ReturnStmt", ReturnStmtParser::new);
        converter.put("DeclStmt", DeclStmtParser::new);
        converter.put("ContinueStmt", ContinueStmtParser::new);
        converter.put("IfStmt", IfStmtParser::new);
        converter.put("WhileStmt", WhileStmtParser::new);
        converter.put("ForStmt", ForStmtParser::new);
        converter.put("CXXForRangeStmt", CXXForRangeStmtParser::new);
        converter.put("CapturedStmt", CapturedStmtParser::new);
        converter.put("LabelStmt", LabelStmtParser::new);
        converter.put("CXXTryStmt", CXXTryStmtParser::new);
        converter.put("CXXCatchStmt", CXXCatchStmtParser::new);
        converter.put("DoStmt", DoStmtParser::new);
        converter.put("NullStmt", NullStmtParser::new);

        /* OMP stmt */
        converter.put("OMPParallelDirective", OMPParallelDirectiveParser::new);

        /* expr */
        converter.put("ImplicitCastExpr", ImplicitCastExprParser::new);
        converter.put("IntegerLiteral", IntegerLiteralParser::new);
        converter.put("CXXFunctionalCastExpr", CXXFunctionalCastExprParser::new);
        converter.put("UnaryOperator", UnaryOperatorParser::new);
        converter.put("DeclRefExpr", DeclRefExprParser::new);
        converter.put("ExprWithCleanups", ExprWithCleanupsParser::new);
        converter.put("CXXConstructExpr", CXXConstructExprParser::new);
        converter.put("MaterializeTemporaryExpr", MaterializeTemporaryExprParser::new);
        converter.put("CXXBindTemporaryExpr", CXXBindTemporaryExprParser::new);
        converter.put("CXXTemporaryObjectExpr", CXXTemporaryObjectExprParser::new);
        converter.put("CXXDefaultArgExpr", CXXDefaultArgExprParser::new);
        converter.put("StringLiteral", StringLiteralParser::new);
        converter.put("CallExpr", CallExprParser::new);
        converter.put("CXXMemberCallExpr", CXXMemberCallExprParser::new);
        converter.put("MemberExpr", MemberExprParser::new);
        converter.put("FloatingLiteral", FloatingLiteralParser::new);
        converter.put("BinaryOperator", BinaryOperatorParser::new);
        converter.put("ParenExpr", ParenExprParser::new);
        converter.put("ArraySubscriptExpr", ArraySubscriptExprParser::new);
        converter.put("CStyleCastExpr", CStyleCastExprParser::new);
        converter.put("CXXBoolLiteralExpr", CXXBoolLiteralExprParser::new);
        converter.put("CXXNewExpr", CXXNewExprParser::new);
        converter.put("ConditionalOperator", ConditionalOperatorParser::new);
        converter.put("CXXOperatorCallExpr", CXXOperatorCallExprParser::new);
        converter.put("BreakStmt", BreakStmtParser::new);
        converter.put("CharacterLiteral", CharacterLiteralParser::new);
        converter.put("CXXDefaultInitExpr", CXXDefaultInitExprParser::new);
        converter.put("CXXNullPtrLiteralExpr", CXXNullPtrLiteralExprParser::new);
        converter.put("CXXThisExpr", CXXThisExprParser::new);
        converter.put("UnaryExprOrTypeTraitExpr", UnaryExprOrTypeTraitExprParser::new);
        converter.put("CompoundAssignOperator", CompoundAssignOperatorParser::new);
        converter.put("CXXDeleteExpr", CXXDeleteExprParser::new);
        converter.put("CXXStdInitializerListExpr", CXXStdInitializerListExprParser::new);
        converter.put("InitListExpr", InitListExprParser::new);
        converter.put("UnresolvedLookupExpr", UnresolvedLookupExprParser::new);
        converter.put("CXXStaticCastExpr", CXXNamedCastExprParser::new);
        converter.put("CXXReinterpretCastExpr", CXXNamedCastExprParser::new);
        converter.put("CXXConstCastExpr", CXXNamedCastExprParser::new);
        converter.put("OffsetOfExpr", OffsetOfExprParser::new);
        converter.put("UserDefinedLiteral", UserDefinedLiteralParser::new);
        converter.put("CXXThrowExpr", CXXThrowExprParser::new);
        converter.put("GNUNullExpr", GNUNullExprParser::new);

        /* type */
        converter.put("RecordType", RecordTypeParser::new);
        converter.put("FunctionProtoType", FunctionProtoTypeParser::new);
        converter.put("BuiltinType", BuiltinTypeParser::new);
        converter.put("LValueReferenceType", LValueReferenceTypeParser::new);
        converter.put("RValueReferenceType", RValueReferenceTypeParser::new);
        converter.put("QualType", QualTypeParser::new);
        converter.put("ElaboratedType", ElaboratedTypeParser::new);
        converter.put("TypedefType", TypedefTypeParser::new);
        converter.put("TemplateSpecializationType", TemplateSpecializationTypeParser::new);
        converter.put("ParenType", ParenTypeParser::new);
        converter.put("PointerType", PointerTypeParser::new);
        converter.put("SubstTemplateTypeParmType", SubstTemplateTypeParmTypeParser::new);
        converter.put("TemplateTypeParmType", TemplateTypeParmTypeParser::new);
        converter.put("DecayedType", DecayedTypeParser::new);
        converter.put("IncompleteArrayType", IncompleteArrayTypeParser::new);
        converter.put("ConstantArrayType", ConstantArrayTypeParser::new);
        converter.put("AttributedType", AttributedTypeParser::new);
        converter.put("AutoType", AutoTypeParser::new);
        converter.put("FunctionNoProtoType", FunctionNoProtoTypeParser::new);
        converter.put("DecltypeType", DecltypeTypeParser::new);
        converter.put("EnumType", EnumTypeParser::new);
        converter.put("UnaryTransformType", UnaryTransformTypeParser::new);
        converter.put("VariableArrayType", VariableArrayTypeParser::new);

        /* comment */
        converter.put("ParagraphComment", ParagraphCommentParser::new);
        converter.put("TextComment", TextCommentParser::new);
        converter.put("FullComment", FullCommentParser::new);
        converter.put("InlineCommandComment", InlineCommandCommentParser::new);

        /* attributes */
        converter.put("FinalAttr", FinalAttrParser::new);

        return converter;
    }

    // public App parse(ClangRootNode clangAst) {
    public App parse() {

        converter.setClangRootData(clangAst.getClangRootData());

        // Parse types
        Map<String, Type> types = new ClangTypesParser(converter).parse(clangAst.getClangRootData().getClangTypes());

        // Add original types to converter
        converter.setOriginalTypes(types);

        Map<String, Type> nodeTypes = buildNodeTypes(types, clangAst.getClangRootData());

        // Add types mapping to converter
        converter.setTypesMapping(nodeTypes);

        // Perform second pass over types
        processTypesSecondPass();

        // Process Clang nodes to add extra information (e.g., namespace and RecordDecl names to CXXMethodDecl)
        new ClangAstProcessor(converter).process(clangAst);

        // Parse root node
        RootParser rootParser = new RootParser(converter);

        App app = rootParser.parse(clangAst);

        // Add text elements (comments, pragmas) to the tree
        new TextParser().addElements(app);

        // Applies several passes to make the tree resemble more the original code, e.g., remove implicit nodes from
        // original clang tree
        if (sourceTree) {
            processSourceTree(app);
        }

        SpecsLogs.msgInfo("--- AST parsing report ---");
        checkUndefinedNodes(app);

        return app;
    }

    public Map<String, Type> getTypes() {
        return converter.getOriginalTypes();
    }

    private void processTypesSecondPass() {
        // Parse non-types node that where delayed until types where parsed
        parseDelayedTypes();

        // Add argument types to TemplateSpecializationType
        completeTemplateSpecializationTypes();
    }

    private void completeTemplateSpecializationTypes() {

        // Go over all original types and find TemplateSpecializationType nodes
        for (Type type : converter.getOriginalTypes().values()) {
            type.getDescendantsStream()
                    .filter(node -> node instanceof TemplateSpecializationType)
                    .map(node -> (TemplateSpecializationType) node)
                    .forEach(this::completeTemplateSpecializationType);
        }
        /*
                MultiMap<String, String> templateArgTypes = converter.getClangRootData().getTemplateArgTypes();
        
        for (Entry<String, List<String>> entry : templateArgTypes.entrySet()) {
            // Get TemplateSpecializationType
            Type type = converter.getOriginalTypes().get(entry.getKey());
        
            Preconditions.checkNotNull(type, "Expected type with id '" + entry.getKey() + "' to exist");
            SpecsChecks.checkClass(type, TemplateSpecializationType.class);
            TemplateSpecializationType templateType = (TemplateSpecializationType) type;
        
            // Get template arguments types
            List<Type> argsTypes = new ArrayList<>();
            for (String typeId : entry.getValue()) {
                // Get type
                Type argType = converter.getOriginalTypes().get(typeId);
                Preconditions.checkNotNull(argType, "Could not find a type for id '" + typeId + "'");
                argsTypes.add(argType);
            }
        
            templateType.setArgsTypes(argsTypes);
        }
        */
    }

    private void completeTemplateSpecializationType(TemplateSpecializationType templateType) {
        // Get args types id
        List<String> typeIds = converter.getClangRootData().getTemplateArgTypes()
                .get(templateType.getInfo().getExtendedId());

        // Get args types
        List<Type> argTypes = typeIds.stream()
                .map(typeId -> converter.getOriginalTypes().get(typeId))
                .collect(Collectors.toList());

        // Set
        templateType.setArgsTypes(argTypes);
    }

    private void parseDelayedTypes() {
        TransformQueue<ClavaNode> queue = new TransformQueue<>("Delayed Types Parsing");

        // Go over all original types and find DelayedParsing nodes
        for (Type type : converter.getOriginalTypes().values()) {
            type.getDescendantsStream()
                    .filter(node -> node instanceof DelayedParsing)
                    .forEach(delayedParsing -> queue.replace(delayedParsing,
                            converter.parse(((DelayedParsing) delayedParsing).getClangNode())));
        }

        // System.out.println("QUEUE:" + queue);
        queue.apply();

    }

    private static Map<String, Type> buildNodeTypes(Map<String, Type> types, ClangRootData clangRootData) {

        // Map ClangAst nodes addresses to the corresponding type
        Map<String, Type> nodeTypes = buildTypes(clangRootData.getNodeToTypes(), types);

        return nodeTypes;
    }

    private static Map<String, Type> buildTypes(Map<String, String> nodeToTypes, Map<String, Type> types) {
        Map<String, Type> nodeTypes = new HashMap<>();

        for (Entry<String, String> entry : nodeToTypes.entrySet()) {
            String nodeAddress = entry.getKey();
            String typeAddress = entry.getValue();

            Type type = types.get(typeAddress);

            if (type == null) {
                System.out.println("TYPES:" + types);
                System.out.println("TYPE ADDRESS:" + typeAddress);
                throw new RuntimeException("STOP");
            }
            // if (type == null) {
            // throw new RuntimeException(
            // "Could not find type for node with address '0x" + Long.toHexString(nodeAddress) + "'");
            // }

            nodeTypes.put(nodeAddress, type);

        }

        return nodeTypes;
    }

    public static void checkUndefinedNodes(ClavaNode node) {
        checkUndefinedNodes(() -> node.getDescendantsAndSelfStream());
    }

    public static void checkUndefinedNodes(Supplier<Stream<? extends ClavaNode>> nodes) {
        // Check how many undefined/dummy nodes there are
        List<DummyNode> unimplemented = nodes.get()
                .filter(descendent -> descendent instanceof DummyNode)
                .map(node -> (DummyNode) node)
                .collect(Collectors.toList());

        // System.out.println("AST:\n" + app);

        // Count who many diff nodes there are
        int numImplementedNodes = nodes.get()
                .filter(descendent -> !(descendent instanceof DummyNode))
                .map(descendent -> descendent.getClass().getSimpleName())
                .collect(Collectors.toSet())
                .size();

        if (!unimplemented.isEmpty()) {

            // Count who many diff nodes there are

            Set<String> tempNodes = unimplemented.stream().map(dummy -> dummy.getOriginalType())
                    .collect(Collectors.toSet());

            SpecsLogs.msgInfo("Found " + tempNodes.size() + " unimplemented nodes out of "
                    + (tempNodes.size() + numImplementedNodes) + ":\n" +
                    tempNodes.stream().collect(Collectors.joining("\n - ", " - ", "\n")));

        } else {
            SpecsLogs.msgInfo("Found " + numImplementedNodes + " different nodes");
        }
    }

    private static void processSourceTree(ClavaNode node) {

        POST_PARSING_RULES.stream()
                .forEach(transform -> transform.visit(node));

        // long tic = System.nanoTime();

        // TraversalStrategy.POST_ORDER.apply(node, new RemoveTemporaryExpressions());
        // TraversalStrategy.POST_ORDER.apply(node, new RemoveMaterializeTempExpressions());

        // TraversalStrategy.POST_ORDER.apply(node, new RemoveExtraNodes());
        // TraversalStrategy.POST_ORDER.apply(node, new ExtractFullComments());
        // TraversalStrategy.POST_ORDER.apply(node, new CreateDeclStmts());
        // TraversalStrategy.POST_ORDER.apply(node, new AdaptBoolTypes());
        // TraversalStrategy.POST_ORDER.apply(node, new AdaptBoolCasts());

        // ParseUtils.printTime("Clava AST Post-processing", tic);
    }

    @Override
    public void close() throws Exception {
        converter.close();
    }

}
