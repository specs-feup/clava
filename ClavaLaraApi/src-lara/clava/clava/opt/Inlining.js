import Query from "@specs-feup/lara/api/weaver/Query.js";
import { FunctionJp } from "../../Joinpoints.js";
import Inliner from "../code/Inliner.js";
import NormalizeToSubset from "./NormalizeToSubset.js";
import PrepareForInlining from "./PrepareForInlining.js";
/**
 *
 * @param options - Object with options. See default value for supported options
 */
export default function Inlining(options = {
    normalizeToSubset: { simplifyLoops: { forToWhile: true } },
    inliner: {},
}) {
    // TODO: Maybe passing a NormalizeToSubset instance is preferrable, but that means making NormalizeToSubset a class instead of a function
    NormalizeToSubset(Query.root(), options.normalizeToSubset);
    const inliner = new Inliner(options.inliner);
    for (const $function of Query.search(FunctionJp, {
        name: (name) => name !== "main",
        isImplementation: true, // Only inline if function has a body
    })) {
        PrepareForInlining($function);
    }
    for (const $function of Query.search(FunctionJp, "main")) {
        inliner.inlineFunctionTree($function);
    }
}
//# sourceMappingURL=Inlining.js.map