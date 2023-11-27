laraImport("lara.Io");
laraImport("lara.Platforms");
laraImport("lara.cmake.CMaker");
laraImport("clava.Clava");

var cmaker = new CMaker("testapp")
  .setMinimumVersion("3.0.2")
  .addCxxFlags("-O3", "-std=c++11")
  .addLibs("stdc++")
  .addCurrentAst();

if (Platforms.isWindows()) {
  cmaker.setGenerator("MinGW Makefiles").setMakeCommand("mingw32-make");
}

/*
	select file end
	apply
		//println("Relative filepath: " + $file.relativeFilepath);
		//println("Basesourcepath: " + $file.baseSourcePath);
		//println("Destination filepath: " + $file.getDestinationFilepath(Clava.getWeavingFolder()));
		cmaker.sources.addSource($file.relativeFilepath);
		
		if($file.isHeader) {
			cmaker.addIncludeFolder($file.relativeFolderpath);
		}
	end
	
	// Write AST code
	Clava.writeCode(Clava.getWeavingFolder());
	*/
// Build
var buildFolder = Io.getPath(Clava.getWeavingFolder(), "build");
var executable = cmaker.build(Clava.getWeavingFolder(), buildFolder);

if (executable !== undefined) {
  println("Created executable: " + Io.removeExtension(executable.getName()));
} else {
  println("Could not create executable");
}

// Writing the build file
//var cmakeFile = Io.getPath(Clava.getWeavingFolder(), "CMakeLists.txt");
//Io.writeFile(cmakeFile, cmaker.getCode());
