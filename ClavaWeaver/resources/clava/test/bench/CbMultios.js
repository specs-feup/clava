import weaver.Query;
import clava.MathExtra;

/**
 * Based on the challenges in https://github.com/trailofbits/cb-multios/
 */
aspectdef CbMultios

//println(Query.search("function", "bitBlaster").first().ast);

	testBitBlaster();
end


function testBitBlaster() {
	// Entry point
	var $function = Query.search("function", "bitBlaster").first();
	
	// Look for casts to pointer type
	var candidateCasts = Query.searchFrom($function, "cast", {type: type => type.isPointer}).get();
	

	// Try to determine in expression can be statically determined to be null or 0
	for(var $cast of candidateCasts) {
		// Get expression being cast
		var $expr = $cast.subExpr;

		// Simplify expression
		var simplifiedExpr = MathExtra.simplify($expr);
		
		if(simplifiedExpr === "0" || simplifiedExpr === "nullptr") {
			println("Found possible NULL pointer dereference in " + $cast.location + " (CWE-476)");
		}		
	}
	
	// Alternatively, code that tests for null pointer could be inserted (if it is detected that such tests is not being done)

}
