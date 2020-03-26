aspectdef InsertsLiteral

	select function{"fooStmtBeforeAfter"}.stmt end
	apply
		$stmt.insert before "int b = 1;";
		$stmt.insert after "int c = a + b;";
	end
	
	select function{"fooStmtReplace"}.stmt end
	apply
		$stmt.insert replace "int a = 100;";
	end

	select function{"fooBodyBeforeAfter"}.body end
	apply
		$body.insert before "int b = 1;";
		$body.insert after "int c = a + b;";
	end
	
	select function{"fooBodyReplace"}.body end
	apply
		$body.insert replace "int a = 100;";
	end
	
	select function{"fooBodyEmptyBeforeAfter"}.body end
	apply
		$body.insert before "int b = 1;";
		$body.insert after "int c = 2 + b;";
	end
	
	select function{"fooBodyEmptyReplace"}.body end
	apply
		$body.insert replace "int a = 100;";
	end
	
	select function{"fooCallBeforeAfter"}.stmt.call end
	apply
		$call.insert before "int b = 1;";
		$call.insert after "int c = 2 + b;";
	end
	
	select function{"fooCallReplace"}.stmt.call end
	apply
		$call.insert replace "pow(2.0, 3.0)";
	end
	
	select function{"fooBeforeAfter"} end
	apply
		$function.insert before "// A comment";
		$function.insert after "int GLOBAL = 10;";
	end
	
	select function{"fooReplace"} end
	apply
		$function.insert replace %{void fooReplaced() {
	int a = 0;
}}%;
	end
	
	select function{"callsInsideFor"}.call end
	apply
		$call.insert after "// After call";
		$call.insertAfter("// After call");
	end
	
	
	// Output code
	select file end
	apply
		println($file.code);
	end

end