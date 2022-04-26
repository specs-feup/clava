laraImport("clava.graphs.CfgBuilder");
laraImport("weaver.Query");

    for(var $function of Query.search("function")) {


//        try {
			const cfg = CfgBuilder.buildGraph($function);
			println("Cfg for function at " + $function.location + ":");
			println(Graphs.toDot(cfg));
//        } catch(e) {
//            println("Could not generate cfg for scope in location " + $scope.location);
//            //println(e);
//            throw e;
//        }

    }

