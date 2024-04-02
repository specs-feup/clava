import AnalyserResult from "../AnalyserResult.js";
export default class DoubleFreeResult extends AnalyserResult {
    ptrName;
    scopeName;
    freedFlag = 0;
    constructor(name, node, message, ptrName, scopeName, fix) {
        super(name, node, message, fix);
        this.ptrName = ptrName;
        this.scopeName = scopeName;
    }
}
//# sourceMappingURL=DoubleFreeResult.js.map