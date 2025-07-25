import Query from "@specs-feup/lara/api/weaver/Query.js";

/**
 * @return static array declarations that have constant size
 */
function getArrayVardecls($jp) {
    const $arrayVardecls = [];

    // Search for variable declarations
    for (const $vardecl of Query.searchFrom($jp, "vardecl")) {
        // Check array types
        if (!$vardecl.type.isArray) {
            continue;
        }

        // Consider only constant array types for now, that have more than two dimensions
        if ($vardecl.type.arraySize < 1) {
            continue;
        }

        if ($vardecl.type.arrayDims.length < 2) {
            continue;
        }

        $arrayVardecls.push($vardecl);
    }

    return $arrayVardecls;
}

function getVardeclUses($vardecl, $vardeclScope) {
    // Get all varrefs in the vardecl scope
    if ($vardeclScope === undefined) {
        $vardeclScope = $vardecl.getAncestor("scope");

        // If scope is still undefined, return
        if ($vardeclScope === undefined) {
            console.log(
                "Could not find a local scope for $vardecl at '" +
                    $vardecl.location +
                    "', global scope not supported"
            );
            return [];
        }
    }

    const $usedVarrefs = [];

    for (const $varref of Query.searchFrom($vardeclScope, "varref")) {
        // Check if the declaration is the same
        if ($vardecl.compareNodes($varref.declaration)) {
            $usedVarrefs.push($varref);
        }
    }

    return $usedVarrefs;
}

function getVardeclInfo($vardecl) {
    const vardeclInfo = {};

    const $function = $vardecl.getAncestor("function");

    if ($function === undefined) {
        console.log(
            "Could not find function of $vardecl '" +
                $vardecl.location +
                "', global variables not supported"
        );
        return;
    }

    vardeclInfo["functionName"] = $function.name;
    vardeclInfo["vardeclName"] = $vardecl.name;

    return vardeclInfo;
}

function linearizeArray(vardeclInfo) {
    // Go to the function where the variable declaration appears
    const queryResult = Query.search("function", vardeclInfo["functionName"])
        .search("vardecl", vardeclInfo["vardeclName"])
        .chain();

    // Check number of results
    if (queryResult.length > 1) {
        console.log(
            "Found more than one candidate for the function/vardecl pair '" +
                vardeclInfo["functionName"] +
                "'/'" +
                vardeclInfo["vardeclName"] +
                "'"
        );
        return;
    }

    const $vardecl = queryResult[0]["vardecl"];

    console.log("DECL: " + $vardecl.location);
}

// Get candidate vardecls
const $arrayVardecls = getArrayVardecls(
    Query.search("function", "main").getFirst()
);

// Check vardecl use
// (not currently being done)
const vardeclsInfo = [];
for (const $arrayVardecl of $arrayVardecls) {
    vardeclsInfo.push(getVardeclInfo($arrayVardecl));
}

// Transform each vardecl
for (const vardeclInfo of vardeclsInfo) {
    linearizeArray(vardeclInfo);
}
