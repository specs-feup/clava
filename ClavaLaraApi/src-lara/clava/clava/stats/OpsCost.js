export default class OpsCost {
    ops = new Map();
    toString() {
        const entries = Array.from(this.ops.entries());
        const strings = entries.map(([key, value]) => `${key}=${value}`);
        return "{" + strings.join(", ") + "}";
    }
    increment(opsId) {
        const currentValue = this.ops.get(opsId) ?? 0;
        this.ops.set(opsId, currentValue + 1);
    }
}
//# sourceMappingURL=OpsCost.js.map