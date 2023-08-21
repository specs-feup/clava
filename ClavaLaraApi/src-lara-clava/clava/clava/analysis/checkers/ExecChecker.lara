import clava.analysis.Checker;
import clava.analysis.CheckResult;
import clava.analysis.Fix;

/*Check for the presence of exec family functions*/

var ExecChecker = function() {
      // Parent constructor
    Checker.call(this, "exec");
    this.advice = " This function executes another program used as parameter and can allow an attacker to execute his own code. "
            + "Be extremely cautious when using this function and check inputs for a better security (CWE-78).\n\n";
};

ExecChecker.prototype = Object.create(Checker.prototype);


ExecChecker.prototype.check = function($node) {
	if ((!$node.instanceOf("call")) || !($node.name.match(/execl|execlp|execle|execv|execvp|execvpe/))) {
      	return;
	}
	return new CheckResult(this.name, $node, this.advice);
}
