laraImport("lcl.LaraCommonLanguage");
laraImport("weaver.jp.ClavaJoinPoint");
laraImport("weaver.jp.FileJp");
laraImport("weaver.Query");

	var $file = Query.search("file").first();
	//printlnObject($file);
	println("line: " + $file.line);
	println("is FileJP? : " + ($file instanceof FileJp));

	var classes = Query.search("classType", "A").get();
	println("# A classes: " + classes.length);

	var classesOnlyDecl = Query.search("classType", "classOnlyDecl").get();
	println("# onlyDecl classes: " + classesOnlyDecl.length);
	
	var fooFunctions = Query.search("function", "foo").get();
	println("# foo functions: " + fooFunctions.length);

	var fooOnlyDeclFunctions = Query.search("function", "fooOnlyDecl").get();
	println("# fooOnlyDecl functions: " + fooOnlyDeclFunctions.length);

	
	var classes = Query.search("classType", "B").get();
	println("# B classes: " + classes.length);
	
	var classes = Query.search("classType", "C").get();
	println("# C classes: " + classes.length);