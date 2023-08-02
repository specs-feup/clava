import lara.analysis.Checker;
import lara.analysis.CheckResult;
import lara.analysis.Fix;

/*Check for the presence of strcpy functions*/

var StrcpyChecker = function() {
      // Parent constructor
    Checker.call(this, "strcpy");
    this.advice = " Unsafe function strcpy() can be replaced by safer strncpy()(Possible Fix). Be careful though because strncpy() doesn't null-terminate. strcpy() doesn\'t check the length of the buffer: risk of buffer overflow (CWE-120).\n\n";
};

StrcpyChecker.prototype = Object.create(Checker.prototype);

StrcpyChecker.fixAction = function($node) {
	var newFunction = "strncpy(" + $node.args[0].code + ", " + $node.args[1].code
	     		+ ", sizeof(" + $node.args[0].code + "))";
	$node.replaceWith(newFunction);
}

StrcpyChecker.prototype.check = function($node) {
	if ((!$node.getInstanceOf("call")) || ($node.name !== 'strcpy')) {
      	return;
    }
	return new CheckResult(this.name, $node, this.advice, new Fix($node, StrcpyChecker.fixAction));
}
