import { Joinpoint } from "../../../Joinpoints.ts";
import AnalyserResult from "../AnalyserResult.ts";
import Fix from "../Fix.ts";

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
