import weaver.WeaverJps;
import clava.ClavaJoinPoints;
import clava.Clava;
import lara.Io;
import lara.Csv;
import clava.hls.MathHInfo;

var MathAnalysis = {};


MathAnalysis.fullAnalysis = function(name, report) {
	//Get functions from math.h
	//If unable to read math.h, use a fallback
	var mathFun = MathHInfo.getInfo();
	if (mathFun.length < 3)
		mathFun = MathHInfo.hardcodedFallback;
	
	if (report) {
		call MathReport(mathFun, true, name);
	} else {
		call MathReport(mathFun, false, "");
		call MathCompare(mathFun);
		call MathReplace(mathFun);
	}
}

aspectdef MathReport
	input
		mathFun,
		csv,
		name
	end
	
	//Counter for occurences of each math.h function
	var occurrences = {};
	for (var f in mathFun) {
		occurrences[f] = 0;
	}
	
	//Count occurrences
	for (var elem of WeaverJps.search('call').chain()) {
		var fun = elem['call'].name;
		if (fun in occurrences)
			occurrences[fun] += 1;
	}

	//Report occurrences
	if (csv) {
		var txt = "";
		if (Io.isFile("MathAnalysis.csv")) {
			var lines = Io.readLines("MathAnalysis.csv");
			for (var i = 0; i < lines.length; i++)
				txt += lines[i] + "\n";
		} else {
			txt = "source file,";
			for (var f in occurrences)
				txt += f + ",";
			txt += "\n";
		}
		txt += name + ",";
		for (var f in occurrences)
			txt += occurrences[f] + ",";
		txt += "\n";
		println(txt);
		Io.writeFile("MathAnalysis.csv", txt);
	} else {
		println("Occurrences of each math.f function (not showing functions with 0 occurrences):");
		for (var f in occurrences) {
			if (occurrences[f] > 0)
				println(f + ": " + occurrences[f]);
		}
		println("");
	}
end

aspectdef MathCompare
	input
		mathFun
	end

	//Compare, for each call, the type of arguments and the function signature
	println("Type of arguments being passed to each call to a math.h function:");
	for (var elem of WeaverJps.search('call').chain()) {
		var fun = elem['call'].name;
		if (fun in mathFun) {
			var args = elem['call'].args;
			var types = [];
			for (var i = 0; i < args.length; i++)
				types.push(args[i].type.builtinKind);
			println("-----------\nSig: " + elem['call'].signature/*mathFun[fun]*/ + "\nArgs: " + types);
		}
	}
	println("");
end

/**
 * 		Applies a series of mathemtatical function replacements using the following rules:
 * - For every function with a signature requiring args of type "double" but with the 
 * provided args of type "float", it replaces the function by its "float" equivalent, 
 * if present on the provided math.h info implementation 
 * - If a square root is found, it is replaced by a fast inverse square root and a multiplication,
 * for both "double" and "float" types, as required. The implementation of the functions is added
 * to the source file
 * - Replaces calls to the "pow" function using an integer exponent with a series of multiplications
 *
 * @param {JSON} mathFun - A JSON holding information about the signature of all functions 
 * present on a math.h. Can be obtained using clava.hls.MathHInfo, or otherwise built manually
 * (see documentation of clava.hls.MathHInfo for the format)
 * */
aspectdef MathReplace
	input
		mathFun
	end

	var includedSqrt = false;

	for (var elem of WeaverJps.search('call').chain()) {
		var fun = elem['call'].name;
		if (fun in mathFun) {
			var args = elem['call'].args;
			var types = [];
			for (var i = 0; i < args.length; i++)
				types.push(args[i].type.builtinKind);

			//sqrtf -> rsqrt32
			if (fun == "sqrtf" || (fun == "sqrt" && !types.includes("Double", 0))) {
				println("Replacing sqrtf for rsqrt32");
				elem['call'].setName("mult_rsqrt32");	
			}
			//sqrt -> rsqrt64
			if (fun == "sqrt" && types.includes("Double", 0)) {
				println("Replacing sqrt for sqrt64");
				elem['call'].setName("mult_rsqrt64");
				
			}
			//pow(d, Int) -> d * ... * d
			if ((fun == "pow" || fun == "powf") && types[1] == "Int" && /[0-9]\d*/.test(args[1].code)) {
				println("Replacing " + fun + " for explicit multiplication");
				var n = parseInt(args[1].code);
				var expr = ClavaJoinPoints.parenthesis(args[0]);

				if (n >= 2) {
					node = ClavaJoinPoints.binaryOp("mul", expr.copy(), expr.copy(), types[0]);
					for (var i = 3; i <= n; i++) {
						node = ClavaJoinPoints.binaryOp("mul", expr.copy(), node, types[0]);
					}
					elem['call'].replaceWith(node);
				}
				if (n == 1) {
					elem['call'].replaceWith(expr);
				}
				if (n == 0) {
					elem['call'].replaceWith(ClavaJoinPoints.integerLiteral(1));
				}
			}
			//conversion from using functions with doubles to floats	
			var isFloat = fun.substr(fun.length - 1) == "f";
			if (!isFloat && !fun.includes("sqrt", 0)) {
				var newFun = fun + "f";
				if (newFun in mathFun) {
					elem['call'].setName(newFun);
					println("Replacing " + fun + " for " + newFun);
				}
			}
		}
	}
end


codedef rsqrt %{

static inline float mult_rsqrt32(float number) {
	uint32_t i;
	float x2, y;
	x2 = number * 0.5F;
	y = number;
	i = * (uint32_t *) &y;
	i = 0x5f375a86 - (i >> 1);
	y = *(float *) &i;
	y = y * (1.5F - (x2 * y * y));
	return y * number;
}

static inline double mult_rsqrt64(double number) {
	uint64_t i;
	double x2, y;
	x2 = number * 0.5;
	y = number;
	i = * (uint64_t *) &y;
	i = 0x5fe6eb50c7b537a9 - (i >> 1);
	y = * (double *) &i;
	y = y * (1.5 - (x2 * y * y));
	return y * number;
}
}%
end