/**
 * Abstract class created as a model for every result of analyser
 */
export default class AnalyserResult {
    name;
    node;
    message;
    fix;
    constructor(name, node, message, fix) {
        this.name = name;
        this.node = node;
        this.message = message;
        this.fix = fix;
    }
    analyse(startNode) {
        throw "Not implemented";
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