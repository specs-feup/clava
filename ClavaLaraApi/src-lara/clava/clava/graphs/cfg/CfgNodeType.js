/**
 * Enumeration of CFG node types.
 */
export default class CfgNodeType {
    static START = new CfgNodeType("START");
    static END = new CfgNodeType("END");
    static IF = new CfgNodeType("IF");
    static THEN = new CfgNodeType("THEN");
    static ELSE = new CfgNodeType("ELSE");
    static LOOP = new CfgNodeType("LOOP");
    static COND = new CfgNodeType("COND");
    static INIT = new CfgNodeType("INIT");
    static STEP = new CfgNodeType("STEP");
    static SCOPE = new CfgNodeType("SCOPE");
    static INST_LIST = new CfgNodeType("INST_LIST");
    static BREAK = new CfgNodeType("BREAK");
    static CONTINUE = new CfgNodeType("CONTINUE");
    static SWITCH = new CfgNodeType("SWITCH");
    static CASE = new CfgNodeType("CASE");
    static RETURN = new CfgNodeType("RETURN");
    nameStr;
    /**
     * Creates a new instance of the CfgNodeType class
     * @param name - The name of the CFG node type
     */
    constructor(name) {
        this.nameStr = name;
    }
    /**
     * @returns The name of the CFG node type
     */
    get name() {
        return this.nameStr;
    }
    /**
     *
     * @returns String representation of the CFG node type, which corresponds to its name
     */
    toString() {
        return this.name;
    }
}
//# sourceMappingURL=CfgNodeType.js.map