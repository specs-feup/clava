import { FunctionJp } from "../../Joinpoints.ts";
import RemoveShadowing from "../code/RemoveShadowing.ts";
import SingleReturnFunction from "../pass/SingleReturnFunction.ts";

export default function PrepareForInlining($function: FunctionJp) {
  new SingleReturnFunction().apply($function);
  RemoveShadowing($function);
}
