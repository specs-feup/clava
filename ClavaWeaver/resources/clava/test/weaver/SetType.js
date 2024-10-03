import clava.ClavaJoinPoints;

aspectdef Launcher

    var doubleType = ClavaJoinPoints.builtinType("double");
    select file.function{"fooType"} end
    apply
        $function.type = doubleType;
    end
    
    
    // Deep Copy test for types
    select function{"deepCopyTest"}.vardecl end
    apply
    	// Deep copy on first vardecl
        var typeCopy = $vardecl.type.deepCopy();
        $vardecl.type = typeCopy;
        
        // Change element type
        typeCopy.elementType.elementType.setValue("elementType", doubleType);  
        
        // Stop        
        break;
    end
	
	select program end
	apply
		println($program.code);
	end
	
end
