import OpsCost from "./OpsCost.js";

export default class OpsBlock {
  
  id: string;
  private cost = new OpsCost();
  nestedOpsBlocks: OpsBlock[] = [];
  repetitions: string = "1";
  
  constructor(id: string) {
    this.id = id;
  }

  add(opsId: string) {
    this.cost.increment(opsId);
  }
}
