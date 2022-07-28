laraImport("weaver.Query");
laraImport("clava.opt.NormalizeToSubset");
laraImport("clava.opt.PrepareForInlining");
laraImport("clava.code.Inliner");

/**
 *
 * @param  {object} options - Object with options. Supported options: 'normalizeToSubset' (default: {}), options for function NormalizeToSubset; 'inliner' (default: {}), options for class Inliner
 */
function Inlining(options) {
  _options = options ?? {};
  _options["normalizeToSubset"] ??= {};
  _options["inliner"] ??= {};

  // TODO: Maybe passing a NormalizeToSubset instance is preferrable, but that means making NormalizeToSubset a class instead of a function
  NormalizeToSubset(Query.root(), _options["normalizeToSubset"]);

  const inliner = new Inliner(_options["inliner"]);

  for (const $function of Query.search("function", {
    name: (name) => name !== "main",
  })) {
    PrepareForInlining($function);
  }

  for (const $function of Query.search("function", "main")) {
    inliner.inlineFunctionTree($function);
  }
}
