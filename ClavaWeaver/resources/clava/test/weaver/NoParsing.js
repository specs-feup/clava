import clava.Clava;
import clava.ClavaJoinPoints;

aspectdef NoParsing

	// Add file
	var $file = ClavaJoinPoints.file("no_parsing.cpp");
	$file.insert after "int main() { return 0;}";
	
	Clava.getProgram().addFile($file);
	
	println(Clava.getProgram().code);
end
