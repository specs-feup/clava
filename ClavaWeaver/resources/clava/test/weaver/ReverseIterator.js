import weaver.Query;

aspectdef ReverseIterator

	var cl = Query.search("class","json_reverse_iterator").first();
	println("Bases: " + cl.bases.map(base => base.name));

	

end
