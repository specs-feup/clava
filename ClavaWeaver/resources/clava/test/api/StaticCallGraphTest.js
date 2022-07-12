laraImport("clava.graphs.StaticCallGraph");

var fullGraph = StaticCallGraph.build().graph;
println("Full graph");
println(Graphs.toDot(fullGraph));

var functionGraph = StaticCallGraph.build(Query.search("function", "foo1").first(), false).graph;
println("Function graph");
println(Graphs.toDot(functionGraph));

var functionGraphFull = StaticCallGraph.build(Query.search("function", "foo1").first()).graph;
println("Function graph full");
println(Graphs.toDot(functionGraphFull));

var callGraphFull = StaticCallGraph.build(Query.search("function", "main").search("call", "foo1").first()).graph;
println("Function graph full");
println(Graphs.toDot(callGraphFull));