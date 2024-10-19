import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

export function _MVP_DeclarePointers_($file, name, typeName, typedef, dims) {
    var pointersType = ClavaJoinPoints.constArrayType(typeName, dims);
    $file.addGlobal(name, pointersType, "{}");

    for (const $vardecl of Query.search("file").search("vardecl", name)) {
        $vardecl.insert("before", typedef);
    }
}

export function _MVP_InitPointers_($function, code) {
    for (const $function of Query.search("function")) {
        $function.body.insertBegin(code);
    }
}
