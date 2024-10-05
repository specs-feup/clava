import lara.util.JpFilter;
import weaver.Query;
import weaver.Weaver;

aspectdef JpFilterTest

	var $records = [];
	select record end
	apply
		$records.push($record);
	end

	
	var filter1 = new JpFilter({name: 'A'});
	println(convert(filter1.filter($records)));
	
	var filter2 = new JpFilter({name: /A/});
	println(convert(filter2.filter($records)));
	
	var filter3 = new JpFilter({name: /A/, kind: 'class'});
	println(convert(filter3.filter($records)));

	var filter4 = new JpFilter({name: /A/});
	println(convert(filter4.filter($records)));
	
	var filter5 = new JpFilter({name: /A/, line: function(line){return line > 7;}});
	println(convert(filter5.filter($records)));
	
	var $jps = Query.search("record", {name: /A/}).search("field", {name: 'x'}).get();
	
	for(var $selected of $jps) {
		println("Select function: " + $selected.code);
	}
	
	//println("FILTER:");
	//printObject(filter2);


	// Test search inclusive
	var $foo2 = Query.search("function", {name: "foo2"}).first();
	var $secondFoo2 = Query.searchFromInclusive($foo2, "function", {name: "foo2"}).first();
	println("Search inclusive: " + $secondFoo2.name);

	// Test retrieving default attribute of join point
	println("Default attribute of 'function': " + Weaver.getDefaultAttribute("function"));
	println("Search with default: " + Query.search("function", "foo2").first().name);

end

function convert($jps) {
	var string = "";
	
	var first = true;
	for(var $jp of $jps) {
		//println("JP:" + $jp);
		//println("JP NAME:" + $jp.name);	
		if(first) {
			first = false;
		} else {
			string += ", ";
		}
		
		string += $jp.name;
	}
	
	return string;
	//return $jps.map(jpName).join(", ");
}

function jpName($jp) {
	return $jp.name;
}