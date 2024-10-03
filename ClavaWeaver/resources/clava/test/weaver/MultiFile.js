aspectdef MultiFileTest

	// Rename record in a header, change should propagate
	select record{"A"} end
	apply
		$record.name = "A_renamed";
		//println("JP: " + $struct.kind);
		//println("Node: " + $struct.astName);
		//println("Struct name: " + $struct.name);
	end
	
	select function{"template_foo"} end
	apply
		$function.name = "template_foo_renamed";
	end
/*	
	select program.call{"template_foo_2"} end
	apply

		var $functionDef = $call.definition;
		var $functionDecl = $call.declaration;

		// If renaming is done without saving first a reference to definition and declaration,
		// it will not be able to find the declaration of the call under the new renamed definition
		//$functionDef.name = "template_foo_2_renamed";		
		$functionDecl.name = "template_foo_2_renamed";
	
		break #program;
	end
	*/
	
	select program.call{"template_foo_2"} end
	apply
		$call.definition.name = "template_foo_2_renamed";	
		break #program;
	end

	select program.call{"template_foo_3"} end
	apply
		$call.declaration.name = "template_foo_3_renamed";	
		break #program;
	end
	
	select program end
	apply
		println($program.code);
	end

end
