import Parallelize from "@specs-feup/clava/api/clava/autopar/Parallelize.js";
import Clava from "@specs-feup/clava/api/clava/Clava.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

Parallelize.forLoops();

for (const $omp of Query.search("omp")) {
    console.log("Inserted OpenMP pragma: " + $omp.code);
}
