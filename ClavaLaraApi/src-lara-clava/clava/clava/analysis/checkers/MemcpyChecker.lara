import clava.analysis.Checker;
import clava.analysis.CheckResult;
import clava.analysis.Fix;

/*Check for the presence of memcpy functions*/

var MemcpyChecker = function() {
      // Parent constructor
    Checker.call(this, "memcpy");
    this.advice = " memcpy() doesn\'t check the length of the destination when copying: risk of buffer overflow. "
                    + "Check if the length of the destination is sufficient (CWE-120).\n\n";
};

MemcpyChecker.prototype = Object.create(Checker.prototype);

MemcpyChecker.prototype.check = function($node) {
	if ((!$node.instanceOf("call")) || ($node.name !== 'memcpy')) {
      	return;
    }
	return new CheckResult(this.name, $node, this.advice);
}
