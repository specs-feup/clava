import { Break, Case, Continue, GotoStmt, If, LabelStmt, Loop, ReturnStmt, Scope, Switch, } from "../../../Joinpoints.js";
import CfgNodeType from "./CfgNodeType.js";
export default class CfgUtils {
    /**
     * @param $stmt - The statement join point
     * @returns True if the statement is considered a leader
     */
    static isLeader($stmt) {
        const graphNodeType = CfgUtils.getNodeType($stmt);
        return graphNodeType !== undefined;
    }
    /**
     * Returns the type of graph node based on the type of the leader statement. If this statement is not a leader, returns undefined
     * @param $stmt - The statement join point
     */
    static getNodeType($stmt) {
        if ($stmt instanceof If) {
            return CfgNodeType.IF;
        }
        // Loop stmt
        if ($stmt instanceof Loop) {
            return CfgNodeType.LOOP;
        }
        // Break stmt
        if ($stmt instanceof Break) {
            return CfgNodeType.BREAK;
        }
        // Continue stmt
        if ($stmt instanceof Continue) {
            return CfgNodeType.CONTINUE;
        }
        // Switch stmt
        if ($stmt instanceof Switch) {
            return CfgNodeType.SWITCH;
        }
        //Case stmt
        if ($stmt instanceof Case) {
            return CfgNodeType.CASE;
        }
        // Goto stmt
        if ($stmt instanceof GotoStmt) {
            return CfgNodeType.GOTO;
        }
        // Label stmt
        if ($stmt instanceof LabelStmt) {
            return CfgNodeType.LABEL;
        }
        // Return stmt
        if ($stmt instanceof ReturnStmt) {
            return CfgNodeType.RETURN;
        }
        // Stmt is part of loop header
        if ($stmt.isInsideLoopHeader) {
            const $loop = $stmt.parent;
            if (!($loop instanceof Loop)) {
                throw new Error("Statement is inside loop header but parent is not a loop: " +
                    $stmt.code);
            }
            if ($stmt.equals($loop.init)) {
                return CfgNodeType.INIT;
            }
            else if ($stmt.equals($loop.cond)) {
                return CfgNodeType.COND;
            }
            else if ($stmt.equals($loop.step)) {
                return CfgNodeType.STEP;
            }
            else {
                throw new Error("Statement is in the header of loop at " +
                    $loop.location +
                    " but could not identify what part of the header: " +
                    $stmt.code);
            }
        }
        // Scope stmt
        if ($stmt instanceof Scope) {
            const parent = $stmt.parent;
            if (parent instanceof If) {
                if ($stmt.equals(parent.then)) {
                    return CfgNodeType.THEN;
                }
                else if ($stmt.equals(parent.else)) {
                    return CfgNodeType.ELSE;
                }
            }
            return CfgNodeType.SCOPE;
        }
        // If is the first statement of a scope and is not any of the other type of statements,
        // consider the beginning of an INST_LIST
        const $stmtParent = $stmt.parent;
        if ($stmtParent instanceof Scope && $stmt.equals($stmtParent.firstStmt)) {
            return CfgNodeType.INST_LIST;
        }
        const left = $stmt.siblingsLeft;
        if (left.length > 0) {
            const lastLeft = left[left.length - 1];
            const leftNodeType = CfgUtils.getNodeType(lastLeft);
            if (leftNodeType !== undefined &&
                leftNodeType !== CfgNodeType.INST_LIST) {
                return CfgNodeType.INST_LIST;
            }
        }
        return undefined;
    }
    static getTarget(node, edgeType) {
        let target = undefined;
        for (const edge of node.connectedEdges()) {
            // Only targets of this node
            if (edge.source() !== node) {
                continue;
            }
            if (edge.data().type === edgeType) {
                if (target !== undefined) {
                    throw new Error(`Found duplicated edge of type '${edgeType.toString()}' in node ${node.data()}`);
                }
                target = edge.target();
            }
        }
        return target;
    }
}
//# sourceMappingURL=CfgUtils.js.map