import weaver.Query;
import clava.pass.DecomposeVarDeclarations;

aspectdef PassSimplifyVarDeclarations

	var result = (new DecomposeVarDeclarations()).apply();
	println("Result: " + result);
	println(Query.root().code);

end
