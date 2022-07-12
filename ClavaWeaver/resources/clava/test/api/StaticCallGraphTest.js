laraImport("clava.graphs.StaticCallGraph");
laraImport("lara.graphs.Graphs");
laraImport("lara.graphs.DotFormatter");

const dotFormatter = new DotFormatter();
dotFormatter.addNodeAttribute("style=dashed", node => Graphs.isLeafNode(node) && !node.data().hasImplementation());
dotFormatter.addNodeAttribute("style=filled", node => Graphs.isLeafNode(node) && node.data().hasCalls());
 

var fullGraph = StaticCallGraph.build().graph;
println("Full graph");
println(Graphs.toDot(fullGraph, dotFormatter));

var functionGraph = StaticCallGraph.build(Query.search("function", "foo1").first(), false).graph;
println("Function graph");
println(Graphs.toDot(functionGraph, dotFormatter));

var functionGraphFull = StaticCallGraph.build(Query.search("function", "foo1").first()).graph;
println("Function graph full");
println(Graphs.toDot(functionGraphFull, dotFormatter));

var callGraphFull = StaticCallGraph.build(Query.search("function", "main").search("call", "foo1").first()).graph;
println("Function graph full");
println(Graphs.toDot(callGraphFull, dotFormatter));

// Get leaf nodes
const fullLeafNodes = fullGraph.nodes().filter(node => Graphs.isLeafNode(node));
println("Full graph leaf nodes: " + fullLeafNodes.map(node => node.data()))

const functionLeafNodes = functionGraph.nodes().filter(node => Graphs.isLeafNode(node));
println("Function graph leaf nodes: " + functionLeafNodes.map(node => node.data()))
println("Function graph leaf nodes with implementation: " + functionLeafNodes.filter(node => node.data().hasCalls()).map(node => node.data()))