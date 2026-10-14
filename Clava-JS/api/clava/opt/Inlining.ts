import Query from "@specs-feup/lara/api/weaver/Query.ts";
import { FunctionJp, Joinpoint } from "../../Joinpoints.ts";
import Inliner from "../code/Inliner.ts";
import NormalizeToSubset from "./NormalizeToSubset.ts";
import PrepareForInlining from "./PrepareForInlining.ts";

/**
 *
 * @param options - Object with options. See default value for supported options
 */
export default function Inlining(
  options = {
    normalizeToSubset: { simplifyLoops: { forToWhile: true } },
    inliner: {},
  }
) {
  // TODO: Maybe passing a NormalizeToSubset instance is preferrable, but that means making NormalizeToSubset a class instead of a function
  NormalizeToSubset(Query.root() as Joinpoint, options.normalizeToSubset);

  const inliner = new Inliner(options.inliner);

  for (const $function of Query.search(FunctionJp, {
    name: (name: string) => name !== "main",
    isImplementation: true, // Only inline if function has a body
  })) {
    PrepareForInlining($function);
  }

  for (const $function of Query.search(FunctionJp, "main")) {
    inliner.inlineFunctionTree($function);
  }
}
