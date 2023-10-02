import NodeData from "lara-js/api/lara/graphs/NodeData.js";
/**
 * The data of a CFG node.
 */
export default class CfgNodeData extends NodeData {
    /**
     * The statement join point of the CFG node
     */
    nodeStmt;
    nodeType;
    nodeName;
    /**
     * Creates a new instance of the CfgNodeData class
     * @param cfgNodeType - Node type
     * @param $stmt - Statement that originated this CFG node
     * @param id - Identification of the CFG node
     */
    constructor(cfgNodeType, $stmt, id = $stmt?.astId) {
        // If id defined, give priority to it. Othewise, use stmt astId, if defined
        const _id = id !== undefined ? id : $stmt === undefined ? undefined : $stmt.astId;
        // Use AST node id as graph node id
        super(_id);
        this.nodeStmt = $stmt;
        this.nodeType = cfgNodeType;
    }
    /**
     * @returns The CFG node type
     */
    get type() {
        return this.nodeType;
    }
    /**
     * @returns The CFG node name
     */
    get name() {
        if (this.nodeName === undefined) {
            const typeName = this.nodeType.name;
            this.nodeName =
                typeName.substring(0, 1).toUpperCase() +
                    typeName.substring(1, typeName.length).toLowerCase();
        }
        return this.nodeName;
    }
    /**
     * @returns The statements associated with this CFG node.
     */
    get stmts() {
        // By default, the list only containts the node statement
        return this.nodeStmt !== undefined ? [this.nodeStmt] : [];
    }
    /**
     *
     * @returns String representation of the CFG node
     */
    toString() {
        // By default, content of the node is the name of the type
        return this.name;
    }
    /**
     *
     * @returns True if this is a branch node, false otherwise. If this is a branch node, contains two edges, true and false.
     * If not, contains only one uncoditional edge (expect if it is the end node, which contains no edges).
     */
    isBranch() {
        return false;
    }
}
//# sourceMappingURL=CfgNodeData.js.map