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
import java.util.List;
import java.util.function.BiFunction;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.util.classmap.ClassMap;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;

public class ChildrenAdapter {

    private final ClavaContext context;

    public ChildrenAdapter(ClavaContext context) {
        this.context = context;
    }

    private ClavaFactory getFactory() {
        return context.get(ClavaContext.FACTORY);
    }

    private final static ClassMap<ClavaNode, BiFunction<List<ClavaNode>, ClavaContext, List<ClavaNode>>> CHILDREN_ADAPTERS;
    static {
        CHILDREN_ADAPTERS = new ClassMap<>((list, context) -> list);
        CHILDREN_ADAPTERS.put(IfStmt.class, ChildrenAdapter::adaptIfStmt);
        CHILDREN_ADAPTERS.put(CompoundStmt.class, ChildrenAdapter::adaptCompoundStmt);
    }

    public List<ClavaNode> adaptChildren(ClavaNode node, List<ClavaNode> children) {
        return CHILDREN_ADAPTERS.get(node.getClass()).apply(children, context);
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

    private static ClavaNode toCompoundStmt(ClavaNode clavaNode, ClavaContext context) {
        if (clavaNode instanceof CompoundStmt) {
            return clavaNode;
        }

        // Wrap Expr around Stmt
        if (clavaNode instanceof Expr) {
            return toCompoundStmt(context.get(ClavaContext.FACTORY).exprStmt((Expr) clavaNode), context);
        }

        if (!(clavaNode instanceof Stmt)) {
            throw new RuntimeException(
                    "Expected node to be of class " + Stmt.class + " but it " + clavaNode.getClass());
        }

        return context.get(ClavaContext.FACTORY).compoundStmt((Stmt) clavaNode);
    }
}
