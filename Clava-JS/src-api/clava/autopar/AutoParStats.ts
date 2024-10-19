import lara.Io;

/**
 * Collects stats about AutoPar execution.
 */
var AutoParStats = function() {
	this.name = "default";
	this.inlinedCalls = 0;
	this.inlineAnalysis = {};
	this.inlineAnalysisFunctions = {};
	this.inlineExcludedFunctions = [];	
	this.inductionVariableReplacements = 0;
};


/*** STATIC FUNCTIONS ***/

AutoParStats._STATS = undefined;

AutoParStats.EXCLUDED_RECURSIVE = "excluded_recursive";
AutoParStats.EXCLUDED_IS_UNSAFE = "excluded_is_unsafe";
AutoParStats.EXCLUDED_GLOBAL_VAR = "excluded_global_var";
AutoParStats.EXCLUDED_CALLS_UNSAFE = "excluded_has_unsafe_calls";

AutoParStats.get = function() {
	if(AutoParStats._STATS === undefined) {
		AutoParStats._STATS = new AutoParStats();
	}
	
	return AutoParStats._STATS;
}

AutoParStats.reset = function() {
	AutoParStats._STATS = new AutoParStats();
}

AutoParStats.save = function() {
	if(AutoParStats._STATS === undefined) {
		console.log("AutoParStats.saveStats: stats have not been initialized, use AutoParStats.resetStats()");
		return;
	}
	
	AutoParStats._STATS.write();
}


/*** INSTANCE FUNCTIONS ***/

/**
 * Sets the name of these stats (will reflect on the name of the output file)
 */
AutoParStats.prototype.setName = function(name) {
	this.name = name;
}

/**
 * Writes the current stats to the given output folder
 */
AutoParStats.prototype.write = function(outputFolder) {
	if(outputFolder === undefined) {
		outputFolder = Io.getWorkingFolder();
	}
	
	Io.writeJson(Io.getPath(outputFolder, "AutoParStats-" + this.name + ".json"), this);
}

AutoParStats.prototype.incInlineCalls = function() {
	this.inlinedCalls++;
}

AutoParStats.prototype.incInlineAnalysis = function(field, functionName) {
	if(field === undefined) {
		console.log("AutoParStats.incInlineAnalysis: field is undefined");
		return;
	}
	
	if(this.inlineAnalysis[field] === undefined) {
		this.inlineAnalysis[field] = 0;
	}
	
	this.inlineAnalysis[field]++;
	
	if(functionName !== undefined) {
		if(this.inlineAnalysisFunctions[field] === undefined) {
			this.inlineAnalysisFunctions[field] = [];
		}
		
		this.inlineAnalysisFunctions[field].push(functionName);
	}

}

AutoParStats.prototype.setInlineExcludedFunctions = function(excludedFunctions) {
	this.inlineExcludedFunctions = excludedFunctions;	
}

AutoParStats.prototype.incIndunctionVariableReplacements = function() {
	this.inductionVariableReplacements++;
}
	

