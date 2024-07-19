import { Joinpoint } from "../../Joinpoints.js";

export interface Operation {
  undo(): void;
}

export class InsertOperation implements Operation {
  constructor(private newJP: Joinpoint) {}

  undo(): void {
      this.newJP.detach();
  }
}