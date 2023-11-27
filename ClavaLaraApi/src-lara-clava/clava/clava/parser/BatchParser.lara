import lara.Io;
import lara.Check;
import lara.System;
import lara.Strings;

import weaver.Query;

import clava.Clava;
import clava.ClavaJoinPoints;

/**
 * Parses C/C++ files.
 */
var BatchParser = function(srcPath) {
	Check.isDefined(srcPath, "srcPath");
	
	this.basePath = srcPath;

	// The source files found on the given path
	this.sourceFiles = Io.getFiles(srcPath, BatchParser._IMPLEMENTATION_PATTERNS, true);
	
	// Maps header file names to the corresponding File objects
	this.headerFilesMap = {};

	var headerFiles = Io.getFiles(srcPath, BatchParser._HEADER_PATTERNS, true);
	for(var headerFile of headerFiles) {
		this.headerFilesMap[headerFile.getName()] = headerFile;
	}
	
	//println("Sources: " + this.sourceFiles.length + "\n" + this.sourceFiles);
	//println("Headers: ");
	//printlnObject(this.headerFilesMap);
};


BatchParser._IMPLEMENTATION_PATTERNS = ["*.c", "*.cpp"];
BatchParser._HEADER_PATTERNS = ["*.h", "*.hpp"];

BatchParser.prototype.getSourceFiles = function() {
	return this.sourceFiles;
}

BatchParser.prototype.parse = function(sourceFile) {
	debug("Parsing " + sourceFile + "...");
	var parsingStart = System.nanos();

	// Save current state
	//Clava.pushAst();
	
	// Clean program
	//Clava.getProgram().removeChildren();
	

	var $literalFile = Clava.getProgram().addFileFromPath(sourceFile);

	// Rebuild tree
	$parsedFile = this._rebuildFile($literalFile);

/*
	// Find file in rebuild
	var $parsedFile = undefined;
	for(var $file of Query.search("file").get()) {
		if($file.name === sourceFile.getName()) {
			$parsedFile = $file;
			break;
		}
	}
*/
	var parsingTime = System.toc(parsingStart);
	debug("Parsing took " + parsingTime);
	
	// Restore previous state
	//Clava.popAst();
	
	return $parsedFile;
}

/**
 * Tries to rebuild the current tree, using several methods to fix any problem it finds
 */
BatchParser.prototype._rebuildFile = function($literalFile) {
	var parsing = true;
	while(parsing) {

		// Add file
		//var newFile = ClavaJoinPoints.file("poll.h", "sys");
		//var newFile = ClavaJoinPoints.file("sys/poll.h");
		//Clava.getProgram().addFile(newFile);


		$parsedFile = $literalFile.rebuildTry();

		// Check if it is a file
		if($parsedFile.instanceOf('file')) {
			return $parsedFile;
		}
		
		// It is an exception
		parsing = this._solveRebuildFile($parsedFile, $literalFile);
/*
		try {
			println("REBUILD");
			
			//Clava.rebuild();
			println("REBUILD END");
		} catch(e) {
			parsing = this._solveRebuildFile(e, $literalFile);	
		}
*/
	}

	return undefined;
}


BatchParser.prototype._solveRebuildFile = function($exception, $literalFile) {
	
	// Get error message
	var message = $exception.message;

	// Check if correct type
	if($exception.exceptionType !== "ClavaParserException") {
		throw $exception.exception;
	}
	//Check.strings($exception.exceptionType, "ClavaParserException");
	
	var lines = Strings.asLines(message);

	// Check first line
	Check.isTrue(lines.length > 0);
	Check.strings(lines[0], "There are errors in the source code:");



	// Parse first error
	return this._parseError(lines.subList(1, lines.length), $literalFile);
	
	/*
	for(var i = 1; i<lines.length; i++) {
		
		println("LINE: " + line[i]);
	}
	
	
	return false;
	*/
}

BatchParser.prototype._parseError = function(lines, $literalFile) {
	var errorLine = lines[0];
	var filename = $literalFile.name;
	
	// Find name of file in line
	var nameIndex = errorLine.indexOf(filename);
	if(nameIndex === -1) {
		throw "Could not find filename '"+filename+"' in " + errorLine;
	}

	// Remove filename
	var parsedLine = errorLine.substring(nameIndex + filename.length, errorLine.length).trim();
	
	// Remove location
	var locationSep = parsedLine.indexOf(" ");
	if(locationSep !== -1) {
		parsedLine = parsedLine.substring(locationSep+1, parsedLine.length).trim();
	}
	
	// Check if fatal error
	if(parsedLine.startsWith("fatal error:")) {
		var fatalError = "fatal error:";
		parsedLine = parsedLine.substring(fatalError.length, parsedLine.length).trim();
		return this._parseFatalError(parsedLine);
	}
	
	println("Line 0: " + lines[0]);
	println("Parsed line: " + parsedLine);
	return false;
}

BatchParser.prototype._parseFatalError = function(error) {
	if(error.endsWith("' file not found")) {
		var fileNotFound = "' file not found";
		var endIndex = error.length - fileNotFound.length;
		var parsedError = error.substring(0, endIndex).trim();
		if(!parsedError.startsWith("'")) {
			throw "Expected file not found string to start with ':" + parsedError;
		}
		parsedError = parsedError.substring(1, parsedError.length).trim();
		
		// Normalize
		Strings.replacer(parsedError, "\\\\", "/");
		
		
		var filename = parsedError;
		var path = undefined;

		// Extract path 
		var slashIndex = parsedError.lastIndexOf('/');
		if(slashIndex !== -1) {
			filename = parsedError.substring(slashIndex + 1);
			path = parsedError.substring(0, slashIndex);
		}
		
		println("File: " + filename);
		println("Path: " + path);
		
		var pathname = filename;
		if(path !== undefined) {
			pathname = path + "/" + filename;
		}
		debug("Adding file " + pathname);
		var newFile = ClavaJoinPoints.file(filename, path);
		Clava.getProgram().addFile(newFile);
		
		return true;
	}
}