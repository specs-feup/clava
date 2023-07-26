import weaver.util.WeaverDataStore;

/**
 * DataStore used in Clava.
 * @constructor
 */
function ClavaDataStore(data) {
    // Parent constructor
    WeaverDataStore.call(this, data, Java.type("pt.up.fe.specs.clava.weaver.CxxWeaver").getWeaverDefinition());
	
	// Add alias
	this.addAlias("Disable Clava Info", "disable_info");
	//this.addAlias("config_folder", "joptions_current_folder_path");
	
}
// Inheritance
ClavaDataStore.prototype = Object.create(WeaverDataStore.prototype);


/*** PRIVATE OVERRIDABLE FUNCTIONS ***/


/**
 * Wraps a Java DataStore around a Lara DataStore.
 */
ClavaDataStore.prototype._dataStoreWrapper = function(javaDataStore) {
	return new ClavaDataStore(javaDataStore, this.definition);
}


/*** NEW CLAVA_DATA_STORE FUNCTIONS ***/

/**
 * @return {string} a string with the current C/C++ compiler flags.
 */
ClavaDataStore.prototype.getFlags = function() {
	return this.get("Compiler Flags");
}

/**
 * @param {string} flags - A string with C/C++ compiler flags.
 * 
 */
ClavaDataStore.prototype.setFlags = function(flags) {
	checkString(flags);

//	if((typeof flags) === "array") {
//		flags = flags.join(" ");
//	} else {
//		checkString(flags);
//	}
	
	this.put("Compiler Flags", flags);	
}

/**
 * @return {J#java.util.List<String>} A list with the current extra system includes.
 */
ClavaDataStore.prototype.getSystemIncludes = function() {
	return this.get("library includes").getFiles();
}

/**
 * @return {J#java.util.List<String>} A list with the current user includes.
 */
ClavaDataStore.prototype.getUserIncludes = function() {
	return this.get("header includes").getFiles();
}

/**
 * @param {string...} arguments - A variable number of strings with the extra system includes.
 * 
 */
ClavaDataStore.prototype.setSystemIncludes = function() {
	var filenames = arrayFromArgs(arguments);
	var files = [];
	for(filename of filenames) {
		files.push(Io.getPath(filename));
	}
	
	var fileList = Java.type("org.lara.interpreter.joptions.keys.FileList").newInstance(files);	
	this.put("library includes", fileList);	
}

/**
 * @param {string...} arguments - A variable number of strings with the user includes.
 * 
 */
ClavaDataStore.prototype.setUserIncludes = function() {
	var filenames = arrayFromArgs(arguments);
	var files = [];
	for(filename of filenames) {
		files.push(Io.getPath(filename));
	}
	
	var fileList = Java.type("org.lara.interpreter.joptions.keys.FileList").newInstance(files);	
	this.put("header includes", fileList);	
}


/**
 * @return {string} a string with the current compilation standard.
 */
ClavaDataStore.prototype.getStandard = function() {
	return this.get("C/C++ Standard").toString();
}

/**
 * @param {string} flags - A string with a C/C++/OpenCL compilation standard.
 * 
 */
ClavaDataStore.prototype.setStandard = function(standard) {
	checkString(standard);

	var stdObject = Java.type("pt.up.fe.specs.clava.language.Standard").getEnumHelper().fromValue(standard);
	this.put("C/C++ Standard", stdObject);	
}
