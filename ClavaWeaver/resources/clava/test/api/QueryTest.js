import Query from "@specs-feup/lara/api/weaver/Query.js";


	
	println("query_loop 1");
	for(var query of Query.search("function", "query_loop").search("loop").search("loop").chain()) {
		println("loop:" + query["loop"].rank);
	}

	println("query_loop 2");	
	//for(var query of Query.search("function", "query_loop").search("loop").children("scope").children("loop").chain()) {
	//for(var query of Query.search("function", "query_loop").search("loop").children("loop").chain()) {
	for(var query of Query.search("function", "query_loop").search("loop").scope("loop").chain()) {
		println("loop:" + query["loop"].rank);
	}	

	println("query_loop 3");
	//for(var query of Query.search("function", "query_loop").search("loop", {"isOutermost": true}).children("scope").children("loop").chain()) {
	//for(var query of Query.search("function", "query_loop").search("loop", {"isOutermost": true}).children("loop").chain()) {
	for(var query of Query.search("function", "query_loop").search("loop", {"isOutermost": true}).scope("loop").chain()) {
		println("loop:" + query["loop"].rank);
	}

	println("query_empty 1");
	for(var query of Query.search("function", "query_empty").scope().chain()) {		
		println("joinpoint: " + query["joinpoint"].joinPointType);				
	}	


	for(var query of Query.search("function", "query_loop").search("loop").search("loop").search("loop").chain()) {
		println("chain keys: " + getKeys(query).sort());
	}
	
	for(var query of Query.search("function", /_regex/)) {
		println("regex: " + query.name);
	}

