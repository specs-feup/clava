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

package pt.up.fe.specs.clang.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.utils.NullNodeAdapter.NullNodeType;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.CXXCatchStmt;
import pt.up.fe.specs.clava.ast.stmt.CXXForRangeStmt;
import pt.up.fe.specs.clava.ast.stmt.CXXTryStmt;
import pt.up.fe.specs.clava.ast.stmt.CaseStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DefaultStmt;
import pt.up.fe.specs.clava.ast.stmt.DoStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.LabelStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WhileStmt;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.utils.NullNode;
import pt.up.fe.specs.util.classmap.ClassMap;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class ChildrenAdapter {

    private final ClavaContext context;

    public ChildrenAdapter(ClavaContext context) {
        this.context = context;
    }

    // private ClavaFactory getFactory() {
    // return context.get(ClavaContext.FACTORY);
    // }

    private final static ClassMap<ClavaNode, BiFunction<List<ClavaNode>, ClavaContext, List<ClavaNode>>> CHILDREN_ADAPTERS;
    static {
        CHILDREN_ADAPTERS = new ClassMap<>((list, context) -> list);
        CHILDREN_ADAPTERS.put(IfStmt.class, ChildrenAdapter::adaptIfStmt);
        CHILDREN_ADAPTERS.put(ForStmt.class, ChildrenAdapter::adaptForStmt);
        CHILDREN_ADAPTERS.put(WhileStmt.class, ChildrenAdapter::adaptWhileStmt);
        CHILDREN_ADAPTERS.put(DoStmt.class, ChildrenAdapter::adaptDoStmt);
        CHILDREN_ADAPTERS.put(CXXForRangeStmt.class, ChildrenAdapter::adaptCXXForRangeStmt);
        CHILDREN_ADAPTERS.put(CompoundStmt.class, ChildrenAdapter::adaptCompoundStmt);
        CHILDREN_ADAPTERS.put(CXXCatchStmt.class, ChildrenAdapter::adaptCXXCatchStmt);
        CHILDREN_ADAPTERS.put(CXXTryStmt.class, ChildrenAdapter::adaptCXXTryStmt);
        CHILDREN_ADAPTERS.put(CaseStmt.class, ChildrenAdapter::adaptCaseStmt);
        CHILDREN_ADAPTERS.put(DefaultStmt.class, ChildrenAdapter.adapt(AdaptationType.STMT));
        CHILDREN_ADAPTERS.put(LabelStmt.class, ChildrenAdapter.adapt(AdaptationType.STMT));
        // CHILDREN_ADAPTERS.put(GotoStmt.class, ChildrenAdapter.adapt(AdaptationType.STMT));
    }

    private final static ClassMap<ClavaNode, NullNodeAdapter> NULL_NODE_MAPPER;
    static {
        NULL_NODE_MAPPER = new ClassMap<>(NullNodeAdapter.newEmpty());
        NULL_NODE_MAPPER.put(IfStmt.class,
                NullNodeAdapter.newInstance(NullNodeType.DECL, null, NullNodeType.STMT, NullNodeType.STMT));
        NULL_NODE_MAPPER.put(WhileStmt.class,
                NullNodeAdapter.newInstance(NullNodeType.DECL, NullNodeType.STMT, NullNodeType.STMT));
        NULL_NODE_MAPPER.put(CXXCatchStmt.class,
                NullNodeAdapter.newInstance(NullNodeType.DECL, NullNodeType.STMT));
    }

    public List<ClavaNode> adaptChildren(ClavaNode node, List<ClavaNode> children) {
        // Replace NullNodeOld instances with NullNode
        List<ClavaNode> adaptedChildren = NULL_NODE_MAPPER.get(node.getClass()).adapt(context.get(ClavaContext.FACTORY),
                node, children);

        // Apply normalization steps to children
        adaptedChildren = CHILDREN_ADAPTERS.get(node.getClass()).apply(adaptedChildren, context);

        return adaptedChildren;
    }

    private static List<ClavaNode> adaptIfStmt(List<ClavaNode> children, ClavaContext context) {
        // Check if then and else statements are compound statements
        if (children.get(2) instanceof CompoundStmt && children.get(3) instanceof CompoundStmt) {
            return children;
        }

        List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());

        adaptedChildren.add(children.get(0));
        adaptedChildren.add(children.get(1));
        adaptedChildren.add(toCompoundStmt(children.get(2), context));
        adaptedChildren.add(toCompoundStmt(children.get(3), context));

        return adaptedChildren;
    }

    private static List<ClavaNode> adaptForStmt(List<ClavaNode> children, ClavaContext context) {
        // Check body is a compound statements
        // if (children.get(3) instanceof CompoundStmt) {
        // return children;
        // }

        List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());

        adaptedChildren.add(toStmt(children.get(0), context));
        adaptedChildren.add(toStmt(children.get(1), context));
        adaptedChildren.add(toStmt(children.get(2), false, context));
        adaptedChildren.add(toCompoundStmt(children.get(3), false, context));

        return adaptedChildren;
    }

    private static List<ClavaNode> adaptCXXForRangeStmt(List<ClavaNode> children, ClavaContext context) {

        List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());

        adaptedChildren.add(toStmt(children.get(0), context));
        adaptedChildren.add(toStmt(children.get(1), context));
        adaptedChildren.add(toStmt(children.get(2), context));
        adaptedChildren.add(toStmt(children.get(3), context));
        adaptedChildren.add(toStmt(children.get(4), context));
        adaptedChildren.add(toCompoundStmt(children.get(5), false, context));

        return adaptedChildren;
    }

    private static List<ClavaNode> adaptWhileStmt(List<ClavaNode> children, ClavaContext context) {

        List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());

        adaptedChildren.add(children.get(0));
        adaptedChildren.add(toStmt(children.get(1), false, context));
        adaptedChildren.add(toCompoundStmt(children.get(2), false, context));

        return adaptedChildren;
    }

    private static List<ClavaNode> adaptDoStmt(List<ClavaNode> children, ClavaContext context) {

        List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());

        adaptedChildren.add(toCompoundStmt(children.get(0), false, context));
        adaptedChildren.add(toStmt(children.get(1), false, context));

        return adaptedChildren;
    }

    private static List<ClavaNode> adaptCompoundStmt(List<ClavaNode> children, ClavaContext context) {

        boolean needsAdaptation = children.stream().filter(child -> !(child instanceof Stmt)).findFirst().isPresent();

        if (!needsAdaptation) {
            return children;
        }

        List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());
        for (ClavaNode child : children) {
            if (child instanceof Stmt) {
                adaptedChildren.add(child);
                continue;
            }

            if (child instanceof Expr) {
                adaptedChildren.add(context.get(ClavaContext.FACTORY).exprStmt((Expr) child));
                continue;
            }

            throw new CaseNotDefinedException(child.getClass());
        }

        return adaptedChildren;
    }

    private static List<ClavaNode> adaptCXXCatchStmt(List<ClavaNode> children, ClavaContext context) {
        List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());

        adaptedChildren.add(children.get(0));
        adaptedChildren.add(toCompoundStmt(children.get(1), context));

        return adaptedChildren;
    }

    private static List<ClavaNode> adaptCXXTryStmt(List<ClavaNode> children, ClavaContext context) {
        List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());
        // System.out.println("CHILDREN:"
        // + children.stream().map(child -> child.get(ClavaNode.ID)).collect(Collectors.joining(", ")));

        // if (children.get(0) instanceof Decl) {
        // System.out.println("FOUND DECL " + children.get(0).get(ClavaNode.ID) + " IN TRY:" + children.get(0));
        // }
        adaptedChildren.add(toCompoundStmt(children.get(0), context));
        adaptedChildren.addAll(children.subList(1, children.size()));

        return adaptedChildren;
    }

    private static List<ClavaNode> adaptCaseStmt(List<ClavaNode> children, ClavaContext context) {
        List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());

        adaptedChildren.add(children.get(0));
        adaptedChildren.add(children.get(1));
        adaptedChildren.add(toStmt(children.get(2), context));

        return adaptedChildren;
    }

    // private static List<ClavaNode> adaptDefaultStmt(List<ClavaNode> children, ClavaContext context) {
    // List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());
    //
    // adaptedChildren.add(toStmt(children.get(0), context));
    //
    // return adaptedChildren;
    // }

    // private static List<ClavaNode> adaptLabelStmt(List<ClavaNode> children, ClavaContext context) {
    // List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());
    //
    // adaptedChildren.add(toStmt(children.get(0), context));
    //
    // return adaptedChildren;
    // }

    private static BiFunction<List<ClavaNode>, ClavaContext, List<ClavaNode>> adapt(AdaptationType... adaptations) {
        return adapt(Arrays.asList(adaptations));
    }

    private static BiFunction<List<ClavaNode>, ClavaContext, List<ClavaNode>> adapt(List<AdaptationType> adaptations) {
        return (children, context) -> {
            Preconditions.checkArgument(children.size() <= adaptations.size());

            List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());

            for (int i = 0; i < children.size(); i++) {
                ClavaNode node = adaptations.get(i).adapt(children.get(i), context);
                adaptedChildren.add(node);
            }

            return adaptedChildren;
        };

    }
    // private static List<ClavaNode> adapt(List<ClavaNode> children, ClavaContext context) {
    // List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());
    //
    // adaptedChildren.add(toStmt(children.get(0), context));
    //
    // return adaptedChildren;
    // }

    static ClavaNode toCompoundStmt(ClavaNode clavaNode, ClavaContext context) {
        return toCompoundStmt(clavaNode, true, context);
    }

    private static ClavaNode toCompoundStmt(ClavaNode clavaNode, boolean isOptional, ClavaContext context) {
        if (clavaNode instanceof CompoundStmt) {
            return clavaNode;
        }

        // NullNode is a valid value for CompoundStmt
        if (clavaNode instanceof NullNode) {
            if (isOptional) {
                return clavaNode;
            }

            return context.get(ClavaContext.FACTORY).compoundStmt().set(CompoundStmt.IS_NAKED);
        }

        // Wrap Expr around Stmt
        if (clavaNode instanceof Expr) {
            return toCompoundStmt(context.get(ClavaContext.FACTORY).exprStmt((Expr) clavaNode), isOptional, context);
        }

        // if (clavaNode instanceof Decl) {
        // return toCompoundStmt(context.get(ClavaContext.FACTORY).declStmt((Decl) clavaNode), isOptional, context);
        // }

        if (!(clavaNode instanceof Stmt)) {
            throw new RuntimeException(
                    "Expected node " + clavaNode.get(ClavaNode.ID) + " to be of class " + Stmt.class + " but it "
                            + clavaNode.getClass() + ": " + clavaNode);
        }

        return context.get(ClavaContext.FACTORY).compoundStmt((Stmt) clavaNode).set(CompoundStmt.IS_NAKED);
    }

    static ClavaNode toStmt(ClavaNode clavaNode, ClavaContext context) {
        return toStmt(clavaNode, true, context);
    }

    private static ClavaNode toStmt(ClavaNode clavaNode, boolean hasSemicolon, ClavaContext context) {
        if (clavaNode instanceof Stmt) {
            return clavaNode;
        }

        // NullNode is a valid value for Stmt
        if (clavaNode instanceof NullNode) {
            return clavaNode;
        }

        // Wrap Expr around Stmt
        if (clavaNode instanceof Expr) {
            Stmt exprStmt = context.get(ClavaContext.FACTORY).exprStmt((Expr) clavaNode);
            if (!hasSemicolon) {
                exprStmt.set(ExprStmt.HAS_SEMICOLON, false);
            }

            return exprStmt;

        }

        throw new NotImplementedException(clavaNode.getClass());
        // throw new RuntimeException(
        // "Expected node to be of class " + Stmt.class + " but it " + clavaNode.getClass());

    }
}
