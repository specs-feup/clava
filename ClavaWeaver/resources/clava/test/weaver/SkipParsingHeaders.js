import Clava from "@specs-feup/clava/api/clava/Clava.js";

// Perform a rebuilds, when the code has headers that are not being parsed
Clava.rebuild();
Clava.rebuild();
Clava.rebuild();

console.log(Clava.getProgram().code);
