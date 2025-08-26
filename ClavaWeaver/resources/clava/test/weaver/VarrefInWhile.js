import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $loop of Query.search("loop", "while")) {
    const $cond = $loop.cond;
    console.log("while line#" + $loop.line);
    console.log("while cond.code = " + $cond.code);
}

for (const $loop of Query.search("loop", "while")) {
    const $cond = $loop.cond;
    for (const $varref of Query.searchFrom($cond, "varref")) {
        console.log($varref.name);
    }
}
