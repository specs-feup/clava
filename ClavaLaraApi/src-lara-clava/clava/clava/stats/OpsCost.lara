/**
 * @class
 */
var OpsCost = function() {
	this.ops = {};
};

OpsCost.prototype.toString = function() {
	return object2stringSimple(this);	
	/*
	var obj = {};

	obj["ops"] = this.ops;

	return object2string(obj);
	*/	
}

OpsCost.prototype.increment = function(opsId) {
	var currentValue = this.ops[opsId];
	if(currentValue === undefined) {
		this.ops[opsId] = 1;
	} else {
		this.ops[opsId] = currentValue + 1;
	}
}