laraImport("clava.mpi.MpiScatterGatherLoop");
laraImport("weaver.Query");

const $function = Query.search("function", "foo").first();

const mpiGsl = new MpiScatterGatherLoop(
  Query.searchFrom($function, "loop").first()
);

mpiGsl.execute();

println(Query.root().code);
