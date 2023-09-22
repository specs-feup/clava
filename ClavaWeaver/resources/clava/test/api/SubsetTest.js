laraImport("clava.opt.NormalizeToSubset");
laraImport("weaver.Query");

// Normalize all code
NormalizeToSubset(Query.root());

// Print code
println(Query.root().code);
