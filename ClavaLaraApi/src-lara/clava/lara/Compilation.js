laraImport("lara.CompilationBase");
laraImport("clava.Clava");

Compilation.getExtraSources = function () {
  return Clava.getProgram().extraSources;
};

Compilation.getExtraIncludes = function () {
  return Clava.getProgram().extraIncludes;
};

Compilation.addExtraInclude = function (path) {
  Clava.getProgram().addExtraInclude(path);
};

Compilation.addExtraIncludeFromGit = function (gitRepository, path) {
  Clava.getProgram().addExtraIncludeFromGit(gitRepository, path ?? null);
};

Compilation.addExtraSource = function (path) {
  Clava.getProgram().addExtraSource(path);
};

Compilation.addExtraSourceFromGit = function (gitRepository, path) {
  Clava.getProgram().addExtraSourceFromGit(gitRepository, path ?? null);
};

/**
 * Adds a project from a git repository. The project should be ready to build.
 *
 * libs - the list of library names exposed by the project
 * */
Compilation.addProjectFromGit = function (gitRepository, libs, path) {
  Clava.getProgram().addProjectFromGit(gitRepository, libs, path ?? null);
};

Compilation.addExtraLib = function (lib) {
  Clava.getProgram().addExtraLib(lib);
};

Compilation.getExtraProjects = function () {
  return Clava.getProgram().getExtraProjects();
};

Compilation.getExtraLibs = function () {
  return Clava.getProgram().getExtraLibs();
};
