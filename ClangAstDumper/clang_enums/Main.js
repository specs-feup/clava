laraImport("clava.Clava");
laraImport("lara.Io");
laraImport("lara.Strings");
laraImport("lara.util.ProcessExecutor");
laraImport("ClangEnums");
laraImport("HeaderEnums");

const llvmFolder = laraArgs.llvmFolder;
const outputFolder = laraArgs.outputFolder;

println("Using LLVM folder '" + llvmFolder + "'");
println("Generating enums to folder '" + outputFolder + "'");

for (const header of ClangEnums._HEADERS) {
  header.process(llvmFolder);
  header.generateCode(outputFolder);
  header.generateJavaEnums(Io.mkdir(outputFolder, "java_enums"));
}
