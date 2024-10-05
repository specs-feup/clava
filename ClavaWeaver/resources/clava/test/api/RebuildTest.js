import clava.Clava;
import clava.ClavaJoinPoints;
import lara.Io;

aspectdef RebuildTest

	// Manually add header file
	var headerFile = Io.getPath("cxx_weaver_output/rebuild.h");
	//println("header exists? " + Io.isFile(headerFile));
	var $file = ClavaJoinPoints.file(headerFile);
	Clava.addFile($file, "cxx_weaver_output");
	
	//println("Header code: " + $file.code);

	//println("Pushing");
	println("Stack size before push: " + Clava.getStackSize());
	Clava.pushAst();
	println("Stack size after push: " + Clava.getStackSize());
	
	select function{"main"} end
	apply
		$function.insert before "// Hello";
	end

	//println("Rebuilding");
	Clava.rebuild();
	println("Temporary code:\n" + Clava.getProgram().code);
	
	//println("Poping");
	Clava.popAst();
	println("Stack size after pop: " + Clava.getStackSize());
	//println("Rebuilding");
	
	
	// Rebuild two times, to stress test rebuild
	Clava.rebuild();
	Clava.rebuild();
	
	println("Original code:\n" + Clava.getProgram().code);

	

/*	
	// Insert error in the code to parse
	select function{"main"}.body end
	apply
		$body.insertBegin("a = 0;\nb = 0;");
	
	end
	
	// Should give an error
	Clava.rebuild();
	/*
	try {
		Clava.rebuild();
		println("Did not find compilation error");		
	} catch(e) {
		println("Found compilation error");
	}
	*/
	
//	select program end
//	apply
//		$program.rebuild();
//	end



end

