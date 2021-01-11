import weaver.Query;

aspectdef TemplateSpecializationType

	for(var $class of Query.search("class")) {
		println("Class: " + $class.name);
		
		for(var $base of $class.bases) {
			println("- " + $base.name);		
		}

	}

end
