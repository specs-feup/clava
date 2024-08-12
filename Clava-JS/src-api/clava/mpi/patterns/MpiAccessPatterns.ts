import ScalarPattern from "./ScalarPattern.js";
import IterationVariablePattern from "./IterationVariablePattern.js";


/**
 * Available MPI access patterns.
 *
 */
export default class MpiAccessPatterns {

/**
 * Access to a scalar variable.
 */
static SCALAR_PATTERN = new ScalarPattern();

/**
 * Array that is accessed using only the iteration variable directly, without modifications.
 */
static ITERATION_VARIABLE = new IterationVariablePattern();

}
