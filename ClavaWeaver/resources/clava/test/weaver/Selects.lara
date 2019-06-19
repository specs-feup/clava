aspectdef Selects

	select function end
	apply
		println("Select function " + $function.name);
	end
	
	select function{"foo"} end
	apply
		println("Select function foo");	
	end
	
	select function{line === 1} end
	apply
		println("Select function at line 1");	
	end

	select function.call end
	apply
		println("Select calls");
	end

end
