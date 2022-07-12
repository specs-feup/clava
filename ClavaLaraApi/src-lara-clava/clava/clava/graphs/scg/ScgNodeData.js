laraImport("lara.graphs.NodeData");

class ScgNodeData extends NodeData {
  // The function represented by this node
  #function;

  constructor($function) {
    super();

    // Should use only canonical functions
    this.#function = $function.canonical;
  }

  get function() {
    return this.#function;
  }

  toString() {
    return this.#function.signature;
  }

  /**
   * @returns true, if the function represented by this node has an available implementation, false otherwise
   */
  hasImplementation() {
    return this.#function.isImplementation;
  }
}
