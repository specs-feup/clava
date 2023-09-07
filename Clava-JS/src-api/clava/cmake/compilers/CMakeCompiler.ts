/**
 * Interface that provides information about a CMake-supported compiler.
 *
 * @class
 */
function CMakeCompiler() {
}

/*** METHODS TO IMPLEMENT ***/

CMakeCompiler.prototype.getCxxCommand = function() {
	notImplemented("CMakeCompiler.getCxxCommand");
}

CMakeCompiler.prototype.getCCommand = function() {
	notImplemented("CMakeCompiler.getCCommand");
}



/*** IMPLEMENTED METHODS ***/

/**
 * Generates the compiler-related arguments that are required when calling the CMake command.
 */
CMakeCompiler.prototype.getCommandArgs = function() {
	return "-DCMAKE_CXX_COMPILER=" + this.getCxxCommand() + " -DCMAKE_C_COMPILER=" + this.getCCommand();
}