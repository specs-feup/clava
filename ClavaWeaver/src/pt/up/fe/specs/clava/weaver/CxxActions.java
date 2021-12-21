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

package pt.up.fe.specs.clava.weaver;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.clava.utils.NodePosition;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

/**
 * Class with utility methods related with weaver actions.
 *
 * TODO: Move methods that require the weaver to a new class that receives the weaver during construction and is
 * available in the weaver.
 * 
 * TODO: There are many inserts here, check if they can be reduced.
 *
 * @author JoaoBispo
 *
 */
public class CxxActions {

    /**
     * Insert children from a C/C++ token before/after the reference token, when the token is a statement of is further
     * below in the AST.
     *
     * <p>
     * Minimum granularity level of insert before/after is at the statement level.
     *
     *
     * 
     * @param target
     * @param position
     * @param from
     */
    public static AJoinPoint insertAsStmt(ClavaNode target, String code, Insert insert, CxxWeaver weaver) {
        // If target is part of App, clear caches
        target.getAncestorTry(App.class).ifPresent(app -> app.clearCache());

        // Convert Insert to NodePosition
        var position = insert.toPosition();
        ClavaNode node = ClavaNodes.insertAsStmt(target, code, position);

        // If null, return
        if (node == null) {
            return null;
        }

        // If replace and could insert a node, clear information of target node
        if (position == NodePosition.REPLACE) {
            weaver.clearUserField(target);
        }

        return CxxJoinpoints.create(node);
    }

    private static void checkInsertAfterReturn(ClavaNode base, ClavaNode newNode) {

        // Special case: inserting code after return
        if (base instanceof ReturnStmt && !(newNode instanceof WrapperStmt)) {
            SpecsLogs.info("Warning: inserting code after return, check if this is intended.\nCode:\n"
                    + newNode.getCode() + "\nReturn: " + base.getCode());
        }
    }

    public static AJoinPoint[] insertAsChild(String position, ClavaNode base, ClavaNode node, CxxWeaver weaver) {
        // If base is part of App, clear caches
        base.getAncestorTry(App.class).ifPresent(app -> app.clearCache());

        switch (position) {
        case "before":
            // Insert before all statements in body
            base.addChild(0, node);
            return null;

        case "after":
            if (base.hasChildren()) {
                checkInsertAfterReturn(base.getChild(base.getNumChildren() - 1), node);
            }

            base.addChild(node);
            return null;

        case "around":
        case "replace":
            removeChildren(base, weaver);
            // // Clear use fields
            // for (ClavaNode child : base.getChildren()) {
            // weaver.clearUserField(child);
            // }
            // // Remove all children
            // base.removeChildren(0, base.getNumChildren());
            base.addChild(node);
            return new AJoinPoint[] { CxxJoinpoints.create(node) };
        default:
            throw new RuntimeException("Case not defined:" + position);
        }
    }

    // public static void replace(ClavaNode newNode, ClavaNode target, String position) {
    //
    // }

    public static ClavaNode replace(ClavaNode target, ClavaNode newNode, CxxWeaver weaver) {
        weaver.clearUserField(target);
        return NodeInsertUtils.replace(target, newNode);
    }

    public static AJoinPoint insertBefore(AJoinPoint baseJp, AJoinPoint newJp) {
        return insert(baseJp, newJp, Insert.BEFORE, (base, node) -> NodeInsertUtils.insertBefore(base, node));
        // Stmt newStmt = ClavaNodes.toStmt(newJp.getNode());
        // Stmt baseStmt = getValidStatement(baseJp.getNode(), Insert.BEFORE);
        // if (baseStmt == null) {
        // return null;
        // }
        // NodeInsertUtils.insertBefore(baseStmt, newStmt);
        //
        // return CxxJoinpoints.create(newStmt);
    }

    public static AJoinPoint insertAfter(AJoinPoint baseJp, AJoinPoint newJp) {
        checkInsertAfterReturn(baseJp.getNode(), newJp.getNode());

        return insert(baseJp, newJp, Insert.AFTER, (base, node) -> NodeInsertUtils.insertAfter(base, node));
        // // If inside a scope, treat nodes at the statement level
        // // if
        // Stmt newStmt = ClavaNodes.toStmt(newJp.getNode());
        // Stmt baseStmt = getValidStatement(baseJp.getNode(), Insert.AFTER);
        // if (baseStmt == null) {
        // return null;
        // }
        // NodeInsertUtils.insertAfter(baseStmt, newStmt);
        //
        // return CxxJoinpoints.create(newStmt);
    }

    public static AJoinPoint insert(AJoinPoint baseJp, AJoinPoint newJp, Insert position,
            BiConsumer<ClavaNode, ClavaNode> insertFunction) {

        // If baseJp will do a statement-base insertion, adapt nodes
        // Check if base is inside a scope
        boolean isInsideScope = baseJp.getNode().getAncestorTry(CompoundStmt.class).isPresent();

        // Optional<Stmt> targetStmt = ClavaNodes.getStatement(baseJp.getNode());
        ClavaNode adaptedBase = isInsideScope ? ClavaNodes.getValidStatement(baseJp.getNode(), position.toPosition())
                : baseJp.getNode();

        if (adaptedBase == null) {
            return null;
        }

        ClavaNode adaptedNew = isInsideScope ? ClavaNodes.toStmt(newJp.getNode()) : newJp.getNode();

        // If adaptedNew is not a comment or a pragma, and we are inserting before, adaptedBase should be the first
        // comment or pragma associated with current base
        if (position == Insert.BEFORE) {
            adaptedBase = ClavaNodes.getFirstNodeOfTargetRegion(adaptedBase, adaptedNew);
        }

        insertFunction.accept(adaptedBase, adaptedNew);

        // If base is part of App, clear caches
        adaptedBase.getAncestorTry(App.class).ifPresent(app -> app.clearCache());

        return CxxJoinpoints.create(adaptedNew);
    }

