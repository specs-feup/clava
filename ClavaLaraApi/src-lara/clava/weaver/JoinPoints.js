import JoinPointsBase from "lara-js/api/weaver/JoinPointsBase.js";
import ClavaJavaTypes from "../clava/ClavaJavaTypes.js";
export default class JoinPoints extends JoinPointsBase {
    toJoinPoint(node) {
        const cxxJps = ClavaJavaTypes.getCxxJoinPoints();
        return cxxJps.createFromLara(node);
    }
    /**
     *
     * @returns The children of the given node
     */
    _all_children($jp) {
        return $jp.children;
    }
    /**
     *
     * @returns The descendants of the given node
     */
    _all_descendants($jp) {
        return $jp.descendants;
    }
    /**
     *
     * @returns All the nodes that are inside the scope of a given node
     */
    _all_scope_nodes($jp) {
        return $jp.scopeNodes;
    }
}
//# sourceMappingURL=JoinPoints.js.map