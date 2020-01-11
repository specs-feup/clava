import clava.Clava;
import clava.ClavaJoinPoints;
import lara.Io;

aspectdef AddHeaderFileTest

	// Manually add header file
	var headerFile = Io.getPath("cxx_weaver_output/add_header_file.h");
	if(!Io.isFile(headerFile)) {
		throw "Could not find header file " + headerFile;
	}

	var $file = ClavaJoinPoints.file(headerFile);
	Clava.addFile($file, "cxx_weaver_output");
	
	println($file.code);
	
end