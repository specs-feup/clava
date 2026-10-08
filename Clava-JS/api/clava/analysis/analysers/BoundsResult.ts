import { Vardecl } from "../../../Joinpoints.ts";
import AnalyserResult from "../AnalyserResult.ts";
import Fix from "../Fix.ts";

export default class BoundsResult extends AnalyserResult {
  arrayName: string;
  scopeName: string;
  initializedFlag: boolean;
  unsafeAccessFlag: boolean;
  lengths: number[];
  line: number | undefined = undefined;

  constructor(
    name: string,
    node: Vardecl,
    message: string,
    scopeName: string,
    initializedFlag: boolean,
    unsafeAccessFlag: boolean,
    lengths: number[],
    fix?: Fix
  ) {
    super(name, node, message, fix);
    this.arrayName = node.name;
    this.scopeName = scopeName;
    this.initializedFlag = initializedFlag;
    this.unsafeAccessFlag = unsafeAccessFlag;
    this.lengths = lengths;
  }
}
