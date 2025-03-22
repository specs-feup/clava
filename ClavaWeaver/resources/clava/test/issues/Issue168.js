import Query from "@specs-feup/lara/api/weaver/Query.js";
import StatementDecomposer from "@specs-feup/clava/api/clava/code/StatementDecomposer.js";
import NormalizeToSubset from "@specs-feup/clava/api/clava/opt/NormalizeToSubset.js";
import { FunctionJp, Statement } from "@specs-feup/clava/api/Joinpoints.js";

for (const fun of Query.search(FunctionJp, { isImplementation: true })) {
    const body = fun.body;
    NormalizeToSubset(body, { simplifyLoops: { forToWhile: false } });
}

const decomp = new StatementDecomposer();
for (const stmt of Query.search(Statement, { isInsideHeader: false })) {
    decomp.decomposeAndReplace(stmt);
}

console.log(Query.root().code);
