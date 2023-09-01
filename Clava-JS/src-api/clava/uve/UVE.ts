import weaver.Query;
import clava.uve.DetectStream;

var UVE = {};


UVE.linearAnalysis = function(func) {
	println("UVE: detecting streams of function \"" + func.name + "\"");
	for (var i = 0; i < func.params.length; i++) {
		param = func.params[i];
		if (param.type.isArray) {
			println("UVE: checking if array parameter \"" + param.name + "\" is a linear stream");
			DetectStream.detectLinear(func, param);
		}
		else {
			println("UVE: parameter \"" + param.name + "\" is not an array, skipping");
		}
	}
	
}
