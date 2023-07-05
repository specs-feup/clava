import clava.ClavaJoinPoints;
import weaver.Query;

aspectdef AddArgTest


	select function{'foo'} end
	apply
		if(!$function.isImplementation) {
			continue;
		}
		
		$function.exec addParam("char* str");
	end

	select function{'bar'} end
	apply
		if($function.isImplementation) {
			continue;
		}
		
		$function.exec addParam("int num");
	end
	
	select call{'bar'} end
	apply
		$call.exec addArg("0", ClavaJoinPoints.builtinType("int"));
	end	
	
	select call{'foo'} end
	apply
		var type = ClavaJoinPoints.pointerFromBuiltin("unsigned char");
		$call.exec addArg('"foo"', type);
	end

	select program end
	apply
		println($program.code);
		println("---------------");
	end
end
