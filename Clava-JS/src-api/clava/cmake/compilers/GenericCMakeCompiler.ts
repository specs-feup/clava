import lara.cmake.compilers.CMakeCompiler;

/**
 * Iterates over a list of values.
 * @constructor
 */
var GenericCMakeCompiler = function(c, cxx) {
    // Parent constructor
    CMakeCompiler.call(this);
	
	checkString(c, "GenericCMakeCompiler.new::c");
	checkString(cxx, "GenericCMakeCompiler.new::cxx");
	
	this.c = c;
	this.cxx = cxx;
};
// Inheritance
GenericCMakeCompiler.prototype = Object.create(CMakeCompiler.prototype);


GenericCMakeCompiler.prototype.getCxxCommand = function() {
	return this.cxx;
}

GenericCMakeCompiler.prototype.getCCommand = function() {
	return this.c;
}