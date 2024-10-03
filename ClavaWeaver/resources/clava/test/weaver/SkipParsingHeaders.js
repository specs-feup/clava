laraImport("clava.Clava");

// Perform a rebuilds, when the code has headers that are not being parsed
Clava.rebuild();
Clava.rebuild();
Clava.rebuild();

console.log(Clava.getProgram().code);
