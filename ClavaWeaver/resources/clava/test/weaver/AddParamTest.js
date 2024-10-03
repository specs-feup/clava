aspectdef AddParamTest

	select function{'foo'} end
	apply
		$function.exec addParam("char* str");
		println($function.code);
	end

	select function{'bar'} end
	apply
		$function.exec addParam("int num");
		println($function.code);
		println("---------------");
	end
end
