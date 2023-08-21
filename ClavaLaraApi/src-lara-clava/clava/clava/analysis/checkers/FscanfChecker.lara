import clava.analysis.Checker;
import clava.analysis.CheckResult;
import clava.analysis.Fix;

/*Check for the presence of fscanf functions*/

var FscanfChecker = function() {
      // Parent constructor
    Checker.call(this, "fscanf");
    this.advice = " If there is no limit specification in the format specifier an attacker can cause buffer overflows. Use a size limitation in format specification (CWE-120, CWE-20).\n\n";
};

FscanfChecker.prototype = Object.create(Checker.prototype);

FscanfChecker.prototype.check = function($node) {
	if ((!$node.instanceOf("call")) || ($node.name !== 'fscanf')) {
      	return;
    }
	return new CheckResult(this.name, $node, this.advice);
}
