laraImport("weaver.WeaverLauncherBase");
laraImport("clava.Clava");

WeaverLauncher.prototype.execute = function (args) {
  return Clava.runClava(args);
};
