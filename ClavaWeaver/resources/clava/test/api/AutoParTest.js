laraImport("clava.autopar.Parallelize");
laraImport("clava.Clava");
laraImport("weaver.Query");

Parallelize.forLoops();

for (const $omp of Query.search("omp")) {
    console.log("Inserted OpenMP pragma: " + $omp.code);
}
