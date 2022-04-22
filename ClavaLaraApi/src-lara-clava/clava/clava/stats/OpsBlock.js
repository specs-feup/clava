laraImport("clava.stats.OpsCost");

class OpsBlock {
  constructor(id) {
    this.id = id;
    this.cost = new OpsCost();
    this.nestedOpsBlocks = [];
    this.repetitions = 1;
  }

  toString() {
    return object2stringSimple(this);
  }

  add(opsId) {
    this.cost.increment(opsId);
  }
}
