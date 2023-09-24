import clava.analysis.Checker;
import clava.analysis.CheckResult;
import clava.analysis.Fix;

/*Check for the presence of system functions*/

var SystemChecker = function() {
      // Parent constructor
    Checker.call(this, "system");
    this.advice = " This function executes the command used as parameter and can allow an attacker to execute his own code. "
            + "Be extremely cautious when using this function and check inputs for a better security (CWE-78).\n\n";
};

SystemChecker.prototype = Object.create(Checker.prototype);

SystemChecker.prototype.check = function($node) {
	if ((!$node.instanceOf("call")) || ($node.name !== "system")) {
      	return;
	}
	return new CheckResult(this.name, $node, this.advice);
}
