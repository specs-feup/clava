import clava.hls.TraceInstrumentation;
import clava.hls.MathAnalysis;
import weaver.Query;

/**
 * @class
 */
var HLSAnalysis = {};

HLSAnalysis.getJavaInterface = function() {
	return Java.type("pt.up.fe.specs.clava.weaver.hls.HighLevelSynthesisAPI");
}

/**
 * 		Applies a set of Vivado HLS directives to a provided function using a set
 * of strategies to select and configure those directives. This function applies the
 * main optimization flow, which is comprised of the following strategies, by this order:
 * - Function Inlining
 * - Array Streaming
 * - Loop strategies (loop unrolling, pipelining and array partitioning)
 * For a description of how each strategy works, as well as for a standalone version of 
 * each of these strategies, please consult the other methods of this class.
 * 
 * @param {Function} func - a JoinPoint of the function to analyze
 * @param {JSON} options - an optional argument to specify the HLS options. The format
 * is the following: {"B": 2, "N": 32, "P": 64}
 * If not specified, the compiler will use the values provided in the example above.
 * 
 */
HLSAnalysis.applyGenericStrategies = function(func, options) {
	if (options == null)
		options = "{}";
	else
		options = JSON.stringify(options);
	HLSAnalysis.getJavaInterface().applyGenericStrategies(unwrapJoinPoint(func), options);
}


/**
 * 		Applies the function inlining directive to every called function.
 * It works by calculating the cost of both the called function and the callee,
 * in which cost is defined as the total number of array loads on the total lifetime
 * of the function. This is then fed to the formula:
 *
 *                calleeCost > (callFrequency * calledCost) / B
 *
 * If this function is true, the function is inlined. Factor B is configurable, and allows
 * for this formula to be more restrictive/permissive, based on the user's needs. The 
 * default value is 2.
 * 
 * 
 * @param {Function} func - a JoinPoint of the function to analyze
 * @param {number} B - a positive real number to control the heuristic's aggressiveness
 * */
HLSAnalysis.applyFunctionInlining = function(func, B) {
	HLSAnalysis.getJavaInterface().applyFunctionInlining(unwrapJoinPoint(func), B.toString());
}

/**
 * 		This strategy analyzes every input/output array of the function, and
 * tries to see if they can be implemented as a FIFO. To do this, the strategy
 * makes a series of checks to see whether the array can be implemented as such.
 * These checks are:
 * - Check if the array only has either loads or stores operations
 * - Check if the array is always accessed sequentially (limited by the information
 * available at compile time)
 * - Check whether each array position is accessed only once during the entire function
 * lifetime
 * If all these checks apply, the array is implemented as a FIFO using a Vivado HLS directive.
 * 
 * @param {Function} func - a JoinPoint of the function to analyze
 * */
HLSAnalysis.applyArrayStreaming = function(func) {
	HLSAnalysis.getJavaInterface().applyArrayStreaming(unwrapJoinPoint(func));
}

/**
 * 		Analyzes every loop nest of a function and applies loop optimizations. These
 * optimizations are a combination of loop unrolling and loop pipelining. For the latter
 * to be efficient, array partitioning is also applied. This strategy works by always
 * unrolling the innermost loop of every nest, with a resource limitation directive to
 * prevent excessive resource usage. Then, if the number of iterations of the outermost
 * loop is less than 4, that loop is also unrolled, and so on. However, this is an edge case,
 * since this rarely happens; the outermost loop is, instead, pipelined. The array partitioning
 * is done in two ways: if an array is less than 4096 bytes, it is mapped into registers; otherwise,
 * it is partitioned into P partitions using a cyclic strategy.
 * 
 * 
 * @param {Function} func - a JoinPoint of the function to analyze
 * @param {number} P - the number of partitions to use. 64 is the default.
 * */
HLSAnalysis.applyLoopStrategies = function(func, P) {
	HLSAnalysis.getJavaInterface().applyLoopStrategies(unwrapJoinPoint(func), P.toString());
}

/**
 * 		Applies the load/stores strategy to simple loops. A simple loop is defined as
 * a function with only one loop with no nests, and in which every array is either only
 * loaded from or stored to in each iteration. This method can validate whether the provided
 * function is a simple loop or not. If it is one, then it applies three HLS directives in tandem:
 * it unrolls the loop by a factor N (called the load/stores factor), it partitions each array by
 * that same factor N using a cyclic strategy and finally it pipelines the loop. This strategy is
 * better than the other generic strategies for this type of function. Generally, larger values lead
 * to a better speedup, although there are exceptions. Therefore, it is recommended for users to
 * experiment with different values if results are unsatisfactory (the default value is 32).
 * 
 * 
 * @param {Function} func - a JoinPoint of the function to analyze
 * @param {number} N - a positive integer value for the load/stores factor
 * */
HLSAnalysis.applyLoadStoresStrategy = function(func, N) {
	HLSAnalysis.getJavaInterface().applyLoadStoresStrategy(unwrapJoinPoint(func), N.toString());
}

/**
 * 		Checks whether a function can be instrumented. Only workds for old versions
 * of the trace2c tool.
 * 
 * @param {Function} func - a JoinPoint of the function to analyze
 * @return {boolean} whether the function can be or not instrumented
 * */
HLSAnalysis.canBeInstrumented = function(func) {
	return HLSAnalysis.getJavaInterface().canBeInstrumented(unwrapJoinPoint(func));
}
