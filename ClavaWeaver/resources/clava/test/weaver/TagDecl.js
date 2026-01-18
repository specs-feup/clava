import Query from "@specs-feup/lara/api/weaver/Query.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";

for (const $vardecl of Query.search("function", "structTest").search(
    "vardecl"
)) {
    console.log("Struct: " + $vardecl.type.desugarAll.decl.name);
}

// Test copying struct
for (const $vardecl of Query.search("vardecl", "struct_decl")) {
    const $elaboratedType = $vardecl.type; // Obtain elaborated type

    // Elaborated type can be other things besides structs (e.g., class A, enum B)
    // You can test the type of the elaborated type by using .keyword
    // Obtain the struct itself.
    const $struct = $elaboratedType.desugarAll.decl;

    const $newStruct = $struct.copy(); // Copy struct
    $newStruct.name = "new_struct"; // Change name
    $struct.insertAfter($newStruct); // Insert new struct in the code

    // Create type for new structure, returns an elaboratedType
    const $newStructType = ClavaJoinPoints.structType($newStruct);
    // Create typedef for new structure
    const $typedef = ClavaJoinPoints.typedefDecl($newStructType, "new_typedef");
    // Insert typedef after vardecl
    $vardecl.insertAfter($typedef);
}

console.log("Code:\n" + Query.root().code);
