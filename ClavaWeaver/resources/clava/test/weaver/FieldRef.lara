import weaver.Query;

aspectdef FieldRef

	var cl = Query.search("class", "Foo3").first();
	println("CLASS: " + cl.ast);
	println("Number of Fields:");
	println(Query.searchFrom(cl, "field").get().length);
	println(Query.searchFromInclusive(cl, "field").get().length);
	println("Number of Classes:");
	println(Query.searchFrom(cl, "class").get().length);
	println(Query.searchFromInclusive(cl, "class").get().length);

end
