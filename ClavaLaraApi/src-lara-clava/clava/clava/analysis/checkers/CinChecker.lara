import clava.analysis.Checker;
import clava.analysis.CheckResult;
import clava.analysis.Fix;

var CinChecker = function() {
      // Parent constructor
    Checker.call(this, "cin");
    this.advice = " Using std::cin with operator>> is risky because there is no verification for buffer overflow. Consider using a safer way to retrieve user input (CWE-20).\n\n";
};

CinChecker.prototype = Object.create(Checker.prototype);


CinChecker.prototype.check = function($node) {
	if ((!$node.instanceOf("call")) || ($node.name !== "operator>>") || (($node.args[0].code !== "cin") && ($node.args[0].code !== "std::cin"))) {
      	return;
	}
	return new CheckResult(this.name, $node, this.advice);
}
