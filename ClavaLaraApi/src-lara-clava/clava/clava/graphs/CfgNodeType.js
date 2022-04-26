/**
 * Enumeration of CFG node types.
 */
class CfgNodeType {
	
	static START = new CfgNodeType("START")
	static END = new CfgNodeType("END")
	static IF = new CfgNodeType("IF")
	static FOR = new CfgNodeType("FOR")
	static SCOPE = new CfgNodeType("SCOPE")
	
	#name;
	
	constructor(name) {
	    this.#name = name;
	}
	
	get name() {
		return this.#name;
	}
}