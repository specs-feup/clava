import Query from "lara-js/api/weaver/Query.js";
import { BinaryOp, Varref, } from "../../Joinpoints.js";
export default class LivenessUtils {
    /**
     * Checks if the given graph is a Cytoscape graph
     * @deprecated Typescript type checking should be used instead
     */
    static isCytoscapeGraph(graph) {
        return (typeof graph === "object" &&
            "nodes" in graph &&
            "edges" in graph &&
            typeof graph.add === "function" &&
            typeof graph.layout === "function");
    }
    /**
     * Computes the union of two sets
     * @param set1 -
     * @param set2 -
     * @returns A set containing the union of elements from both input sets
     */
    static unionSets(set1, set2) {
        if (set1 === undefined && set2 === undefined) {
            return new Set();
        }
        else if (set1 === undefined) {
            if (set2 === undefined) {
                return new Set();
            }
            else {
                return set2;
            }
        }
        else if (set2 === undefined) {
            return set1;
        }
        return new Set([...set1, ...set2]);
    }
    /**
     * Computes the set difference between two sets
     * @param set1 -
     * @param set2 -
     * @returns A set containing all the elements present in set1 but not in set2
     */
    static differenceSets(set1, set2) {
        if (set1 === undefined) {
            return new Set();
        }
        if (set2 === undefined) {
            return set1;
        }
        return new Set([...set1].filter((x) => !set2.has(x)));
    }
    /**
     * Checks if two sets contain the same elements
     */
    static isSameSet(set1, set2) {
        if (set1 === undefined) {
            return false;
        }
        if (set2 === undefined) {
            return false;
        }
        if (set1.size !== set2.size) {
            return false;
        }
        return [...set1].every((e) => set2.has(e), set2);
    }
    /**
     * Returns the children of a node
     * @param node -
     * @returns An array containing the children of the given node
     */
    static getChildren(node) {
        const edges = node.connectedEdges();
        const outgoingEdges = edges.filter((edge) => edge.source() == node);
        return outgoingEdges.map((edge) => edge.target());
    }
    /**
     * Checks if the provided joinpoint refers to an assigned variable.
     * @param $varref - The varref join point
     * @deprecated This method assumes that the giver Varref has a BinaryOp parent. Use carefully.
     */
    static isAssignedVar($varref) {
        const $parent = $varref.parent;
        if ($parent === undefined) {
            return false;
        }
        if (!($parent instanceof BinaryOp)) {
            return false;
        }
        return ($parent !== undefined &&
            $parent.isAssignment &&
            $parent.left.astId === $varref.astId);
    }
    /**
     * Checks if the given joinpoint is a local variable or parameter
     * @param $varref - The varref join point
     */
    static isLocalOrParam($varref) {
        const $varDecl = $varref.vardecl;
        return $varDecl !== undefined && !$varDecl.isGlobal;
    }
    /**
     *
     * @param $jp - The statement join point
     * @returns A set of variable names declared with initialization in the given joinpoint
     */
    static getVarDeclsWithInit($jp) {
        const $varDecls = Query.searchFromInclusive($jp, "vardecl", {
            hasInit: true,
        });
        const varNames = [...$varDecls].map(($decl) => $decl.name);
        return new Set(varNames);
    }
    /**
     *
     * @param $jp - The statement join point
     * @returns A set containing the names of the local variables or parameters on the left-hand side (LHS) of each assignment present in the given joinpoint
     */
    static getAssignedVars($jp) {
        const $assignments = Query.searchFromInclusive($jp, "binaryOp", {
            isAssignment: true,
            left: (left) => left instanceof Varref,
        });
        const assignedVars = [...$assignments]
            .filter(($assign) => LivenessUtils.isLocalOrParam($assign.left))
            .map(($assign) => $assign.left.name);
        return new Set(assignedVars);
    }
    /**
     *
     * @param $jp - The statement join point
     * @returns A set containing the names of local variables or parameters referenced by varref joinpoints, excluding those present on the LHS of assignments.
     */
    static getVarRefs($jp) {
        const $varRefs = Query.searchFromInclusive($jp, "varref");
        const varNames = [...$varRefs]
            .filter(($ref) => !LivenessUtils.isAssignedVar($ref) &&
            LivenessUtils.isLocalOrParam($ref))
            .map(($ref) => $ref.name);
        return new Set(varNames);
    }
}
//# sourceMappingURL=LivenessUtils.js.map