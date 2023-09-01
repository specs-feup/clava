/**
 * Represents an MPI access pattern.
 *
 * @class
 */
function MpiAccessPattern() {
//	checkDefined(name, "Metric::name");
//	this.name = name;
}

MpiAccessPattern.prototype.sendMaster = function($varJp, totalIterations) {
	throw "Not yet implemented";
}

MpiAccessPattern.prototype.receiveMaster = function($varJp, totalIterations) {
	throw "Not yet implemented";
}

MpiAccessPattern.prototype.sendWorker = function($varJp, totalIterations) {
	throw "Not yet implemented";
}

MpiAccessPattern.prototype.receiveWorker = function($varJp, totalIterations) {
	throw "Not yet implemented";
} 

MpiAccessPattern.prototype.outputDeclWorker = function($varJp, totalIterations) {
	throw "Not yet implemented";
}


