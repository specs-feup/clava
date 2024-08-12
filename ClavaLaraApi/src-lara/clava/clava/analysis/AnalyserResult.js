/**
 * Abstract class created as a model for every result of analyser
 */
export default class AnalyserResult {
    node;
    fix;
    name;
    message;
    constructor(name, node, message, fix) {
        this.name = name;
        this.node = node;
        this.message = message;
        this.fix = fix;
    }
    getName() {
        return this.name;
    }
    getNode() {
        return this.node;
    }
    getMessage() {
        return this.message;
    }
    performFix() {
        this.fix?.execute();
    }
}
//# sourceMappingURL=AnalyserResult.js.map