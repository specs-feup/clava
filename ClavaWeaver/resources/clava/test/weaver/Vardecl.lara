aspectdef Vardecl

	select vardecl end
	apply
		println("vardecl " + $vardecl.name);
		println("is param? " + $vardecl.isParam);
	end
	
	select call end
	apply
		if($call.declaration !== undefined) {
			println("Call '" + $call.name + "' declaration:" + $call.declaration.line);
		}

		if($call.definition !== undefined) {
			println("Call '" + $call.name + "' definition:" + $call.definition.line);
		}

	end


end
