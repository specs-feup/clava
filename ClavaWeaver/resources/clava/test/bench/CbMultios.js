laraImport("weaver.Query");
laraImport("clava.MathExtra");

/**
 * Based on the challenges in https://github.com/trailofbits/cb-multios/
 */
// Entry point
const $function = Query.search("function", "bitBlaster").first();

// Look for casts to pointer type
const candidateCasts = Query.searchFrom($function, "cast", {
    type: (type) => type.isPointer,
}).get();

// Try to determine in expression can be statically determined to be null or 0
for (const $cast of candidateCasts) {
    // Get expression being cast
    const $expr = $cast.subExpr;

    // Simplify expression
    const simplifiedExpr = MathExtra.simplify($expr);

    if (simplifiedExpr === "0" || simplifiedExpr === "nullptr") {
        console.log(
            "Found possible NULL pointer dereference in " +
                $cast.location +
                " (CWE-476)"
        );
    }
}

// Alternatively, code that tests for null pointer could be inserted (if it is detected that such tests is not being done)
