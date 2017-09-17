aspectdef Pragmas

/*
	select function{"fooCallBeforeAfter"} end
	apply
		println("AST:"+$function.ast);
	end
*/
	select marker end
	apply
		var $scope = $marker.contents;
		$scope.insert before '// Before scope - [[$marker.id]]';
		$scope.insert after '// After scope - [[$marker.id]]';

		$scope.exec insertBegin("// Scope start - " + $marker.id);
		$scope.exec insertEnd("// Scope end - " + $marker.id);
	end
		//println("name:"+$pragma.name);
		//println("target:"+$pragma.target.joinPointType);
		
	select file end
	apply
		println("Code:\n" + $file.code);
	end
	
	select marker{'foo'} end
	apply
		println('default marker attribute "id" is working: ' + $marker.id);
		println('marker contents: ' + $marker.contents.code);
	end
	
end