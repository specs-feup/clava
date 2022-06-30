laraImport("weaver.Query");
laraImport("clava.opt.NormalizeToSubset");
laraImport("clava.opt.PrepareForInlining");
laraImport("clava.code.Inliner");

function Inlining() {
  NormalizeToSubset(Query.root());

  const inliner = new Inliner();

  for (const $function of Query.search("function", {
    self: ($f) => $f.isCanonical && $f.body.numChildren <= 2,
  })) {
    PrepareForInlining($function);
    for (const $call of Query.search("call", {
      self: ($c) =>
        $c.parent.instanceOf("binaryOp") &&
        $c.parent.isAssignment &&
        $c.parent.parent.instanceOf("exprStmt") &&
        $c.function.name === $function.name,
    })) {
      const $function = $call.ancestor("function");
      println($function.name);

      const $exprStmt = $call.parent.parent;
      inliner.inline($exprStmt);
    }
  }
}
