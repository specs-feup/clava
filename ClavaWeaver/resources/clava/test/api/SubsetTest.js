import NormalizeToSubset from "@specs-feup/clava/api/clava/opt/NormalizeToSubset.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

// Normalize all code
NormalizeToSubset(Query.root());

// Print code
console.log(Query.root().code);
