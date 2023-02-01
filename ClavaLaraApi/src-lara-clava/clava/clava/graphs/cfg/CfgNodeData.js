laraImport("clava.graphs.cfg.CfgNodeType");
laraImport("lara.graphs.NodeData");
laraImport("clava.ClavaJoinPoints");

/**
 * The data of a CFG node.
 */
class CfgNodeData extends NodeData {
  /**
   * The statement that originated this node
   */
  #nodeStmt;

  #type;

  #name;

  constructor(cfgNodeType, $stmt, id) {
    // If id defined, give priority to it. Othewise, use stmt astId, if defined
    const _id =
      id !== undefined ? id : $stmt === undefined ? undefined : $stmt.astId;

    // Use AST node id as graph node id
    super(_id);

    this.#nodeStmt = $stmt;

    this.#type = cfgNodeType;
  }

  get type() {
    return this.#type;
  }

  get name() {
    if (this.#name === undefined) {
      const typeName = this.#type.name;
      this.#name =
        typeName.substring(0, 1).toUpperCase() +
        typeName.substring(1, typeName.length).toLowerCase();
    }

    return this.#name;
  }

  get nodeStmt() {
    return this.#nodeStmt;
  }

  set nodeStmt($stmt) {
    this.#nodeStmt = $stmt;
  }

  /**
   * The stmts associated with this CFG node.
   */
  get stmts() {
    // By default, the list only containts the node statement
    return this.nodeStmt !== undefined ? [this.nodeStmt] : [];
  }

  toString() {
    // By default, content of the node is the name of the type
    return (
      this.name.substring(0, 1).toUpperCase() +
      this.name.substring(1, this.name.length).toLowerCase()
    );
  }

  /**
   *
   * @returns true if this is a branch node, false otherwise. If this is a branch node, contains two edges, true and false.
   * If not, contains only one uncoditional edge (expect if it is the end node, which contains no edges).
   */
  isBranch() {
    return false;
  }
}
