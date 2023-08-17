import clava.analysis.AnalyserResult;

var BoundsResult = function(name, node, message, scopeName, initializedFlag, unsafeAccessFlag, lengths, fix) {
    AnalyserResult.call(this, name, node, message, fix);
    this.arrayName = node.name;
    this.scopeName = scopeName;
    this.initializedFlag = initializedFlag;
    this.unsafeAccessFlag = unsafeAccessFlag;
    this.lengths = lengths;
};

BoundsResult.prototype = Object.create(AnalyserResult.prototype);
