import { Vardecl } from "../../../Joinpoints.js";
import AnalyserResult from "../AnalyserResult.js";
import Fix from "../Fix.js";

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
