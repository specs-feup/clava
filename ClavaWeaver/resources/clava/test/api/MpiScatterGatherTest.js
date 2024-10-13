import MpiScatterGatherLoop from "@specs-feup/clava/api/clava/mpi/MpiScatterGatherLoop.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

const $function = Query.search("function", "foo").first();

const mpiGsl = new MpiScatterGatherLoop(
  Query.searchFrom($function, "loop").first()
);

// Not yet implemented
//mpiGsl.addInput("a");

mpiGsl.execute();

println(Query.root().code);
