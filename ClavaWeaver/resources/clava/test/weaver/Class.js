laraImport("weaver.Query");

for(let $class of Query.search("class")) {
	println("Class: " + $class.name);
	println("Is abstract: " + $class.isAbstract);	
	println("Is interface: " + $class.isInterface);		
	
	println("All Bases:");
	for(let $base of $class.allBases) {
		println($base.name);	
	}
	
	println("All methods:")
	for(let $function of $class.allMethods) {
		println($function.signature);
		//println("Is pure? " + $function.isPure);
	}
}