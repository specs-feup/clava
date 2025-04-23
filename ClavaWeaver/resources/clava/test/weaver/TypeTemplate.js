import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

const floatType = ClavaJoinPoints.builtinType("float");
const intType = ClavaJoinPoints.builtinType("int");
const doubleType = ClavaJoinPoints.builtinType("double");

//// Vector

// Get template argument types
for (const $vardecl of Query.search("file").search("vardecl", "W")) {
    console.log("Original template args: " + $vardecl.type.templateArgsStrings);

    const typesStrings = [];
    for (const $type of $vardecl.type.templateArgsTypes) {
        typesStrings.push($type.code);
    }
    console.log("Original template args types: " + typesStrings.join(", "));
}

// Set a single template argument
for (const $vardecl of Query.search("file").search("vardecl", "W")) {
    $vardecl.type.setTemplateArgType(0, floatType);
    console.log("After setting float: " + $vardecl.type.templateArgsStrings);
}

// Set all template arguments
for (const $vardecl of Query.search("file").search("vardecl", "W")) {
    $vardecl.type.templateArgsTypes = [intType];
    console.log(
        "After setting int with array: " + $vardecl.type.templateArgsStrings
    );
}

//// Map

// Get template argument types
for (const $vardecl of Query.search("file").search("vardecl", "map")) {
    console.log("Original template args: " + $vardecl.type.templateArgsStrings);
}

// Set a single template argument
for (const $vardecl of Query.search("file").search("vardecl", "map")) {
    $vardecl.type.setTemplateArgType(1, doubleType);
    console.log(
        "After second arg to double: " + $vardecl.type.templateArgsStrings
    );
}

// Set all template arguments
for (const $vardecl of Query.search("file").search("vardecl", "map")) {
    $vardecl.type.templateArgsTypes = [doubleType, intType];
    console.log(
        "After setting with array [double, int]: " +
            $vardecl.type.templateArgsStrings
    );
}

// Change typedef declaration
for (const $typedefDecl of Query.search("typedefDecl", "typedef_to_change")) {
    $typedefDecl.type.templateArgsTypes = [floatType];
    console.log("After setting typedef_to_change: " + $typedefDecl.type.code);
}

// Change type of vardecl which is a typedef
for (const $vardecl of Query.search("vardecl", "changed_typedef_type")) {
    $vardecl.type.templateArgsTypes = [intType];
    if ($vardecl.type.kind === "TypedefType") {
        $vardecl.type = $vardecl.type.desugar;
    }

    console.log("After setting changed_typedef_type: " + $vardecl.type.code);
}

// Def on arrays
for (const $vardecl of Query.search("function", "type_params").search(
    "vardecl",
    "intVector"
)) {
    const arrayType = ClavaJoinPoints.constArrayType("float", 1, 2);
    $vardecl.type.templateArgsTypes = [doubleType, arrayType];
}
