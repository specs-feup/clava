import Query from "@specs-feup/lara/api/weaver/Query.js";

//laraImport("weaver.Query");
laraImport("Joinpoints");
//import {FunctionJp, Statement} from "@specs-feup/clava/api/Joinpoints.js";
laraImport("clava.code.StatementDecomposer");
laraImport("clava.opt.NormalizeToSubset");

for (const fun of Query.search(FunctionJp, {"isImplementation": true})) {
    const body = fun.body;
    NormalizeToSubset(body, {simplifyLoops: {forToWhile: false}});
}

const decomp = new StatementDecomposer();
for (var stmt of Query.search(Statement, {isInsideHeader: false})) {
    decomp.decomposeAndReplace(stmt);
}

println(Query.root().code);