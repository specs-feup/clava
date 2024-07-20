import { Joinpoint, Type } from "../../Joinpoints.js";

export interface Operation {
  undo(): void;
}

export class InsertOperation implements Operation {
  constructor(private newJP: Joinpoint) {}

  undo(): void {
      this.newJP.detach();
  }
}

export class ReplaceOperation implements Operation {
  constructor(private oldJP: Joinpoint, private newJP: Joinpoint, private count: number) {}

  undo(): void {
    const siblings: Joinpoint[] = this.newJP.siblingsRight;
    for (let i = 0; i < this.count - 1; i++){
      siblings.at(i)?.detach();
    }
    this.newJP.replaceWith(this.oldJP);
  }
}

export class SetChildOperation implements Operation {
  constructor(private newChildJP: Joinpoint, private oldChildJP?: Joinpoint) {}

  undo(): void {
    if (this.oldChildJP){
      this.newChildJP.replaceWith(this.oldChildJP);
    }
    else {
      this.newChildJP.detach();
    }
  }
}

export class TypeChangeOperation implements Operation {
  constructor(private jp: Joinpoint, private oldType: Type) {}

  undo(): void {
    this.jp.setType(this.oldType);
  }
}