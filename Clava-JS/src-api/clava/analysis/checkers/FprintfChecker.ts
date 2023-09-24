import clava.analysis.Checker;
import clava.analysis.CheckResult;
import clava.analysis.Fix;

/*Check for the presence of fprintf functions*/

var FprintfChecker = function() {
      // Parent constructor
    Checker.call(this, "fprintf");
    this.advice = " Variable used as format string can be modified by an attacker.Use a constant format specification instead (CWE-134).\n\n";
};

FprintfChecker.prototype = Object.create(Checker.prototype);

FprintfChecker.prototype.check = function($node) {
	if ((!$node.instanceOf("call")) || ($node.name !== "fprintf") || ($node.args[1].code.match(/\s*".*"\s*/))) {
      	return;
	}
	return new CheckResult(this.name, $node, this.advice);
}
