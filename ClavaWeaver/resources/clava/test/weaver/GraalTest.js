laraImport("weaver.Query");

println("Root type: " + Query.root().joinPointType);
println("Root: " + Query.root());
println("Is join point: " + isJoinPoint(Query.root()));
println("Is program: " + isJoinPoint(Query.root(), "program"));

const root = Query.root();
const $file = root.children[0];
const $function = $file.children[0];

println("First grandchild: " + $function.joinPointType);
println("Ancestor that exists: " + $function.ancestor("file").joinPointType);
println("Ancestor that does not exist: " + $function.ancestor("expr"));

for(const $f of Query.search("function")) {
	println("Function name: " + $f.name);
}

//println("Function: " + Query.search("function").first().joinPointType);
