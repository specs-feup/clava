import Query from "lara-js/api/weaver/Query.js";
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
    for (const $jp of Query.search("function", {
        name: (name) => name !== "main",
        isImplementation: true, // Only inline if function has a body
    })) {
        const $function = $jp;
        PrepareForInlining($function);
    }
    for (const $jp of Query.search("function", "main")) {
        const $function = $jp;
        inliner.inlineFunctionTree($function);
    }
}
//# sourceMappingURL=Inlining.js.map