import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

const $vardecl = Query.search("function", "multiMatrix")
    .search("vardecl", "s_a")
    .first();
for (const $attr of $vardecl.attrs) {
    console.log("Attr kind: " + $attr.kind);
    console.log("Attr code: " + $attr.code);
}