    /**
     * Returns the first valid statement where we can insert another node in the after/before inserts
     *
     * @param node
     * @return
     */
    // public static Stmt getValidStatement(ClavaNode node, Insert position) {
    // Stmt target = getValidStatement(node);
    //
    // // Check: if inserting before or after, check if target is valid
    // if (!isTargetValid(target, position)) {
    // ClavaLog.info("Could not insert code " + position.getString() + " location " + target.getLocation());
    // return null;
    // // return Optional.empty();
    // }
    //
    // // return Optional.of(target);
    // return target;
    // }

    // public static Stmt getValidStatement(ClavaNode node) {
    // Optional<Stmt> stmt = ClavaNodes.getStatement(node);
    //
    // if (!stmt.isPresent()) {
    // throw new RuntimeException("Node does not have a statement ancestor:\n" + node);
    // }
    //
    // return stmt.get();
    // }

    public static AJoinPoint insertJpAsStatement(AJoinPoint baseJp, AJoinPoint newJp, String position,
            CxxWeaver weaver) {

        AStatement stmtJp = CxxJoinpoints.create(ClavaNodes.toStmt(newJp.getNode()), AStatement.class);

        return insertJp(baseJp, stmtJp, position, weaver);
    }

    /**
     * Generic implementation that just directly inserts/replaced the node in the joinpoint.
     *
     * @param baseJp
     * @param newJpS
     * @param position
     */
    public static AJoinPoint insertJp(AJoinPoint baseJp, AJoinPoint newJp, String position, CxxWeaver weaver) {
        // If baseJp is part of App, clear caches
        baseJp.getNode().getAncestorTry(App.class).ifPresent(app -> app.clearCache());

        switch (position) {
        case "before":
            var newBase = ClavaNodes.getFirstNodeOfTargetRegion(baseJp.getNode(), newJp.getNode());
            NodeInsertUtils.insertBefore(newBase, newJp.getNode());
            break;

        case "after":
            NodeInsertUtils.insertAfter(baseJp.getNode(), newJp.getNode());
            break;

        case "around":
        case "replace":
            weaver.clearUserField(baseJp.getNode());
            NodeInsertUtils.replace(baseJp.getNode(), newJp.getNode());
            break;

        default:
            throw new RuntimeException("Case not defined:" + position);
        }

        return newJp;
    }

    public static void insertStmt(String position, Stmt body, Stmt stmt, CxxWeaver weaver) {
        Preconditions.checkArgument(body instanceof CompoundStmt);

        // If body is part of App, clear caches
        body.getAncestorTry(App.class).ifPresent(app -> app.clearCache());

        switch (position) {
        case "before":
            // Insert before all statements in body
            body.addChild(0, stmt);
            break;

        case "after":

            if (body.hasChildren()) {
                checkInsertAfterReturn(body.getChild(body.getNumChildren() - 1), stmt);
            }

            body.addChild(stmt);
            break;

        case "around":
        case "replace":
            // Remove all children
            removeChildren(body, weaver);
            // Add given statement
            body.addChild(stmt);
            break;
        default:
            throw new RuntimeException("Case not defined:" + position);
        }
    }

    public static void removeChildren(ClavaNode node, CxxWeaver weaver) {
        // If node is part of App, clear caches
        node.getAncestorTry(App.class).ifPresent(app -> app.clearCache());

        // Clear use fields
        for (ClavaNode child : node.getChildren()) {
            weaver.clearUserField(child);
        }

        // Remove all children
        node.removeChildren(0, node.getNumChildren());
    }

    public static AJoinPoint insertReturn(AScope scope, AJoinPoint code) {
        // Does not take into account situations where functions returns in all paths of an if/else.
        // This means it can lead to dead-code, although for C/C++ that does not seem to be problematic.

        List<Stmt> bodyStmts = ((CompoundStmt) scope.getNode()).toStatements();

        // Check if it has return statement, ignoring wrapper statements
        Stmt lastStmt = SpecsCollections.reverseStream(bodyStmts)
                .filter(stmt -> !(stmt instanceof WrapperStmt))
                .findFirst().orElse(null);

        ReturnStmt lastReturnStmt = lastStmt instanceof ReturnStmt ? (ReturnStmt) lastStmt : null;

        // Get list of all return statements inside children
        List<ReturnStmt> returnStatements = bodyStmts.stream()
                .flatMap(Stmt::getDescendantsStream)
                .filter(ReturnStmt.class::isInstance)
                .map(ReturnStmt.class::cast)
                .collect(Collectors.toList());

        AJoinPoint lastInsertPoint = null;

        if (lastReturnStmt != null) {
            returnStatements = SpecsCollections.concat(returnStatements, lastReturnStmt);
        }

        for (ReturnStmt returnStmt : returnStatements) {
            ACxxWeaverJoinPoint returnJp = CxxJoinpoints.create(returnStmt);
            lastInsertPoint = returnJp.insertBefore(code);
        }

        // If there is no return in the body, add at the end of the function
        if (lastReturnStmt == null) {
            lastInsertPoint = scope.insertEnd(code);
        }

        return lastInsertPoint;
    }
}
