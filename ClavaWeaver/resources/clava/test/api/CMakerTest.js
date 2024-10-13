import Io from "@specs-feup/lara/api/lara/Io.js";
import Platforms from "@specs-feup/lara/api/lara/Platforms.js";
import CMaker from "@specs-feup/clava/api/clava/cmake/CMaker.js";
import Clava from "@specs-feup/clava/api/clava/Clava.js";

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
