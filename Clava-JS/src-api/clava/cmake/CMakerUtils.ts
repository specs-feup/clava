import lara.Strings;
import lara.cmake.compilers.GenericCMakeCompiler;

/**
 * @class
 */
var CMakerUtils = {};
	
CMakerUtils._compilerTable = {};	
CMakerUtils._compilerTable["gcc"] = function() {return new GenericCMakeCompiler("gcc", "g++");};
CMakerUtils._compilerTable["clang"] = function() {return new GenericCMakeCompiler("clang", "clang++");};
CMakerUtils._compilerTable["icc"] = function() {return new GenericCMakeCompiler("icc", "icpc");};
	
/**
 * Creates a CMakerCompiler object based on a string with the name.
 *
 * @param {string} compilerName - Name of the compiler. Currently supported names:  'gcc', 'clang', 'icc'.
 * @returns {lara.cmake.compilers.CMakeCompiler}
 */ 
CMakerUtils.getCompiler = function(compilerName) {
	var compilerSupplier = CMakerUtils._compilerTable[compilerName];
	if(compilerSupplier === undefined) {
		info("Compiler name '"+compilerName+"' not supported. Supported names: " + Object.keys(CMakerUtils._compilerTable) ,"CMakerUtils.getCompiler");
		return undefined;
	}

	return compilerSupplier();
}	