laraImport("clava.Clava");
laraImport("clava.pass.TransformSwitchToIf");

laraImport("weaver.Query");

const switchTransformer = new TransformSwitchToIf(true);

for (const $switch of Query.search("switch")) {
    switchTransformer.transformJoinpoint($switch);
}

Clava.rebuild();
println(Query.search("function", "foo").first().code);