import clava.analysis.Checker;
import clava.analysis.CheckResult;
import clava.analysis.Fix;

/*Check for the presence of printf functions*/

var PrintfChecker = function() {
      // Parent constructor
    Checker.call(this, "printf");
    this.advice = " Variable used as format string can be modified by an attacker. Use a constant format specification instead (CWE-134).\n\n";
};

PrintfChecker.prototype = Object.create(Checker.prototype);

PrintfChecker.prototype.check = function($node) {
	if ((!$node.instanceOf("call")) || ($node.name !== "printf") || ($node.args[0].code.match(/\s*".*"\s*/))) {
      	return;
	}
	return new CheckResult(this.name, $node, this.advice);
}
