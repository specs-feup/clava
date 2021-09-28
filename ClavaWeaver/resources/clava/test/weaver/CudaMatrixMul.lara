import clava.ClavaJoinPoints;
import weaver.Query;


aspectdef CudaMatrixMul

	var $vardecl = Query.search("function", "multiMatrix").search("vardecl", "s_a").first();
	for(var $attr of $vardecl.attrs) {
		println("Attr kind: " + $attr.kind);	
		println("Attr code: " + $attr.code);	
	}	


end
