laraImport("lara.Io");
laraImport("lara.Platforms");
laraImport("clava.cmake.CMaker");
laraImport("clava.Clava");

const cmaker = new CMaker("testapp")
  .setMinimumVersion("3.0.2")
  .addCxxFlags("-O3", "-std=c++11")
  .addLibs("stdc++")
  .addCurrentAst();

if (Platforms.isWindows()) {
  cmaker.setGenerator("MinGW Makefiles").setMakeCommand("mingw32-make");
}

// Build
const executable = cmaker.build();

if (executable !== undefined) {
  println("Created executable: " + Io.removeExtension(executable.getName()));
} else {
  println("Could not create executable");
}
