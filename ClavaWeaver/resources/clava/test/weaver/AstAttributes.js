aspectdef AstAttributes

	select vardecl{"a"} end
	apply
		println("Type kind:" + $vardecl.type.kind);
		println("Is builtin? " + $vardecl.type.astIsInstance("BuiltinType"));
		println("Is decl? " + $vardecl.astIsInstance("Decl"));
	end
	
end
