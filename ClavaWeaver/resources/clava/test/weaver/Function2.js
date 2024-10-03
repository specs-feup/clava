import weaver.Query;
import clava.ClavaJoinPoints;

aspectdef OCL_Experiments
	var $d = Query.search("function", {isImplementation: true}).first();
	var $fn = $d.clone($d.name+"_");
	$d.setReturnType(ClavaJoinPoints.typeLiteral("void"));
	//$fn.type = ClavaJoinPoints.typeLiteral("void");	
	$d.body.replaceWith(ClavaJoinPoints.scope());


	$fn.addParam("ret", ClavaJoinPoints.typeLiteral($fn.type.code + ($fn.type.isPointer ? "" : "*")));
	println($d.params.map($param => $param.name));
	println($fn.params.map($param => $param.name));
	
	println(Query.root().code);
end