import clava.hdf5.Hdf5;
import clava.Clava;
import clava.ClavaJoinPoints;
import clava.Format;

aspectdef Launcher

	var $records = [];
	select record end
	apply
		$records.push($record);
	end
	
	var $hdf5LibFiles = Hdf5.toLibrary($records);

	
	// Output
	for(var $file of $hdf5LibFiles) {
		println($file.code);	
	
		// Add files so that their syntax can be checked
		Clava.getProgram().addFile($file);
	}
	
	// Example 
	//call Hdf5Types("./", "Routing");
/*
	// Output
	select file{"CompType.h"} end
	apply
	        println("Header File:");
	        println($file.code);
	end
	
	select file{"CompType.cpp"} end
	apply
	        println("Implementation File:");
	        println($file.code);
	end
end
*/
end

aspectdef Hdf5Types
	input 
		srcFolder,
		namespace
	end

	// Folder for the generated files
	var filepath = srcFolder + "/lara-generated";
	
	// Create files for generated code
	var $compTypeC = ClavaJoinPoints.file("CompType.cpp", filepath);
	var $compTypeH = ClavaJoinPoints.file("CompType.h", filepath);

	// Add files to the program
	
	select program end
	apply
		$program.exec addFile($compTypeC);
		$program.exec addFile($compTypeH);
	end
	

	var hDeclarationsCode = "";

	// Add include for CompTypes
	$compTypeC.exec addInclude("CompType.h", false);
	$compTypeC.exec addInclude("H5CompType.h", true);

	
	// For each record, create code
	//var recordCounter = 0;
	select file.record{kind === "class", kind === "struct"} end
	apply
		//recordCounter++;
		var className = $record.name + "Type";
		var typeName = "itype";
		
		/* Generate code for .h file */

		// Create declaration
		hDeclarationsCode += HDeclaration($file.name, className);
		
		/* Generate code for .cpp file */
	
		// Add include to the file where record is
		$compTypeC.exec addIncludeJp($record);

		// Create code for translating C/C++ type to HDF5 type
		
		call result : RecordToHdf5($record, typeName);
		var cxxFunction = CImplementation(namespace, className, Format.addPrefix(result.code, "    "));

		$compTypeC.exec insertAfter(ClavaJoinPoints.declLiteral(cxxFunction));
	end
	
	/* Generate code for .h file */
	
	// Add include to HDF5 CPP library
	$compTypeH.exec addInclude("H5Cpp.h", true);

	// Create header code inside the target namespace
 	hDeclarationsCode = '\nnamespace '+namespace +' {\n\n' + Format.addPrefix(hDeclarationsCode, "    ") + "\n}\n";

	// Insert code inside header file
 	$compTypeH.exec insertAfter(ClavaJoinPoints.declLiteral(hDeclarationsCode));

end

codedef HDeclaration(filename, className) %{
//  [[filename]]
class [[className]] {
	public:
	static H5::CompType GetCompType();
};

}% end


codedef CImplementation(namespace, className, body) %{
H5::CompType [[namespace]]::[[className]]::GetCompType() {
[[body]]

    return itype;
}

}% end


aspectdef RecordToHdf5
	input $record, typeName end
	output code end

	var recordName = $record.type.code;
	code = "H5::CompType "+ typeName +"(sizeof("+recordName+"));\n";

	select $record.field end
	apply
		if($field.type.constant) continue; // Ignore constant fields
		if(!$field.isPublic) continue; // Ignore private fields
	
		fieldName = $field.name;
		var HDF5Type = toHdf5($field.type);
		if(HDF5Type === undefined) continue; // Warning message omitted for the example
		var params = %{"[[fieldName]]",offsetof([[recordName]], [[fieldName]]), [[HDF5Type]]}%;
		code += %{[[typeName]].insertMember([[params]]);}% + "\n";
	end

end



