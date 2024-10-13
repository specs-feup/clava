import JavaTypes from "@specs-feup/lara/api/lara/util/JavaTypes.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

const tic = JavaTypes.LaraI.getThreadLocalLarai().getWeaver().currentTime();
let acc = 0;

let toc;
for (const $function of Query.search("function", { name: "exact_rhs" })) {
    for (const _ of Query.searchFrom($function.body, "varref")) {
        toc = JavaTypes.LaraI.getThreadLocalLarai()
            .getWeaver()
            .currentTime();
        console.log("Time:" + (toc - tic));
        acc += 1;
    }
}

const toc2 = JavaTypes.LaraI.getThreadLocalLarai().getWeaver().currentTime();
console.log("Time 2:" + (toc2 - toc));

console.log("Found " + acc + " variables");
