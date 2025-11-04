/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.weaver;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.cilk.CilkFor;
import pt.up.fe.specs.clava.ast.cilk.CilkSpawn;
import pt.up.fe.specs.clava.ast.cilk.CilkSync;
import pt.up.fe.specs.clava.ast.comment.Comment;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.expr.*;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TagDeclVars;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.lara.LaraMarkerPragma;
import pt.up.fe.specs.clava.ast.lara.LaraTagPragma;
import pt.up.fe.specs.clava.ast.omp.OmpPragma;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.ast.stmt.*;
import pt.up.fe.specs.clava.ast.type.*;
import pt.up.fe.specs.clava.utils.NullNode;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.joinpoints.*;
import pt.up.fe.specs.clava.weaver.joinpoints.cilk.CxxCilkFor;
import pt.up.fe.specs.clava.weaver.joinpoints.cilk.CxxCilkSpawn;
import pt.up.fe.specs.clava.weaver.joinpoints.cilk.CxxCilkSync;
import pt.up.fe.specs.clava.weaver.joinpoints.types.*;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.classmap.FunctionClassMap;

import java.util.List;
import java.util.Optional;

public class CxxJoinpoints {

    private static final FunctionClassMap<ClavaNode, ACxxWeaverJoinPoint> JOINPOINT_FACTORY;

