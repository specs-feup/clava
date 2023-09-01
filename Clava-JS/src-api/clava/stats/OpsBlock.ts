import clava.stats.OpsCost;

/**
 * @class
 */
var OpsBlock = function(id) {
	this.id = id;
	this.cost = new OpsCost();
	this.nestedOpsBlocks = [];
	this.repetitions = 1;
	//this.isRecursive = false;
};

OpsBlock.prototype.toString = function() {
	return object2stringSimple(this);	
	/*
	//return object2string(this, '', true);
	var obj = {};
	obj["id"] = this.id;
	obj["cost"] = this.cost;
	obj["nestedOpsBlocks"] = this.nestedOpsBlocks;
	obj["repetitions"] = this.repetitions;
	obj["isRecursive"] = this.isRecursive;

	return object2string(obj);	
	*/
}

OpsBlock.prototype.add = function(opsId) {
	this.cost.increment(opsId);
}
