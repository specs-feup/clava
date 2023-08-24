import lara.Io;

import lara.benchmark.BenchmarkInstance;

import clava.Clava;
import clava.ClavaJoinPoints;

import weaver.Query;
import weaver.Weaver;


/**
 * Instance of a Clava benchmark.
 *
 * Implements _compilePrivate and .getKernel().
 */
function ClavaBenchmarkInstance(name) {
    // Parent constructor
    BenchmarkInstance.call(this, name);
	
//	this._cmaker =  new CMaker(this.getName());
	this._isCachedAst = false;
}
// Inheritance
ClavaBenchmarkInstance.prototype = Object.create(BenchmarkInstance.prototype);

ClavaBenchmarkInstance._CACHE_ENABLE = false;

/**
 * @param {boolean} enable - if true, enables caching of parsed files. By default, caching is enabled.
 */
ClavaBenchmarkInstance.setCache = function(enable) {
	ClavaBenchmarkInstance._CACHE_ENABLE = enable;
}

/**
 * @return temporary folder for caching ASTs.
 */
ClavaBenchmarkInstance._getCacheFolder = function() {
	return Io.getTempFolder("clavaBenchmarkAsts");
}

/**
 * Clears compilation cache of all ClavaBenchmarkInstances.
 */
ClavaBenchmarkInstance.purgeCache = function() {
	Io.deleteFolderContents(ClavaBenchmarkInstance._getCacheFolder());
}

ClavaBenchmarkInstance.prototype.isCachedAst = function() {
	return this._isCachedAst;
}

/**
 * @return {J#java.io.File} the File representing the cached program of this BenchmarkInstance. The file might not exist.
 */ 
ClavaBenchmarkInstance.prototype._getCachedFile = function() {
	return Io.getPath(ClavaBenchmarkInstance._getCacheFolder(), this.getName() + ".ast");
	
	//return Io.isFile(cachedFile) ? cachedFile : undefined;
}


/**
 * The output folder for this BenchmarkInstance.
 */ 
ClavaBenchmarkInstance.prototype._getOutputFolder = function() {
	return Io.mkdir(this.getBaseFolder(), this.getName());
}

/*
ClavaBenchmarkInstance.prototype.getCMaker = function() {
	return this._cmaker;
}
*/


ClavaBenchmarkInstance.prototype._loadCached = function(astFile) {
	println("Loading cached AST from file " + astFile.getAbsolutePath() + "...");

	// Load saved AST
	var $app = Weaver.deserialize(Io.readFile(astFile));

	// Push loaded AST
	Clava.pushAst($app);
/*	
	// Clean AST
	Query.root().removeChildren();


	// Add code
	this._addCode();
	
	// Set standard
	this._previousStandard = Clava.getData().getStandard();
	Clava.getData().setStandard("c99");	

	// Set define
	this._previousFlags = Clava.getData().getFlags();
	var modifiedFlags = this._previousFlags + " -D" + this._inputSize + "_DATASET -DPOLYBENCH_TIME";
	Clava.getData().setFlags(modifiedFlags);

	// Rebuild
	Clava.rebuild();
*/	
}




ClavaBenchmarkInstance.prototype._loadPrologue = function() {
	// by default, does nothing
}

/*** IMPLEMENTATIONS ***/


/*
ClavaBenchmarkInstance.prototype.close = function() {
	//var result = super.close();
	
	// Clean CMaker
	this._cmaker =  new CMaker(this.getName());
	
	return result;
}
*/

/**
 * 
 */
ClavaBenchmarkInstance.prototype._compilePrivate = function() {

	
	var folder = this._getOutputFolder();
	Clava.writeCode(folder);
	
	var cmaker = this.getCMaker();

	cmaker.addCurrentAst();

	var exe = cmaker.build(folder);

	this._setExecutable(exe);
	
	return exe;
}

/**
 * Looks for #pragma kernel, returns target of that pragma
 */
ClavaBenchmarkInstance.prototype.getKernel = function() {
	var $pragma = Query.search("pragma", "kernel").first();
	
	if($pragma === undefined) {  
		throw "ClavaBenchmarkInstance.getKernel: Could not find '#pragma kernel' in benchmark " + this.getName();
	}
	
	return $pragma.target;
}



/** RE-IMPLEMENTATIONS **/

/**
 * Adds support for caching.
 */
ClavaBenchmarkInstance.prototype.load = function() {
	// Check if already loaded
	if(this._hasLoaded) {
		//println("BenchmarkInstance.load(): Benchmark " + this.getName() + " is already loaded");
		return;
	}

	// Execute things that are common to cache and AST parsing
	this._loadPrologue();

	var result = undefined;

	// Check if a chached version of the tree has already been cached
	var cachedFile = this._getCachedFile();
	if(Io.isFile(cachedFile)) {
		// Load cached AST
		result = this._loadCached(cachedFile);	
		this._isCachedAst = true;
	} else {

		println("Parsing " + this.getName() + "...");
		var result = this._loadPrivate();


		// If caching enabled, save AST
		if(ClavaBenchmarkInstance._CACHE_ENABLE) {
			println("Saving AST to file " + cachedFile.getAbsolutePath() + "...");
			var serialized = Weaver.serialize(Query.root());
			Io.writeFile(cachedFile, serialized);
		}
		
		this._isCachedAst = false;
	}
	

	// Mark as loaded
	this._hasLoaded = true;
	
	return result;
}


