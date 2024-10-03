laraImport("clava.Clava");
laraImport("weaver.Query");

const $foo = Query.search("function", "foo").first();

const $foo2 = $foo.cloneOnFile("foo2", "bar/newFile.cpp");

console.log($foo2.getAncestor("file").includes.map((inc) => inc.code));
