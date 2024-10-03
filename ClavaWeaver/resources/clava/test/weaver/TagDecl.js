import weaver.Query;
import clava.ClavaJoinPoints;

aspectdef TagDecl

	select function{"structTest"}.vardecl end
    apply
        println("Struct: " + $vardecl.type.desugarAll.decl.name);
    end


	// Test copying struct
	select vardecl{"struct_decl"} end
	apply
	 	var $elaboratedType = $vardecl.type; // Obtain elaborated type
        	        
	    // Elaborated type can be other things besides structs (e.g., class A, enum B)
	    // You can test the type of the elaborated type by using .keyword
	    // Obtain the struct itself.
        var $struct = $elaboratedType.desugarAll.decl; 

        var $newStruct = $struct.copy(); // Copy struct
        $newStruct.name = "new_struct"; // Change name	        
        $struct.insertAfter($newStruct); // Insert new struct in the code

		// Create type for new structure, returns an elaboratedType
		var $newStructType = ClavaJoinPoints.structType($newStruct);
		// Create typedef for new structure
		var $typedef = ClavaJoinPoints.typedefDecl($newStructType, "new_typedef"); 		
		// Insert typedef after vardecl
		$vardecl.insertAfter($typedef);
	end
	
	println("Code:\n" + Query.root().code);
end
