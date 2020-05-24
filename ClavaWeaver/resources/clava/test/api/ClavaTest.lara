import clava.Clava;
import weaver.Query;
import clava.ClavaJoinPoints;

aspectdef ClavaTest

	// Add file
	var $newFile = ClavaJoinPoints.fileWithSource("addedFile.cpp", "int foo() {return 0;}").rebuild();
	Clava.addFile($newFile); 
	println("Add file:\n" + Query.search('file', 'addedFile.cpp').first().code);

end