    static {
        JOINPOINT_FACTORY = new FunctionClassMap<>();
        JOINPOINT_FACTORY.put(ElaboratedType.class, CxxElaboratedType::new);

        JOINPOINT_FACTORY.put(CastExpr.class, CxxCast::new);
        JOINPOINT_FACTORY.put(BinaryOperator.class, CxxBinaryOp::new);
        JOINPOINT_FACTORY.put(UnaryOperator.class, CxxUnaryOp::new);
        JOINPOINT_FACTORY.put(ConditionalOperator.class, CxxTernaryOp::new);
        JOINPOINT_FACTORY.put(CXXMemberCallExpr.class, CxxMemberCall::new);
        JOINPOINT_FACTORY.put(CUDAKernelCallExpr.class, CXXCudaKernelCall::new);
        JOINPOINT_FACTORY.put(CallExpr.class, CxxCall::new);
        JOINPOINT_FACTORY.put(DeclRefExpr.class, CxxVarref::new);
        JOINPOINT_FACTORY.put(ArraySubscriptExpr.class, CxxJoinpoints::arrayAccessFactory);
        JOINPOINT_FACTORY.put(MemberExpr.class, CxxMemberAccess::new);
        JOINPOINT_FACTORY.put(CXXNewExpr.class, CxxNewExpr::new);
        JOINPOINT_FACTORY.put(CXXDeleteExpr.class, CxxDeleteExpr::new);
        JOINPOINT_FACTORY.put(UnaryExprOrTypeTraitExpr.class, CxxUnaryExprOrType::new);
        JOINPOINT_FACTORY.put(CXXThisExpr.class, CxxThis::new);
        // JOINPOINT_FACTORY.put(LiteralExpr.class, CxxExpression::new);
        // JOINPOINT_FACTORY.put(IntegerLiteral.class, CxxExpression::new);
        // JOINPOINT_FACTORY.put(FloatingLiteral.class, CxxExpression::new);
        JOINPOINT_FACTORY.put(IntegerLiteral.class, CxxIntLiteral::new);
        JOINPOINT_FACTORY.put(FloatingLiteral.class, CxxFloatLiteral::new);
        JOINPOINT_FACTORY.put(CXXBoolLiteralExpr.class, CxxBoolLiteral::new);
        JOINPOINT_FACTORY.put(Literal.class, CxxLiteral::new);
        JOINPOINT_FACTORY.put(ParenExpr.class, CxxParenExpr::new);
        JOINPOINT_FACTORY.put(InitListExpr.class, CxxInitList::new);
        JOINPOINT_FACTORY.put(ImplicitValueInitExpr.class, CxxImplicitValue::new);
        JOINPOINT_FACTORY.put(Expr.class, CxxExpression::new);
        JOINPOINT_FACTORY.put(IfStmt.class, CxxIf::new);
        JOINPOINT_FACTORY.put(LoopStmt.class, CxxLoop::new);
        // JOINPOINT_FACTORY.put(CompoundStmt.class, CxxScope::new);
        JOINPOINT_FACTORY.put(CompoundStmt.class, CxxJoinpoints::compoundStmtFactory);
        JOINPOINT_FACTORY.put(ReturnStmt.class, CxxReturnStmt::new);
        JOINPOINT_FACTORY.put(SwitchStmt.class, CxxSwitch::new);
        JOINPOINT_FACTORY.put(SwitchCase.class, CxxCase::new);
        JOINPOINT_FACTORY.put(ExprStmt.class, CxxExprStmt::new);
        JOINPOINT_FACTORY.put(DeclStmt.class, CxxDeclStmt::new);
        JOINPOINT_FACTORY.put(GotoStmt.class, CxxGotoStmt::new);
        JOINPOINT_FACTORY.put(ContinueStmt.class, CxxContinue::new);
        JOINPOINT_FACTORY.put(BreakStmt.class, CxxBreak::new);
        JOINPOINT_FACTORY.put(LabelStmt.class, CxxLabelStmt::new);
        JOINPOINT_FACTORY.put(AsmStmt.class, CxxAsmStmt::new);
        JOINPOINT_FACTORY.put(EmptyStmt.class, CxxEmptyStmt::new);
        JOINPOINT_FACTORY.put(Stmt.class, CxxStatement::new);
        JOINPOINT_FACTORY.put(CXXMethodDecl.class, CxxMethod::new);
        JOINPOINT_FACTORY.put(FunctionDecl.class, CxxFunction::new);
        JOINPOINT_FACTORY.put(CXXRecordDecl.class, CxxClass::new);
        JOINPOINT_FACTORY.put(RecordDecl.class, CxxStruct::new);
        // JOINPOINT_FACTORY.put(RecordDecl.class, CxxJoinpoints::recordDeclFactory);
        JOINPOINT_FACTORY.put(FieldDecl.class, CxxField::new);
        JOINPOINT_FACTORY.put(ParmVarDecl.class, CxxParam::new);
        JOINPOINT_FACTORY.put(VarDecl.class, CxxVardecl::new);
        JOINPOINT_FACTORY.put(EnumDecl.class, CxxEnumDecl::new);
        JOINPOINT_FACTORY.put(EnumConstantDecl.class, CxxEnumeratorDecl::new);
        JOINPOINT_FACTORY.put(TypedefNameDecl.class, CxxTypedefNameDecl::new);
        JOINPOINT_FACTORY.put(TypedefDecl.class, CxxTypedefDecl::new);
        JOINPOINT_FACTORY.put(NamedDecl.class, CxxNamedDecl::new);
        JOINPOINT_FACTORY.put(IncludeDecl.class, CxxInclude::new);
        JOINPOINT_FACTORY.put(AccessSpecDecl.class, CxxAccessSpecifier::new);
        JOINPOINT_FACTORY.put(LabelDecl.class, CxxLabelDecl::new);
        JOINPOINT_FACTORY.put(Decl.class, CxxDecl::new);
        // JOINPOINT_FACTORY.put(LiteralDecl.class, CxxDecl::new);
        JOINPOINT_FACTORY.put(BuiltinType.class, CxxBuiltinType::new);
        JOINPOINT_FACTORY.put(PointerType.class, CxxPointerType::new);
        JOINPOINT_FACTORY.put(VariableArrayType.class, CxxVariableArrayType::new);
        JOINPOINT_FACTORY.put(IncompleteArrayType.class, CxxIncompleteArrayType::new);
        JOINPOINT_FACTORY.put(ArrayType.class, CxxArrayType::new);
        JOINPOINT_FACTORY.put(FunctionType.class, CxxFunctionType::new);
        JOINPOINT_FACTORY.put(EnumType.class, CxxEnumType::new);
        JOINPOINT_FACTORY.put(TemplateSpecializationType.class, CxxTemplateSpecializationType::new);
        JOINPOINT_FACTORY.put(TagType.class, CxxTagType::new);
        JOINPOINT_FACTORY.put(QualType.class, CxxQualType::new);
        JOINPOINT_FACTORY.put(ParenType.class, CxxParenType::new);
        JOINPOINT_FACTORY.put(AdjustedType.class, CxxAdjustedType::new);
        JOINPOINT_FACTORY.put(TypedefType.class, CxxTypedefType::new);
        // JOINPOINT_FACTORY.put(ElaboratedType.class, CxxElaboratedType::new);
        JOINPOINT_FACTORY.put(Type.class, CxxType::new);
        JOINPOINT_FACTORY.put(LaraMarkerPragma.class, CxxMarker::new);
        JOINPOINT_FACTORY.put(LaraTagPragma.class, CxxTag::new);
        JOINPOINT_FACTORY.put(OmpPragma.class, CxxOmp::new);
        JOINPOINT_FACTORY.put(Pragma.class, CxxPragma::new);
        JOINPOINT_FACTORY.put(TranslationUnit.class, CxxFile::new);
        JOINPOINT_FACTORY.put(App.class, CxxJoinpoints::programFactory);
        JOINPOINT_FACTORY.put(NullExpr.class, CxxJoinpoints::nullNode);
        JOINPOINT_FACTORY.put(NullDecl.class, CxxJoinpoints::nullNode);
        JOINPOINT_FACTORY.put(NullStmt.class, CxxJoinpoints::nullNode);
        JOINPOINT_FACTORY.put(NullType.class, CxxUndefinedType::new);
        // JOINPOINT_FACTORY.put(NullNodeOld.class, CxxEmpty::new);
        JOINPOINT_FACTORY.put(Comment.class, CxxComment::new);
        // JOINPOINT_FACTORY.put(WrapperStmt.class, CxxJoinpoints::wrapperStmtFactory);
        JOINPOINT_FACTORY.put(WrapperStmt.class, CxxWrapperStmt::new);
        JOINPOINT_FACTORY.put(CilkFor.class, CxxCilkFor::new);
        JOINPOINT_FACTORY.put(CilkSync.class, CxxCilkSync::new);
        JOINPOINT_FACTORY.put(CilkSpawn.class, CxxCilkSpawn::new);
        JOINPOINT_FACTORY.put(TagDeclVars.class, GenericJoinpoint::new);
        JOINPOINT_FACTORY.put(ClavaNode.class, CxxJoinpoints::defaultFactory);
    }

