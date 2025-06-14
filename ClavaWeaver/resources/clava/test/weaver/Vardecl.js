import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $vardecl of Query.search("vardecl")) {
    console.log("vardecl " + $vardecl.name);
    console.log("is param? " + $vardecl.isParam);
}

for (const $call of Query.search("call")) {
    if ($call.declaration !== undefined) {
        console.log(
            "Call '" + $call.name + "' declaration:" + $call.declaration.line
        );
    }

    if ($call.definition !== undefined) {
        console.log(
            "Call '" + $call.name + "' definition:" + $call.definition.line
        );
    }
}
