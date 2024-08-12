export default class Fix {
    node;
    fixAction;
    constructor(node, fixAction) {
        this.node = node;
        this.fixAction = fixAction;
    }
    getNode() {
        return this.node;
    }
    execute() {
        this.fixAction(this.node);
    }
}
//# sourceMappingURL=Fix.js.map