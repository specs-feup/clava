laraImport("clava.opt.NormalizeToSubset");

// Normalize all code
NormalizeToSubset(Query.root());

// Print code
println(Query.root().code);
