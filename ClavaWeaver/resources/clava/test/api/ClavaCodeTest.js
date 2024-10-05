import clava.ClavaCode;

aspectdef ClavaCodeTest

	select pragma{"foo_loop_once"} end
	apply
		println("Executes once: " + ClavaCode.isExecutedOnce($pragma.target));
	end

	select pragma{"foo_loop_not_once"} end
	apply
		println("Cannot prove one exec: " + ClavaCode.isExecutedOnce($pragma.target));
	end

end

