laraImport("weaver.Query");
laraImport("clava.opt.NormalizeToSubset");
laraImport("clava.opt.PrepareForInlining");
laraImport("clava.code.Inliner");

function Inlining() {
  NormalizeToSubset(Query.root());

  const inliner = new Inliner();

  for (const $function of Query.search("function", {
    name: (name) => name !== "main",
  })) {
    PrepareForInlining($function);
  }

  for (const $function of Query.search("function", "main")) {
    inliner.inlineFunctionTree($function);
  }
}
