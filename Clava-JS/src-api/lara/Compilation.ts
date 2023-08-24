import lara.CompilationBase;
import clava.Clava;

Compilation.getExtraSources = function() {
	return Clava.getProgram().extraSources;
}


Compilation.getExtraIncludes = function() {
	return Clava.getProgram().extraIncludes;
}


Compilation.addExtraInclude = function(path) {
	Clava.getProgram().addExtraInclude(path);
}


Compilation.addExtraIncludeFromGit = function(gitRepository, path) {
	if(isUndefined(path)) {
		path = null;
	}
	
	Clava.getProgram().addExtraIncludeFromGit(gitRepository, path);
}


Compilation.addExtraSource = function(path) {
	Clava.getProgram().addExtraSource(path);
}


Compilation.addExtraSourceFromGit = function(gitRepository, path) {
	if(isUndefined(path)) {
		path = null;
	}
	
	Clava.getProgram().addExtraSourceFromGit(gitRepository, path);
}

/**
 * Adds a project from a git repository. The project should be ready to build.
 * 
 * libs - the list of library names exposed by the project
 * */
Compilation.addProjectFromGit = function(gitRepository, libs, path) {
	if(isUndefined(path)) {
		path = null;
	}
	Clava.getProgram().addProjectFromGit(gitRepository, libs, path);
}

Compilation.addExtraLib = function(lib) {
	Clava.getProgram().addExtraLib(lib);
}

Compilation.getExtraProjects = function() {
	return Clava.getProgram().getExtraProjects();
}

Compilation.getExtraLibs = function() {
	return Clava.getProgram().getExtraLibs();
}
