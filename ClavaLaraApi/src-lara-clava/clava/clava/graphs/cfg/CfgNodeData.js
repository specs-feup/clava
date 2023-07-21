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

  /**
   * Creates a new instance of the CfgNodeData class
   * @param {CfgNodeType} cfgNodeType node type
   * @param {joinpoint} $stmt statement that originated this CFG node
   * @param id identification of the CFG node
   */
  constructor(cfgNodeType, $stmt, id) {
    // If id defined, give priority to it. Othewise, use stmt astId, if defined
    const _id =
      id !== undefined ? id : $stmt === undefined ? undefined : $stmt.astId;

    // Use AST node id as graph node id
    super(_id);

    this.#nodeStmt = $stmt;

    this.#type = cfgNodeType;
  }

  /**
   * @returns {CfgNodeType} the CFG node type
   */
  get type() {
    return this.#type;
  }

  /**
   * @returns {String} the CFG node name
   */
  get name() {
    if (this.#name === undefined) {
      const typeName = this.#type.name;
      this.#name =
        typeName.substring(0, 1).toUpperCase() +
        typeName.substring(1, typeName.length).toLowerCase();
    }

    return this.#name;
  }

  /**
   * @returns {joinpoint} the corresponding statement join point of the CFG node
   */
  get nodeStmt() {
    return this.#nodeStmt;
  }

  /**
   * Sets the corresponding statement join point of the CFG node
   * @param {joinpoint} $stmt the statement join point
  */
  set nodeStmt($stmt) {
    this.#nodeStmt = $stmt;
  }

  /**
   * @returns {Array <joinpoint>} The statements associated with this CFG node.
   */
  get stmts() {
    // By default, the list only containts the node statement
    return this.nodeStmt !== undefined ? [this.nodeStmt] : [];
  }

  /**
   * 
   * @returns {String} string representation of the CFG node
   */
  toString() {
    // By default, content of the node is the name of the type
    return (
      this.name.substring(0, 1).toUpperCase() +
      this.name.substring(1, this.name.length).toLowerCase()
    );
  }

  /**
   *
   * @returns {boolean} true if this is a branch node, false otherwise. If this is a branch node, contains two edges, true and false.
   * If not, contains only one uncoditional edge (expect if it is the end node, which contains no edges).
   */
  isBranch() {
    return false;
  }
}
