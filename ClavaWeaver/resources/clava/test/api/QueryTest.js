import Query from "@specs-feup/lara/api/weaver/Query.js";
import { getKeys } from "@specs-feup/lara/api/lara/core/LaraCore.js";

console.log("query_loop 1");
for(const query of Query.search("function", "query_loop").search("loop").search("loop").chain()) {
	console.log("loop:" + query["loop"].rank);
}

console.log("query_loop 2");	
//for(const query of Query.search("function", "query_loop").search("loop").children("scope").children("loop").chain()) {
//for(const query of Query.search("function", "query_loop").search("loop").children("loop").chain()) {
for(const query of Query.search("function", "query_loop").search("loop").scope("loop").chain()) {
	console.log("loop:" + query["loop"].rank);
}	

console.log("query_loop 3");
//for(const query of Query.search("function", "query_loop").search("loop", {"isOutermost": true}).children("scope").children("loop").chain()) {
//for(const query of Query.search("function", "query_loop").search("loop", {"isOutermost": true}).children("loop").chain()) {
for(const query of Query.search("function", "query_loop").search("loop", {"isOutermost": true}).scope("loop").chain()) {
	console.log("loop:" + query["loop"].rank);
}

console.log("query_empty 1");
for(const query of Query.search("function", "query_empty").scope().chain()) {		
	console.log("joinpoint: " + query["joinpoint"].joinPointType);				
}	


for(const query of Query.search("function", "query_loop").search("loop").search("loop").search("loop").chain()) {
	console.log("chain keys: " + getKeys(query).sort());
}

for(const query of Query.search("function", /_regex/)) {
	console.log("regex: " + query.name);
}

