import OpsCost from "./OpsCost.js";
export default class OpsBlock {
    id;
    cost = new OpsCost();
    nestedOpsBlocks = [];
    repetitions = "1";
    constructor(id) {
        this.id = id;
    }
    add(opsId) {
        this.cost.increment(opsId);
    }
}
//# sourceMappingURL=OpsBlock.js.map