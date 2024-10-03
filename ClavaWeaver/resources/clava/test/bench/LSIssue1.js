import weaver.Query;
import clava.ClavaJoinPoints;
import clava.hls.HLSAnalysis;
import clava.Clava;

aspectdef OCL_Experiments
	var $t = Query.search("function", {qualifiedName: name => name.endsWith("_Kernel")}).first();
	var $nt = $t.clone($t.name+"C", false);
	$t.insertBefore($nt);
	var $callArgs = $t.params.map(($param) =>
        ClavaJoinPoints.varRefFromDecl($param)
    );
	var $call = $nt.newCall($callArgs);
	$t.body.replaceWith($call);


	var $newCall = Query.search("call", "matrix_mult_KernelC").first();
	println("Def: " + $newCall.definition.name);
/*
	var $kernels = Query.search("function", {
		joinPointType: "function",
		qualifiedName: name => name.endsWith("lt_Kernel")
	}).get();
		
	for (var $kernel of $kernels) {
		println("\n\n\n");
		println($kernel.joinPointType + " --> " + $kernel.qualifiedName);
		HLSAnalysis.applyGenericStrategies($kernel);
	}
	*/
	/*
	for(var $f of Query.search("function")) {
		println("Function: " + $f.qualifiedName);
		println("Ends with lt_Kernel? " + $f.qualifiedName.endsWith("lt_Kernel"));
	}
	*/
	
	//println(Query.root().code);
end