    private static ACxxWeaverJoinPoint nullNode(ClavaNode node) {
        SpecsCheck.checkArgument(node instanceof NullNode, () -> "Expected an instance of NullNode, received: " + node);

        return null;
    }

    /**
     * Makes sure the node and its super have a weaver set.
     *
     * @param newJoinPoint
     */
    /*
     * private static void setWeaverEngine(ACxxWeaverJoinPoint newJoinPoint) {
     * ACxxWeaverJoinPoint currentJoinpoint = newJoinPoint;
     * CxxWeaver weaver = getWeaver();
     *
     * while (currentJoinpoint != null) {
     *
     * // Set engine
     * currentJoinpoint.setWeaverEngine(weaver);
     * currentJoinpoint = currentJoinpoint.getSuper()
     * .map(ACxxWeaverJoinPoint.class::cast)
     * .orElse(null);
     *
     * }
     * }
     */

    /*
     * private final CxxWeaver weaverEngine;
     *
     * public CxxJoinpoints(CxxWeaver weaverEngine) {
     * this.weaverEngine = weaverEngine;
     * }
     */
    // private static ACxxWeaverJoinPoint typeFactory(Type type, ACxxWeaverJoinPoint
    // parent) {
    //
    // }

    // private static ACxxWeaverJoinPoint tuFactory(TranslationUnit tu,
    // ACxxWeaverJoinPoint parent) {
    // return new CxxFile(tu, parent);
    // // return new CxxFile(tu, parent == null ? null : parent.getRoot());
    // }
    public static CxxProgram programFactory(App app) {
        CxxWeaver weaver = CxxWeaver.getCxxWeaver();
        return new CxxProgram(weaver.getProgramName(), app, weaver);
    }

