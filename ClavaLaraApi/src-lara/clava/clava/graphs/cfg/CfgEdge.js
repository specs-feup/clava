import EdgeData from "lara-js/api/lara/graphs/EdgeData.js";
import CfgEdgeType from "./CfgEdgeType.js";
/**
 * An edge of the CFG
 */
export default class CfgEdge extends EdgeData {
    edgeType;
    /**
     * Creates a new instance of the CfgEdge class
     * @param type - Edge type
     */
    constructor(type) {
        super();
        this.edgeType = type;
    }
    /**
     * @returns The edge type
     */
    get type() {
        return this.edgeType;
    }
    /**
     *
     * @returns String representation of the edge. If it is a unconditional edge, an empty string is returned
     */
    toString() {
        // If unconditional jump, do not print a label
        if (this.edgeType === CfgEdgeType.UNCONDITIONAL) {
            return "";
        }
        // Otherwise, return the type name
        return this.edgeType.name;
    }
}
//# sourceMappingURL=CfgEdge.js.map