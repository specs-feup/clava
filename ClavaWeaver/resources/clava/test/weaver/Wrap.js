
aspectdef Wrap

	var lastWrapped = undefined;
	select call end
	apply
		$call.exec wrap("wrap_" + $call.name);
		// At this point, the call joinpoint changed and became the wrapped call
		lastWrapped = $call.name;
	end

	select function{lastWrapped}.call end
	apply
		$call.insert before "// Before call";
	end
	
	select program end
	apply
		println($program.code);
	end
	

end
