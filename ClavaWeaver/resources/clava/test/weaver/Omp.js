import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $omp of Query.search("omp")) {
    console.log("OMP:" + $omp.name);
    console.log("OMP CONTENT:" + $omp.content);
    console.log("OMP TARGET:" + $omp.target.code);
}
