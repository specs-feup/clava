import clava.Clava;
import clava.ClavaJoinPoints;

import weaver.Query;

import lara.Io;

var MathHInfo = {};

MathHInfo.getInfo = function() {

	// Save current AST
	Clava.pushAst();

	// Clear AST
	for($file of Query.search("file")) {
		$file.detach;
	}

	// Prepare source file that will test math.h
	var mathTestCode = %{
		#include <math.h>
		double foo() {return abs(-1);}		
	}%;

	var $testFile = ClavaJoinPoints.file("math_h_test.c");
	$testFile.insertBegin(mathTestCode);

	// Compile example code that will allow us to get path to math.h
	Query.root().addFile($testFile);
	Clava.rebuild();

	// Seach for the abs call and obtain math.h file where it was declared
	var $absCall = Query.search("call", "abs").first();
	var mathIncludeFile = $absCall.declaration.filepath;
	
	
	// Clear AST
	for($file of Query.search("file")) {
		$file.detach;
	}

	// Add math.h to the AST
	var $mathFile = ClavaJoinPoints.file("math_copy.h");
	$mathFile.insertBegin(Io.readFile(mathIncludeFile));
	Query.root().addFile($mathFile);
	Clava.rebuild();

	var results = [];
	for(var $mathFunction of Query.search('file', 'math_copy.h').search('function')) {
		var mathInfo = {};

		var paramTypes = [];
		for(var $param of $mathFunction.params) {
			paramTypes.push($param.type.code);
		}

		mathInfo["name"] = $mathFunction.name;
		mathInfo["returnType"] = $mathFunction.type.code;
		mathInfo["paramTypes"] = paramTypes;
			
		
		results.push(mathInfo);
	}


	// Restore original AST
	Clava.popAst();

	return results;
}

MathHInfo.hardcodedFallback = {
    "acos": ["Double"],
    "asin": ["Double"],
    "atan": ["Double"],
    "atan2": ["Double", "Double"],
    "cos": ["Double"],
    "cosh": ["Double"],
    "sin": ["Double"],
    "sinh": ["Double"],
    "tanh": ["Double"],
    "exp": ["Double"],
    "frexp": ["Double", "Int"],
    "ldexp": ["Double", "Int"],
    "log": ["Double"],
    "log10": ["Double"],
    "modf": ["Double", "Double"],
    "pow": ["Double", "Double"],
    "sqrt": ["Double"],
    "ceil": ["Double"],
    "fabs": ["Double"],
    "floor": ["Double"],
    "fmod": ["Double", "Double"],
    "acosf": ["Float"],
    "asinf": ["Float"],
    "atanf": ["Float"],
    "atan2f": ["Float", "Float"],
    "cosf": ["Float"],
    "coshf": ["Float"],
    "sinf": ["Float"],
    "sinhf": ["Float"],
    "tanhf": ["Float"],
    "expf": ["Float"],
    "frexpf": ["Float", "Int"],
    "ldexpf": ["Float", "Int"],
    "logf": ["Float"],
    "log10f": ["Float"],
    "modff": ["Float", "Float"],
    "powf": ["Float", "Float"],
    "sqrtf": ["Float"],
    "ceilf": ["Float"],
    "fabsf": ["Float"],
    "floorf": ["Float"],
    "fmodf": ["Float", "Float"]
};
