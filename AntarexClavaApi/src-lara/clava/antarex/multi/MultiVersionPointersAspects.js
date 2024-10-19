import clava.ClavaJoinPoints;

aspectdef _MVP_DeclarePointers_

	input
		$file,
		name,
		typeName,
		typedef,
		dims
	end


	var pointersType = ClavaJoinPoints.constArrayType(typeName, dims);
	$file.exec addGlobal(name, pointersType, '{}');
	
	select $file.vardecl{name} end
	apply
		insert before typedef;
	end
end

aspectdef _MVP_InitPointers_

	input
		$function,
		code
	end
	
	select $function.body end
	apply
		$function.exec insertBegin(code);
	end
end
