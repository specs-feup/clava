import StaticCallGraph from "@specs-feup/clava/api/clava/graphs/StaticCallGraph.js";
import Graphs from "@specs-feup/lara/api/lara/graphs/Graphs.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
 

var fullGraph = StaticCallGraph.build();
console.log("Full graph");
console.log(fullGraph.toDot());

var functionGraph = StaticCallGraph.build(Query.search("function", "foo1").first(), false);
console.log("Function graph");
console.log(functionGraph.toDot());

var functionGraphFull = StaticCallGraph.build(Query.search("function", "foo1").first());
console.log("Function graph full");
console.log(functionGraphFull.toDot());

var callGraphFull = StaticCallGraph.build(Query.search("function", "main").search("call", "foo1").first());
console.log("Function graph full");
console.log(callGraphFull.toDot());

// Get leaf nodes
const fullLeafNodes = fullGraph.graph.nodes().filter(node => Graphs.isLeaf(node));
console.log("Full graph leaf nodes: " + fullLeafNodes.map(node => node.data()))

const functionLeafNodes = functionGraph.graph.nodes().filter(node => Graphs.isLeaf(node));
console.log("Function graph leaf nodes: " + functionLeafNodes.map(node => node.data()))
console.log("Function graph leaf nodes with implementation: " + functionLeafNodes.filter(node => node.data().hasCalls()).map(node => node.data()))