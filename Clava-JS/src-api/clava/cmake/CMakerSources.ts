import lara.Strings;
import lara.Compilation;
import lara.util.StringSet;
import lara.cmake.CMakerUtils;
import lara.tool.ToolUtils;

/**
 * Contains CMaker sources
 */
var CMakerSources = function(disableWeaving) {
	this.untaggedSources = [];
	this.taggedSources = {};
	this.tags = new StringSet();
	this.disableWeaving = defaultValue(disableWeaving, false);
};

CMakerSources.prototype._VARIABLE_UNTAGGED_SOURCES = "CMAKER_SOURCES";
CMakerSources.prototype._VARIABLE_EXTERNAL_SOURCES = "EXTERNAL_SOURCES";

/**
 * 
 */ 
CMakerSources.prototype.copy = function() {
	var newCMakerSources = new CMakerSources();
	
	//this.untaggedSources = this.untaggedSources.slice();
	//this.taggedSources = JSON.parse(JSON.stringify(this.taggedSources));
	//this.tags = this.tags.copy();
	newCMakerSources.untaggedSources = this.untaggedSources.slice();
	newCMakerSources.taggedSources = JSON.parse(JSON.stringify(this.taggedSources));
	newCMakerSources.tags = this.tags.copy();
	newCMakerSources.disableWeaving = this.disableWeaving;
	
	return newCMakerSources;
}
	
/**
 * Adds the given sources.
 *
 * @param paths array with paths to sources
 */
CMakerSources.prototype.addSources = function(paths) {
	checkDefined(paths, "path", "CMakerSources.addSources");
	for(path of paths) {
		this._addSourcePrivate(this.untaggedSources, path);
		//this.untaggedSources.push(path);
	}
}

/**
 * Add the given sources.
 */
CMakerSources.prototype.addSource = function(path) {
	checkDefined(path, "path", "CMakerSources.addSource");
	this._addSourcePrivate(this.untaggedSources, path);
}

/**
 * Adds the given sources associated to a tag.
 */
CMakerSources.prototype.addTaggedSources = function(tag, paths) {
	checkDefined(tag, "tag", "CMakerSources.addTaggedSources");
	checkDefined(paths, "paths", "CMakerSources.addTaggedSources");
	
	// Get current tagged sources
	var sources = this.taggedSources[tag];
	
	// If not defined, initialize it
	if(sources === undefined) {
		sources = [];
		this.taggedSources[tag] = sources;
		this.tags.add(tag);
	}
	
	for(var path of paths) {
		this._addSourcePrivate(sources, path);
	}
	
}

CMakerSources.prototype._addSourcePrivate = function(sources, path) {
	var parsedPath = ToolUtils.parsePath(path);
	sources.push('\"' + parsedPath + '\"');
	//sources.push('\"' + Strings.escapeJson(path) + '\"');
}


/**
 * @return an array with the CMake variables that have source files
 */
CMakerSources.prototype.getSourceVariables = function() {
	var sources = [];
	
	if(this.untaggedSources.length > 0) {
		sources.push(this._VARIABLE_UNTAGGED_SOURCES);
	}

	if(!this.disableWeaving) {
		if(Compilation.getExtraSources().length > 0) {
			sources.push(this._VARIABLE_EXTERNAL_SOURCES);
		}
	}

	
	
	for(tag of this.tags.values()) {
		sources.push(tag);
	}
	
	return sources;
}

CMakerSources.prototype._parseSourcePath = function(path) {
	var parsedPath = CMakerUtils.parsePath(path);
	return '\"' + parsedPath + '\"';	
}


/**
 * @return String with the CMake code that declares the current sources
 */
CMakerSources.prototype.getCode = function() {
	var code = "";

	// Add untagged sources
	if(this.untaggedSources.length > 0) {
		code += this._getCodeSource(this._VARIABLE_UNTAGGED_SOURCES, this.untaggedSources);
	}
	
	// Add external sources if weaving is not disabled
	if(!this.disableWeaving) {
		code = this._addExternalSources(code);
	}

	/*
	var extraSources = Compilation.getExtraSources();
	var extraSourcesArray = [];
	for(var extraSource of extraSources) {
		debug("Adding external source '" + extraSource + "'");
		if(Io.isFile(extraSource)) {
			extraSourcesArray.push(this._parseSourcePath(extraSource));
		} else if(Io.isFolder(extraSource)) {
			for(var sourcePath of Io.getPaths(extraSource)) {
				extraSourcesArray.push(this._parseSourcePath(sourcePath));
			}
			//extraSourcesArray = extraSourcesArray.concat(Io.getPaths(extraSource));
		} else {
			println("[CMAKER] Extra source ' " + extraSource +  " ' does not exist");
		}
	}	
	
	if(extraSourcesArray.length > 0) {
		if(code.length !== 0) {
			code += "\n";
		}
		
		code += this._getCodeSource(this._VARIABLE_EXTERNAL_SOURCES, extraSourcesArray);
	}
	*/
	
	for(tag of this.tags.values()) {
		var tagCode = this._getCodeSource(tag, this.taggedSources[tag]);
		
		if(code.length !== 0) {
			code += "\n";
		}
		
		code += tagCode;
	}
	
	return code;
}

CMakerSources.prototype._addExternalSources = function(code) {

	var extraSources = Compilation.getExtraSources();
	var extraSourcesArray = [];
	for(var extraSource of extraSources) {
		debug("Adding external source '" + extraSource + "'");
		if(Io.isFile(extraSource)) {
			extraSourcesArray.push(this._parseSourcePath(extraSource));
		} else if(Io.isFolder(extraSource)) {

			for(var sourcePath of Io.getPaths(extraSource)) {
				extraSourcesArray.push(this._parseSourcePath(sourcePath));
			}
			//extraSourcesArray = extraSourcesArray.concat(Io.getPaths(extraSource));
		} else {
			println("[CMAKER] Extra source ' " + extraSource +  " ' does not exist");
		}
	}	

	if(extraSourcesArray.length > 0) {
		if(code.length !== 0) {
			code += "\n";
		}
		
		code += this._getCodeSource(this._VARIABLE_EXTERNAL_SOURCES, extraSourcesArray);
	}
	
	return code;
}

/**
 * @return String with the CMake code for a given source name and values
 **/
CMakerSources.prototype._getCodeSource = function(sourceName, values) {
	var prefix = "set (" + sourceName +  " ";
	var code = prefix;

	// Build space
	var space = "";
	for(var i = 0; i<prefix.length; i++) {
		space += " ";
	}
	
	code += values.join("\n" + space);
	
	code += "\n)";
	
	return code;
}
