import weaver.Query;
import clava.ClavaJoinPoints;

aspectdef FileTest

	var $file = Query.search('file').first();

	var $program = Query.root();
	$program.addFile(ClavaJoinPoints.file("user_include.h"));
	$program.addFile(ClavaJoinPoints.file("system_include.h"));
	$program.addFile(ClavaJoinPoints.file("c_include.h"));

	// Includes
	$file.addInclude("user_include.h");
	$file.addInclude("system_include.h", true);
	$file.addCInclude("c_include.h");

	println($file.code);
end

