import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $omp of Query.search("omp")) {
    console.log("kind: " + $omp.kind);
    console.log("clause kinds:" + $omp.clauseKinds);
    console.log("has shared:" + $omp.hasClause("shared"));
    console.log("nowait legal:" + $omp.isClauseLegal("nowait"));
    console.log("num_threads:" + $omp.numThreads);
    console.log("proc_bind:" + $omp.procBind);
    console.log("private:" + $omp.private);
    console.log("reduction +:" + $omp.getReduction("+"));
    console.log("reduction max:" + $omp.getReduction("MAX"));
    console.log("reduction kinds:" + $omp.reductionKinds);
    console.log("firstprivate:" + $omp.firstprivate);
    console.log("lastprivate:" + $omp.lastprivate);
    console.log("shared:" + $omp.shared);
    console.log("copyin:" + $omp.copyin);
    console.log("schedule kind:" + $omp.scheduleKind);
    console.log("schedule chunk size:" + $omp.scheduleChunkSize);
    console.log("schedule modifiers:" + $omp.scheduleModifiers);
    console.log("collapse:" + $omp.collapse);
    console.log("ordered:" + $omp.hasClause("ordered"));
    console.log("ordered value:" + $omp.ordered);
}

for (const $omp of Query.search("omp", "for")) {
    $omp.insertAfter(ClavaJoinPoints.omp("master"));
    $omp.target.insertAfter(ClavaJoinPoints.omp("barrier"));
    console.log($omp.getAncestor("function").body.code);
}
