import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $loop of Query.search("loop")) {
    $loop.insertBefore("#pragma omp parallel for");
}

for (const $omp of Query.search("omp")) {
    if ($omp.target.isInnermost) {
        $omp.detach();
    }
}

console.log(Query.root().code);
