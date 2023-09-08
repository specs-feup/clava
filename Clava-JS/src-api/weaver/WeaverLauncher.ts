import weaver.WeaverLauncherBase;
import clava.Clava;

WeaverLauncher.prototype.execute = function(args) {
	return Clava.runClava(args);
}