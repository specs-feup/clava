class OpsCost {
  constructor() {
    this.ops = {};
  }

  toString() {
    return object2stringSimple(this);
  }

  increment(opsId) {
    const currentValue = this.ops[opsId];
    if (currentValue === undefined) {
      this.ops[opsId] = 1;
    } else {
      this.ops[opsId] = currentValue + 1;
    }
  }
}
