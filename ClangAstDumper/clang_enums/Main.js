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
  /*	
	try {
		header.process(llvmFolder);
		header.generateCode("C:\\Users\\JBispo\\Work\\Repos\\Lab\\clava\\ClangAstDumper");		
	} catch(e) {
		println("Skipping header " + header.getName() + ": " + e);
	}
*/
}

/*
		const header = ClangEnums._DECL_CXX_H;
		header.process(llvmFolder);
		header.generateCode("C:\\Users\\JBispo\\Work\\Repos\\Lab\\clava\\ClangAstDumper");		
		header.generateJavaEnums("C:\\Users\\JBispo\\Work\\Repos\\Lab\\clava\\ClangAstDumper\\clang_enums_v2\\JavaEnums");		
*/
/*
const testHeaderEnum = ClangEnums._OPERATIONS_KINDS_H;

testHeaderEnum.process(llvmFolder)

testHeaderEnum.generateCode("C:\\Users\\JBispo\\Work\\Repos\\Lab\\clava\\ClangAstDumper");
*/
/*
const compilerCmd = "clang"
const baseFolder = Clava.getData().getContextFolder();



const headerFile = Io.getPath(llvmFolder, "/src/include/clang/AST/OperationKinds.h");

//const preprocessorFolder = Io.mkdir(baseFolder, "preprocessorOutput")

// Preprocess header
const exec = new ProcessExecutor();
// Clean folder and set working directory
//Io.deleteFolderContents(preprocessorFolder);
//exec.setWorkingDir(preprocessorFolder);
exec.setPrintToConsole(false);
const preprocessedFile = exec.execute(compilerCmd, "-E", headerFile.getAbsolutePath());
//println(headerFile.getAbsolutePath())

println(preprocessedFile)

const headerLines = Strings.asLines(preprocessedFile);

// Extract enum from header
const enums = extractEnum("CastKind", headerLines);
println("enums:\n"+enums);
*/
/*
function extractEnum(enumName, lines) {
	const regex = "enum\\s+"+enumName+"\\s+{";
	const enumNameRegex = new RegExp(regex);
	//const enumNameRegex = new RegExp("\\s"+enumName+"\\s");

	const enums = [];
	let searching = true;
	let completed = false;
	

	for(var line of lines) {
		if(searching) {
			// Find enum
			const matchResult = line.match(enumNameRegex)
			if(matchResult == null) {
				continue;
			}

			// Found enum, finish search
			searching = false;
			continue;
		}

		// Collect enums until } is found
		line = line.trim();

		if(line.startsWith("}")){
			// Finished, set flag
			completed = true;
			break;
		}

		// Continue if empty or comment
		if(line == "" || line.startsWith("#")) {
			continue;
		}

		// Remove , if present
		if(line.endsWith(",")) {
			line = line.substring(0, line.length-1);
		}

		// Get enum

		//println("Enum: " + line)
		enums.push(line);	
		
	}

	if(!completed) {
		throw new Error("Could not find } for enum " + enumName);
	}

	return enums;
}
*/
