laraImport("lcl.LaraCommonLanguage");
laraImport("weaver.jp.ClavaJoinPoint");
laraImport("weaver.jp.FileJp");
laraImport("weaver.Query");

	var $file = Query.search("file").first();
	//printlnObject($file);
	println("line: " + $file.line);
	println("is FileJP? : " + ($file instanceof FileJp));

	var $class = Query.search("classType").first();
	println("Class name: " + $class.name);
