import { FunctionJp } from "../../Joinpoints.js";
import RemoveShadowing from "../code/RemoveShadowing.js";
import SingleReturnFunction from "../pass/SingleReturnFunction.js";

export default function PrepareForInlining($function: FunctionJp) {
  new SingleReturnFunction().apply($function);
  RemoveShadowing($function);
}
