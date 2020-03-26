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

import java.util.Optional;
import java.util.function.BiConsumer;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.textparser.SnippetParser;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.stmt.CXXForRangeStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DoStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

/**
 * Class with utility methods related with weaver actions.
 *
 * TODO: Move methods that require the weaver to a new class that receives the weaver during construction and is
 * available in the weaver.
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
     * @param target
     * @param position
     * @param from
     */
    public static AJoinPoint insertAsStmt(ClavaNode target, String code, Insert insert, CxxWeaver weaver) {

        // Check: if inserting before or after, check if target is valid
        if (!isTargetValid(target, insert)) {
            ClavaLog.info("Could not insert code " + insert.getString() + " location " + target.getLocation());
            return null;
        }

        SnippetParser snippetParser = CxxWeaver.getSnippetParser();
        ClavaNode realTarget = null;
        switch (insert) {
        case BEFORE:
            // System.out.println("INSERT BEFORE");
            // NodeInsertUtils.insertBefore(getValidStatement(target), ClavaNodeFactory.literalStmt(code));
            Stmt beforeNode = snippetParser.parseStmt(code);
            // System.out.println("CODE: " + code);
            // System.out.println("INSERTING: " + beforeNode);
            realTarget = getValidStatement(target, insert);
            if (realTarget == null) {
                return null;
            }
            NodeInsertUtils.insertBefore(realTarget, beforeNode);
            return CxxJoinpoints.create(beforeNode);

        case AFTER:
            // NodeInsertUtils.insertAfter(getValidStatement(target), ClavaNodeFactory.literalStmt(code));
            Stmt afterNode = snippetParser.parseStmt(code);
            realTarget = getValidStatement(target, insert);
            if (realTarget == null) {
                return null;
            }
            NodeInsertUtils.insertAfter(realTarget, afterNode);
            return CxxJoinpoints.create(afterNode);

        case AROUND:
        case REPLACE:
            // Has to replace with a node of the same "kind" (e.g., Expr, Stmt...)
            ClavaNode replaceNode = ClavaNodes.toLiteral(code, CxxWeaver.getFactory().nullType(), target);
            weaver.clearUserField(target);
            NodeInsertUtils.replace(target, replaceNode);
            return CxxJoinpoints.create(replaceNode);
        default:
            throw new RuntimeException("Case not defined:" + insert);
        }
    }

    private static boolean isTargetValid(ClavaNode target, Insert insert) {
        // If before or after, check if invalid child of a For/ForRange/Do
        if (insert == Insert.AFTER || insert == Insert.BEFORE) {
            int indexOfTarget = target.indexOfSelf();
            ClavaNode targetParent = target.getParent();
            // System.out.println("INDEX OF TARGET: " + indexOfTarget);
            // System.out.println("CLASS: " + targetParent.getClass());
            // For
            if (targetParent instanceof ForStmt) {
                return indexOfTarget > 2 ? true : false;
            }

            // ForRange
            if (targetParent instanceof CXXForRangeStmt) {
                return indexOfTarget > 4 ? true : false;
            }

            // ForRange
            if (targetParent instanceof DoStmt) {
                return indexOfTarget == 0 ? true : false;
            }
        }

        return true;
    }

    public static AJoinPoint[] insertAsChild(String position, ClavaNode base, ClavaNode node, CxxWeaver weaver) {

        switch (position) {
        case "before":
            // Insert before all statements in body
            base.addChild(0, node);
            return null;

        case "after":
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
        ClavaNode adaptedBase = isInsideScope ? getValidStatement(baseJp.getNode(), position)
                : baseJp.getNode();

        if (adaptedBase == null) {
            return null;
        }

        ClavaNode adaptedNew = isInsideScope ? ClavaNodes.toStmt(newJp.getNode()) : newJp.getNode();

        insertFunction.accept(adaptedBase, adaptedNew);

        return CxxJoinpoints.create(adaptedNew);
    }

    /**
     * Returns the first valid statement where we can insert another node in the after/before inserts
     *
     * @param node
     * @return
     */
    public static Stmt getValidStatement(ClavaNode node, Insert position) {
        Stmt target = getValidStatement(node);

        // Check: if inserting before or after, check if target is valid
        if (!isTargetValid(target, position)) {
            ClavaLog.info("Could not insert code " + position.getString() + " location " + target.getLocation());
            return null;
            // return Optional.empty();
        }

        // return Optional.of(target);
        return target;
    }

    public static Stmt getValidStatement(ClavaNode node) {
        Optional<Stmt> stmt = ClavaNodes.getStatement(node);

        if (!stmt.isPresent()) {
            throw new RuntimeException("Node does not have a statement ancestor:\n" + node);
        }

        return stmt.get();
    }

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

        switch (position) {
        case "before":
            NodeInsertUtils.insertBefore(baseJp.getNode(), newJp.getNode());
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
        switch (position) {
        case "before":
            // Insert before all statements in body
            body.addChild(0, stmt);
            break;

        case "after":
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
        // Clear use fields
        for (ClavaNode child : node.getChildren()) {
            weaver.clearUserField(child);
        }

        // Remove all children
        node.removeChildren(0, node.getNumChildren());
    }
}
