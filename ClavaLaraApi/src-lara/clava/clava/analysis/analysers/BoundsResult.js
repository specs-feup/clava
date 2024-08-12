import AnalyserResult from "../AnalyserResult.js";
export default class BoundsResult extends AnalyserResult {
    arrayName;
    scopeName;
    initializedFlag;
    unsafeAccessFlag;
    lengths;
    line = undefined;
    constructor(name, node, message, scopeName, initializedFlag, unsafeAccessFlag, lengths, fix) {
        super(name, node, message, fix);
        this.arrayName = node.name;
        this.scopeName = scopeName;
        this.initializedFlag = initializedFlag;
        this.unsafeAccessFlag = unsafeAccessFlag;
        this.lengths = lengths;
    }
}
//# sourceMappingURL=BoundsResult.js.map