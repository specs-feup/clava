aspectdef Inline
	
	select function{"main"}.call end
	apply
		$call.exec inline;
	end

	select function{"main"} end
	apply
		println($function.code);
	end
	/*
	select function{"main"} end
	apply
		println($function.ast);
	end
	*/
end
