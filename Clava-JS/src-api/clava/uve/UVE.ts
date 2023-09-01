import { FunctionJp } from "../../Joinpoints.js";
import DetectStream from "./DetectStream.js";

export default class UVE {
  static linearAnalysis(func: FunctionJp): void {
    console.log(`UVE: detecting streams of function "${func.name}"`);
    for (let i = 0; i < func.params.length; i++) {
      const param = func.params[i];
      if (param.type.isArray) {
        console.log(
          `UVE: checking if array parameter "${param.name}" is a linear stream`
        );
        DetectStream.detectLinear(func, param);
      } else {
        console.log(
          `UVE: parameter "${param.name}" is not an array, skipping`
        );
      }
    }
  }
}
