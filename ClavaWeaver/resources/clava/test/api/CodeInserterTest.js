import CodeInserter from "@specs-feup/clava/api/clava/util/CodeInserter.js";
import Clava from "@specs-feup/clava/api/clava/Clava.js";
import Io from "@specs-feup/lara/api/lara/Io.js";

const codeInserter = new CodeInserter();

const outputFolder = Clava.getWeavingFolder();

// Select the only file of the test
const $file = Clava.getProgram().getDescendants("file")[0];

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

const outputFiles = Io.getFiles(outputFolder, undefined, true);
const outputFile = outputFiles.map((file) => file.getPath()).find((file) => file.endsWith("code_inserter.c"));

if (!outputFile) {
  throw new Error("Output file not found");
}

// Print file
console.log(Io.readFile(outputFile));

// Clean
//Io.deleteFolder(outputFolder);
