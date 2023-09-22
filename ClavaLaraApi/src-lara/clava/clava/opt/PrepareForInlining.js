import RemoveShadowing from "../code/RemoveShadowing.js";
import SingleReturnFunction from "../pass/SingleReturnFunction.js";
export default function PrepareForInlining($function) {
    new SingleReturnFunction().apply($function);
    RemoveShadowing($function);
}
//# sourceMappingURL=PrepareForInlining.js.map