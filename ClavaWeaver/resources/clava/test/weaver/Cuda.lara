import clava.ClavaJoinPoints;
import weaver.Query;


aspectdef Cuda

	// Test CUDA kernel call
	for(var $call of Query.search("function", "main").search("cudaKernelCall")) {
		println("kernel call before: " + $call.code);
		//println("Original: " + $call.config.map(node => node.code));
		$call.setConfigFromStrings(["20", "2048"]);
		//println("Modified: " + $call.config.map(node => node.code));		
		println("kernel call after: " + $call.code);
	}
	
	// Find CUDA kernel
	var $cudaKernel = Query.search("function", {isCudaKernel: true}).first();
	println("CUDA kernel: " + $cudaKernel.name);
	
	// Print attributes
	for(var $attr of $cudaKernel.attrs) {
        println("Attr kind: " + $attr.kind);
    }
	
	// Add parameter to CUDA kernel
	$cudaKernel.addParam("bool inst0");
	println("After adding param: " + $cudaKernel.getDeclaration(false));

	// Add argumento to call
	var $cudaKernelCall = Query.search("function", "main").search("cudaKernelCall").first();
	$cudaKernelCall.addArg("false", ClavaJoinPoints.typeLiteral("bool"));
	println("Call after adding arg: " + $cudaKernelCall.code);
	
	// Get properties
	for(var $varref of Query.searchFrom($cudaKernel, "varref", {hasProperty: true})) {
		println("Varref: " + $varref.name);
		println("Cuda index: " + $varref.property);		
	}

end
