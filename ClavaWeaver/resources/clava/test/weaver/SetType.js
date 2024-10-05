laraImport("clava.ClavaJoinPoints");
laraImport("weaver.Query");

const doubleType = ClavaJoinPoints.builtinType("double");
for (const $function of Query.search("file").search("function", "fooType")) {
    $function.type = doubleType;
}

// Deep Copy test for types
for (const $vardecl of Query.search("function", "deepCopyTest").search(
    "vardecl"
)) {
    // Deep copy on first vardecl
    const typeCopy = $vardecl.type.deepCopy();
    $vardecl.type = typeCopy;

    // Change element type
    typeCopy.elementType.elementType.setValue("elementType", doubleType);

    // Stop
    break;
}

console.log(Query.root().code);