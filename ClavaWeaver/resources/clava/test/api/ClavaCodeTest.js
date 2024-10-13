import ClavaCode from "@specs-feup/clava/api/clava/ClavaCode.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $pragma of Query.search("pragma", { name: "foo_loop_once" })) {
    console.log("Executes once: " + ClavaCode.isExecutedOnce($pragma.target));
}

for (const $pragma of Query.search("pragma", { name: "foo_loop_not_once" })) {
    console.log(
        "Cannot prove one exec: " + ClavaCode.isExecutedOnce($pragma.target)
    );
}
