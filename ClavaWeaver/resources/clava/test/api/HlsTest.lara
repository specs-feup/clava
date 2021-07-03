import clava.hls.HLSAnalysis;
import weaver.Query;

aspectdef HlsTestTest

	var filter = {joinPointType: "function", 
                  qualifiedName: name => name.endsWith("_Kernel")};
	var $fnKernelArray = Query.search("function", filter).get();
	

	for (var $fnKernel of $fnKernelArray) {
		println($fnKernel.joinPointType + " --> " + $fnKernel.qualifiedName);
		HLSAnalysis.applyGenericStrategies($fnKernel);
	}

	println(Query.root().code);

end
