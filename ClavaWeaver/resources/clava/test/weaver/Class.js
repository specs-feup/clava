import Query from "@specs-feup/lara/api/weaver/Query.js";

for(let $class of Query.search("class")) {
	console.log("Class: " + $class.name);
	console.log("Is abstract: " + $class.isAbstract);	
	console.log("Is interface: " + $class.isInterface);		
	
	console.log("All Bases:");
	for(let $base of $class.allBases) {
		console.log($base.name);	
	}
	
	console.log("All methods:")
	for(let $function of $class.allMethods) {
		console.log($function.signature);
		//console.log("Is pure? " + $function.isPure);
	}
}