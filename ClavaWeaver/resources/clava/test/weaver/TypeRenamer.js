laraImport("clava.ClavaType");
laraImport("weaver.Query");

const $fn = Query.search("function", "inputInCast").first();

for (const $vardecl of Query.searchFrom($fn.body, "vardecl")) {
    const varrefs = [];

    const $typeCopy = ClavaType.getVarrefsInTypeCopy($vardecl.type, varrefs);

    for (const $varref of varrefs) {
        const $vdecl = $varref.declaration;
        if ($vdecl.name.endsWith("_renamed")) {
            continue;
        }

        $vdecl.name = $vdecl.name + "_renamed";
    }

    console.log("Original type: " + $vardecl.type.code);
    $vardecl.type = $typeCopy;
}

console.log(Query.root().code);