    private static ACxxWeaverJoinPoint compoundStmtFactory(CompoundStmt stmt) {
        // If no parent, use Scope as default
        if (!stmt.hasParent()) {
            return new CxxScope(stmt);
        }

        // If CompoundStmt parent is another CompoundStmt, is a Scope.
        if (stmt.getParent() instanceof CompoundStmt) {
            return new CxxScope(stmt);
        }

        // Otherwise, is a Body
        return new CxxBody(stmt);
    }

    // private static ACxxWeaverJoinPoint recordDeclFactory(RecordDecl record) {
    //
    // if (record.getTagKind() == TagKind.STRUCT) {
    // return new CxxStruct(record);
    // }
    //
    // if (record.getTagKind() == TagKind.CLASS) {
    // return new CxxClass((CXXRecordDecl) record);
    // }
    //
    // return new CxxRecord(record);
    // }

    private static ACxxWeaverJoinPoint arrayAccessFactory(ArraySubscriptExpr expr) {
        /*
         * if (!expr.isTopLevel()) {
         * return CxxJoinpoints.nullNode(expr.getFactory().nullExpr());
         * }
         */
        return new CxxArrayAccess(expr);
    }

    private static ACxxWeaverJoinPoint defaultFactory(ClavaNode node) {
        SpecsLogs.warn("Factory not defined for nodes of class '" + node.getClass().getSimpleName() + "'");
        return new GenericJoinpoint(node);
    }

    public static ACxxWeaverJoinPoint createFromLara(Object node) {
        if (!(node instanceof ClavaNode)) {
            throw new RuntimeException(
                    "Expected input to be a ClavaNode, is " + node.getClass().getSimpleName() + ": " + node);
        }

        return create((ClavaNode) node);
    }

    public static ACxxWeaverJoinPoint create(ClavaNode node) {
        if (node == null) {
            ClavaLog.debug("CxxJoinpoints: tried to create join point from null node, returning undefined");
            return null;
        }

        return JOINPOINT_FACTORY.apply(node);
    }

    public static <T extends AJoinPoint> T create(ClavaNode node, Class<T> targetClass) {
        if (targetClass == null) {
            throw new RuntimeException("Check if you meant to call 'create' with a single argument");
        }

        return targetClass.cast(create(node));
    }

    public static <T extends AJoinPoint> T[] create(List<? extends ClavaNode> nodes, Class<T> targetClass) {
        return nodes.stream()
                .map(node -> create(node, targetClass))
                .toArray(size -> SpecsCollections.newArray(targetClass, size));
    }

    public static CxxProgram getProgram(AJoinPoint joinpoint) {
        AJoinPoint currentJp = joinpoint;
        while (currentJp.getHasParentImpl()) {
            currentJp = currentJp.getParentImpl();
        }

        // Check that root node is a CxxProgram
        if (!(currentJp instanceof CxxProgram)) {
            throw new RuntimeException("Expected root node to be of type '" + CxxProgram.class + "'");
        }

        return (CxxProgram) currentJp;
    }

    /**
     * The first ancestor (including self) of the given type.
     *
     * @param joinpointClass
     * @return
     */
    public static <T extends AJoinPoint> Optional<T> getAncestorandSelf(AJoinPoint joinpoint, Class<T> joinpointClass) {
        AJoinPoint currentJp = joinpoint;

        if (joinpointClass.isInstance(currentJp)) {
            return Optional.of(joinpointClass.cast(currentJp));
        }

        while (currentJp.getHasParentImpl()) {
            currentJp = currentJp.getParentImpl();

            if (joinpointClass.isInstance(currentJp)) {
                return Optional.of(joinpointClass.cast(currentJp));
            }
        }

        return Optional.empty();
    }

    public static CxxWeaver getWeaver(AJoinPoint joinpoint) {
        // Get root joinpoint (program)
        return getProgram(joinpoint).getWeaver();
    }

    // public static AJoinPoint create(Stmt newNode) {
    // return create(newNode, null);
    // }
}
