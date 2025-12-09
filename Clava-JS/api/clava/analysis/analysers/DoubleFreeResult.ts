import { Joinpoint } from "../../../Joinpoints.js";
import AnalyserResult from "../AnalyserResult.js";
import Fix from "../Fix.js";

export default class DoubleFreeResult extends AnalyserResult {
  ptrName: string;
  scopeName: string;
  freedFlag: number = 0;

  constructor(
    name: string,
    node: Joinpoint,
    message: string,
    ptrName: string,
    scopeName: string,
    fix?: Fix
  ) {
    super(name, node, message, fix);
    this.ptrName = ptrName;
    this.scopeName = scopeName;
  }
}
