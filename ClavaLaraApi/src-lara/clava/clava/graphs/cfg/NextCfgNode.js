import { Case, FunctionJp, If, Loop, Scope, Statement, } from "../../../Joinpoints.js";
export default class NextCfgNode {
    /**
     * The AST node to process
     */
    entryPoint;
    /**
     * Maps stmts to graph nodes
     */
    nodes;
    /**
     * The end node of the graph
     */
    endNode;
    constructor($entryPoint, nodes, endNode) {
        this.entryPoint = $entryPoint;
        this.nodes = nodes;
        this.endNode = endNode;
    }
    /**
     *
     * @param $stmt -
     *
     * @returns the next graph node that executes unconditionally after the given stmt, or end node if no statement is executed
     */
    nextExecutedNode($stmt) {
        const afterStmt = this.nextExecutedStmt($stmt);
        // If after statement is undefined, return end node
        if (afterStmt === undefined) {
            return this.endNode;
        }
        // Get node corresponding to the after statement
        const afterNode = this.nodes.get(afterStmt.astId);
        // If the statement does not have an associated node, this means the next node is out of scope and should be considered the end node
        if (afterNode === undefined) {
            return this.endNode;
        }
        return afterNode;
    }
    /**
     * @returns The next stmt that executes unconditionally after the given stmt, of undefined if no statement is executed
     */
    nextExecutedStmt($stmt) {
        // By definition, there is no statement executed after the entry point
        if ($stmt.equals(this.entryPoint)) {
            return undefined;
        }
        // If stmt is a scope, there are several special cases
        if ($stmt instanceof Scope) {
            return this.nextExecutedStmtAfterScope($stmt);
        }
        const rightStmts = $stmt.siblingsRight;
        // If there are statements to the right, the rightmost non-case statement is the next to be executed
        if (rightStmts.length > 0) {
            for (const sibling of rightStmts) {
                if (!(sibling instanceof Case))
                    return sibling;
            }
        }
        // When there are no more statements, return what's next for the parent
        const $parent = $stmt.parent;
        if ($parent instanceof Statement) {
            return this.nextExecutedStmt($parent);
        }
        // There are no more statements
        else if ($parent instanceof FunctionJp) {
            return undefined;
        }
        else {
            throw new Error("Case not defined for nodes with parent of type " +
                $parent.joinPointType);
        }
    }
    /**
     * @returns The the next stmt that executes unconditionally after the given scope, of undefined if no statement is executed
     */
    nextExecutedStmtAfterScope($scope) {
        // Before returning what's next to the scope of the statement, there are some special cases
        // Check if scope is a then/else of an if
        const $scopeParent = $scope.parent;
        if ($scopeParent instanceof If) {
            // Next stmt is what comes next of if
            return this.nextExecutedStmt($scopeParent);
        }
        // Check if scope is the body of a loop
        if ($scopeParent instanceof Loop) {
            // Next stmt is what comes next of if
            switch ($scopeParent.kind) {
                case "while":
                case "dowhile":
                    if ($scopeParent.cond === undefined) {
                        throw new Error("Not implemented when for loops do not have a condition statement");
                    }
                    return $scopeParent.cond;
                case "for":
                    if ($scopeParent.step === undefined) {
                        throw new Error("Not implemented when for loops do not have a step statement");
                    }
                    return $scopeParent.step;
                default:
                    throw new Error("Case not defined for loops of kind " + $scopeParent.kind);
            }
        }
        // Special cases handled, check scope siblings
        const rightStmts = $scope.siblingsRight;
        // If there are statements, return next of parent
        if (rightStmts.length > 0) {
            return rightStmts[0];
        }
        // If scope parent is not a statement, there is no next statement
        if ($scopeParent instanceof Statement) {
            // Return next statement of parent statement
            return this.nextExecutedStmt($scopeParent);
        }
        else {
            return undefined;
        }
    }
}
//# sourceMappingURL=NextCfgNode.js.map