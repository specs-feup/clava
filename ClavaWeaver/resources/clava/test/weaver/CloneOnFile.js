import Clava from "@specs-feup/clava/api/clava/Clava.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

const $foo = Query.search("function", "foo").first();

const $foo2 = $foo.cloneOnFile("foo2", "bar/newFile.cpp");

console.log($foo2.getAncestor("file").includes.map((inc) => inc.code));
