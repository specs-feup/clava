laraImport("clava.graphs.StaticCallGraph");
laraImport("lara.graphs.Graphs");
laraImport("weaver.Query");
 

var fullGraph = StaticCallGraph.build();
println("Full graph");
println(fullGraph.toDot());

var functionGraph = StaticCallGraph.build(Query.search("function", "foo1").first(), false);
println("Function graph");
println(functionGraph.toDot());

var functionGraphFull = StaticCallGraph.build(Query.search("function", "foo1").first());
println("Function graph full");
println(functionGraphFull.toDot());

var callGraphFull = StaticCallGraph.build(Query.search("function", "main").search("call", "foo1").first());
println("Function graph full");
println(callGraphFull.toDot());

// Get leaf nodes
const fullLeafNodes = fullGraph.graph.nodes().filter(node => Graphs.isLeaf(node));
println("Full graph leaf nodes: " + fullLeafNodes.map(node => node.data()))

const functionLeafNodes = functionGraph.graph.nodes().filter(node => Graphs.isLeaf(node));
println("Function graph leaf nodes: " + functionLeafNodes.map(node => node.data()))
println("Function graph leaf nodes with implementation: " + functionLeafNodes.filter(node => node.data().hasCalls()).map(node => node.data()))