import { laraImport } from "@specs-feup/lara/api/lara/core/LaraCore.js";

laraImport("lcl.LaraCommonLanguage");
laraImport("weaver.jp.ClavaJoinPoint");
laraImport("weaver.jp.FileJp");
laraImport("weaver.Query");

var $file = Query.search("file").first();
//printlnObject($file);
console.log("line: " + $file.line);
console.log("is FileJP? : " + ($file instanceof FileJp));

var classes = Query.search("classType", "A").get();
console.log("# A classes: " + classes.length);

var classesOnlyDecl = Query.search("classType", "classOnlyDecl").get();
console.log("# onlyDecl classes: " + classesOnlyDecl.length);

var fooFunctions = Query.search("function", "foo").get();
console.log("# foo functions: " + fooFunctions.length);

var fooOnlyDeclFunctions = Query.search("function", "fooOnlyDecl").get();
console.log("# fooOnlyDecl functions: " + fooOnlyDeclFunctions.length);

var bClasses = Query.search("classType", "B").get();
console.log("# B classes: " + bClasses.length);

var cClasses = Query.search("classType", "C").get();
console.log("# C classes: " + cClasses.length);

var dCalls = Query.search("classType", "D").search("call").get();
console.log("# calls in D: " + dCalls.length);
