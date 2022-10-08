laraImport("clava.util.CodeInserter");
laraImport("clava.Clava");
laraImport("weaver.Query");

var codeInserter = new CodeInserter();

var outputFolder = Clava.getWeavingFolder();
//var outputFolder = Io.getAbsolutePath(Io.mkdir("./codeInserter"));

// Select the only file of the test
var $file = Clava.getProgram().descendants("file")[0];

codeInserter.add(
  $file,
  11,
  "    #pragma omp parallel for default(shared) firstprivate(numIter) reduction(+ : a)"
);
codeInserter.add(
  $file,
  15,
  "    #pragma omp parallel for default(shared) reduction(+ : a)"
);
codeInserter.add($file, 24, "    #pragma omp parallel for default(shared)");

// Add include
codeInserter.add($file, 1, "#include <omp.h>");

// Write
codeInserter.write(outputFolder);

// Check file (first element is the folder)
//println("PATHS:");
//printObject(Io.getFilesRecursive(outputFolder));
//println("PATHS *.c:");
//printObject(Io.getFilesRecursive(outputFolder, "*.c"));

//var outputFile = Io.getFilesRecursive(outputFolder)[0];
var outputFile = Io.getFiles(outputFolder, undefined, true)[0];

// Print file
println(Io.readFile(outputFile));

// Clean
//Io.deleteFolder(outputFolder);
