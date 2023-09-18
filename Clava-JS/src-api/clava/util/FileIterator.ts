import lara.Check;
import lara.Io;
import lara.util.StringSet;

import clava.Clava;

/**
 * Given a folder, collects sources in that folder, parses and returns one each time next() is called.
 *
 * Pushes an empty Clava AST. Parsed files are added one at a time, and the AST contains at most one file at any given time.
 *
 * @param {string} srcFoldername name of the folder with the source files to iterate.
 * @param {string[]} [sourceExt=["c", "cpp"]] extensions of the source files.
 * @param {string[]} [headerExt=["h", "hpp"]] extensions of the header files.
 */
var FileIterator = function(srcFoldername, sourceExt, headerExt) {
	Check.isDefined(srcFoldername, "srcFoldername");
	
	this.srcFoldername = srcFoldername;

	if(sourceExt === undefined) {
		sourceExt = ["c", "cpp"];
	}
	this.sourceExt = sourceExt;

	if(headerExt === undefined) {
		headerExt = ["h", "hpp"];
	}
	this.headerExt = headerExt;
	
	this.files = [];
	this.currentFile = 0;
	this.isInit = false;
	this.isClosed = false;
	this.pushedAst = false;
	
};

/*
FileIterator.prototype.setSourceExtensions = function() {
	return this;
}

FileIterator.prototype.setHeaderExtensions = function() {

	return this;
}
*/

/**
 * @return $file join point, if there are still files to iterate over, or undefined otherwise
 */
FileIterator.prototype.next = function() {

	// Initialized, in case it has not been initialized yet
	this._init();

	// Check if finished
	if(!this.hasNext()) {
		return undefined;
	}

	// Next file
	var sourceFile = this.files[this.currentFile];

	// Increment
	this.currentFile++;

	debug("FileIterator.next: Processing file " + sourceFile);
	
	// Ensure program tree is empty before adding file
	Clava.getProgram().removeChildren();
	
	Clava.getProgram().addFileFromPath(sourceFile);

	// Rebuild
	Clava.rebuild();

	var $firstChild = Clava.getProgram().firstChild;


	
	return $firstChild;
}

/**
 * @return {boolean} true if there are still files to iterate over, false otherwise.
 */
FileIterator.prototype.hasNext = function() {
	// Init, if not yet initalized
	this._init();


	if(this.currentFile < this.files.length) {
		return true;
	}
	

	// Close, if not yet closed
	this._close();
		
	return false;
}




FileIterator.prototype._init = function() {

	if(this.isInit) {
		// Already initialized
		return;
	}

	this.isInit = true;

	var srcFolder = Io.getPath(Clava.getData().getContextFolder(), this.srcFoldername);

	this._addIncludes(srcFolder, this.headerExt);

	this.files = this._getFiles(srcFolder, this.sourceExt);

	// Sort files
	this.files.sort();

	debug("FileIterator: found " + this.files.length + " files");
	
	// Work on new AST tree
	Clava.pushAst();
	this.pushedAst = true;

}

FileIterator.prototype._close = function() {
	
	if(this.isClosed) {
		return;
	}

	this.isClosed = true;
	
	// Recover previous AST
	if(this.pushedAst) {
		Clava.popAst();
	}


}

/**
 * Attempts to add folders of header files as includes.
 *
 */
FileIterator.prototype._addIncludes = function(srcFolder, headerExt) {

	// TODO: If needed, add a 'nestingLevel' parameter, which includes up to X ancestors for each header,
	// cutting off if ancestors go before srcFolder
	
	// Current user includes
	var data = Clava.getData();
	var userIncludes = data.getUserIncludes();
	debug("FileIterator._addIncludes: User includes before " + userIncludes);

	// Populate initial set with user includes
	var parents = new StringSet();

	for(var userInclude of userIncludes) {
		parents.add(Io.getAbsolutePath(userInclude));
	}
		
	// Get folders of hFiles
	var headerFiles = this._getFiles(srcFolder, headerExt);

	for(var hFile of headerFiles) {
		parents.add(Io.getAbsolutePath(hFile.getParentFile()));
	}

	// Build new value
	var includeFolders = [];
	for(var parent of parents.values()) {
		
		// Converting to File
		includeFolders.push(Io.getPath(parent));
	}
	data.setUserIncludes(includeFolders);
	debug("FileIterator._addIncludes: User includes after " + data.getUserIncludes());

}

FileIterator.prototype._getFiles = function(folder, extensions) {
	var files = [];

	for(var extension of extensions) {
		var sourceFiles = Io.getFiles(folder, "*." + extension, true);
		files = files.concat(sourceFiles);
	}

	return files;
}
