import weaver.Query;

aspectdef Test

	for(var chain of Query.search("function").search("call").chain()) {
		var $function = chain["function"];
		var $call = chain["call"];
		
		println($function.name + " -> " + $call.name);
		println($function.location + " -> " + $call.location);		
		var location = $call.decl.location;
		if(!location.includes(".h")) {
			println(location);
		}

/*
    for(var $function of Query.search("function")) {


        try {
			println($function.body.cfg);
        } catch(e) {
            println("Could not generate cfg for scope in location " + $scope.location);
            println(e);
        }
*/
    }

end