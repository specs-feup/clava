import clava.analysis.AnalyserResult;

var DoubleFreeResult = function(name, node, message, ptrName, scopeName, fix) {
    AnalyserResult.call(this, name, node, message, fix);
    this.ptrName = ptrName;
    this.scopeName = scopeName;
    this.freedFlag = 0;
};

DoubleFreeResult.prototype = Object.create(AnalyserResult.prototype);
