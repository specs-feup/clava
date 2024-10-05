laraImport("clava.ClavaJoinPoints");
laraImport("clava.Clava");
laraImport("lara.code.Logger");
laraImport("lara.util.TupleId");
laraImport("weaver.Query");

/**
 * Inserts code for increments as a comma operator,
 * as suggested by Pedro Silva (up201304961@fc.up.pt)
 */
const _DCG_USE_COMMA_OPERATOR_ = true;

let graphFile;

/**
 * Instruments code in order to build a runtime call graph.
 *
 * If _DCG_USE_COMMA_OPERATOR_ is true, it will take into account execution
 * when functions are called as part of a short-circuit operation,
 * e.g., foo() || foo2(), when foo() returns true, the call to foo2() is not counted.
 * However, if for any reason the comma operator cannot be used,
 * set _DCG_USE_COMMA_OPERATOR_ to false (it is true by default),
 * and an alternative method is used that will not take into account short-circuit operators.
 */

const tupleId = new TupleId();

const dcgName = "clava_dcg_global";

/* Instrument function calls and increment the corresponding position */
for (const chain of Query.search("function").search("call").chain()) {
    const $function = chain["function"];
    const $call = chain["call"];

    const id = tupleId.getId($function.name, $call.name);

    insertIncrement($call, dcgName + "[ " + id + " ]++");
}

// Get tuples and count them
const tuples = tupleId.getTuples();
let total = 0;
for (const key in tuples) {
    total++;
}

/* Declare the array in each file */
for (const chain of Query.search("file").search("function").chain()) {
    const $file = chain["file"];
    const $function = chain["function"];

    if ($file.hasMain) {
        $function.insertBefore(`int ${dcgName}[ ${total} ] = {0};`);
    } else {
        $function.insertBefore(`extern int ${dcgName}[ ${total} ];`);
    }
    break;
}

// Build function to print call graph
const callgraphFunctionName = "clava_call_graph";
const $callgraph = ClavaJoinPoints.functionDecl(
    callgraphFunctionName,
    ClavaJoinPoints.builtinType("void")
);
$callgraph.setBody(ClavaJoinPoints.scope());

// Using a comment as a marker for log insertions
const $markerStmt = $callgraph.body.insertBegin(
    ClavaJoinPoints.stmtLiteral("// MARKER")
);

// Insert function before main

for (const $function of Query.search("function", "main")) {
    // Just before main declaration
    if (!$function.hasDefinition) {
        continue;
    }

    $function.insertBefore($callgraph);

    // Insert only once
    break;
}

const graphLogger = new Logger(false, graphFile);
graphLogger.append("digraph dynamic_call_graph {").ln().ln().log($markerStmt);

let $lastStmt = graphLogger.getAfterJp();

for (const id in tuples) {
    const tuple = tuples[id];

    const dcgCount = dcgName + "[" + id + "]";
    $lastStmt = $lastStmt.insertAfter(
        ClavaJoinPoints.ifStmt(dcgCount + " != 0")
    );

    graphLogger
        .append("\t" + tuple[0] + " -> " + tuple[1] + ' [label=\\"')
        .int(dcgCount)
        .append('\\"];')
        .ln()
        .logBefore($lastStmt.then);
}

graphLogger.append("}").ln().log($lastStmt);

// Remove marker stmt
$markerStmt.detach();

// Register function to be executed when program exits
Clava.getProgram().atexit($callgraph);

console.log(Clava.getProgram().code);

function insertIncrement($call, code) {
    // Increments using comma operator
    if (_DCG_USE_COMMA_OPERATOR_) {
        // First, replace call with parenthesis
        const $parenthesis = $call.replaceWith(
            ClavaJoinPoints.parenthesis("/* DCG TEMP */")
        );

        // Create comma operator
        const $commaOp = ClavaJoinPoints.binaryOp(
            "comma",
            code,
            $call,
            $call.type
        );

        // Replace parenthesis child
        $parenthesis.firstChild = $commaOp;

        return;
    }

    // Increments using statements, add ;
    code += ";";

    // If call is inside a loop header (e.g., for, while),
    // insert increment at the beginning of the loop body
    if ($call.isInsideLoopHeader) {
        const $loop = $call.getAncestor("loop");
        checkDefined($loop);
        $loop.body.insertBegin(code);
        return;
    }

    $call.insertBefore(code);
}
