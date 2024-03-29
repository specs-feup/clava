laraImport("weaver.Query");
laraImport("clava.ClavaJoinPoints");
laraImport("lara.pass.SimplePass");
laraImport("lara.pass.results.PassResult");


/**
 * Transforms a switch statement into an if statement.
 * 
 * This means that code like this:
 * 
 * ```c
 *	int num = 1, a;
 *	
 *	switch (num) {
 *	    case 1:
 *          a = 10;
 *	   default:
 *	   	    a = 30;
 *	        break;
 *	   case 2:
 *	       a = 20;
 *	       break;
 *     case 3 ... 7:
 *         a = 30;
 *	}
 * ```
 * 
 * Will be transformed into:
 * 
 * ```c
 *  int num = 1, a;
 * 
 *  if (num == 1) 
 *      goto case_1;
 *  else if (num == 2) 
 *      goto case_2;
 *  else if (num >= 3 && num <= 7) 
 *       goto case_3_7;
 *  else 
 *       goto case_default;
 * 
 *  case_1:
 *      a = 10;
 *  case_default:
 *      a = 80;
 *      goto switch_exit;
 *  case_2:
 *      a = 20;
 *      goto switch_exit;
 *  case_3_7:
 *      a = 30;
 * 
 *  switch_exit:
 *  ;
 * ```
 */
class TransformSwitchToIf extends SimplePass {
    /**
     * Maps each case statement id to the corresponding label statement
     */
    #caseLabels;

    /**
     * A list with the corresponding if statement for each case in the switch statement. For the default case, the list keeps its goto statement
     */
    #caseIfStmts;

    /**
     * {boolean} If true, uses deterministic ids for the labels (e.g. switch_exit_0, sw1_case_3...). Otherwise, uses $jp.astId whenever possible.
     */
    #deterministicIds;

    /**
     * Current switch id, in case deterministic ids are used
     */
    #currentId;

    /**
     * @param {boolean} [deterministicIds = false] If true, uses deterministic ids for the labels (e.g. switch_exit_0, sw1_case_3, ...). Otherwise, uses $jp.astId whenever possible.
     */
    constructor(deterministicIds = false) {
        super();
        this.#deterministicIds = deterministicIds;
        this.#currentId = 0;
    }

    /**
     * @return {string} Name of the pass
     * @override
     */
    get name() {
        return "TransformSwitchToIf";
    }

    matchJoinpoint($jp) {
        return $jp.instanceOf("switch");
    }

    /**
     * Transformation to be applied to matching joinpoints
     * @override
     * @param {JoinPoint} $jp Join point to transform
     */
    transformJoinpoint($jp) {
        this.#currentId++;

        const exitName = "switch_exit_" + (this.#deterministicIds ? this.#currentId : $jp.astId);
        const $switchExitLabel = ClavaJoinPoints.labelDecl(exitName);
        const $switchExitLabelStmt = ClavaJoinPoints.labelStmt($switchExitLabel);
        const $switchExitGoTo = ClavaJoinPoints.gotoStmt($switchExitLabel);

        // Insert the switch exit label and an empty statement if needed
        $jp.insertAfter($switchExitLabelStmt);
        if ($switchExitLabelStmt.isLast)
            $switchExitLabelStmt.insertAfter(ClavaJoinPoints.emptyStmt());

        this.#computeIfAndLabels($jp);
        
        if ($jp.hasDefaultCase)
            this.#moveDefaultToEnd();
        else
            this.#caseIfStmts.push($switchExitGoTo);

        $jp.insertBefore(this.#caseIfStmts[0]);

        this.#linkIfStmts();
        this.#replaceBreakWithGoto($jp, $switchExitGoTo);
        this.#addLabelsAndInstructions($jp);

        $jp.detach();
        return new PassResult(this, this.#caseIfStmts[0]);
    }

    /**
     * Generates the label based on the switch ID and the values of the provided case statement.
     * If no switch ID is provided, a generic label based on the case statement's AST ID is returned.
     * @param {joinpoint} $caseStmt the case statement
     * @returns {string} the generated label name for the provided case statement
     */
    #computeLabelName($caseStmt) {
        if (!this.#deterministicIds)
            return "case_" + $caseStmt.astId;

        let labelName = "sw" + this.#currentId;
        if ($caseStmt.isDefault)
           labelName += "_default";
        else if ($caseStmt.values.length == 1)
            labelName += `_case_${$caseStmt.values[0].code}`;
        else
           labelName += `_case_${$caseStmt.values[0].code}_to_${$caseStmt.values[1].code}`;
        return labelName;
    }

    /**
     * Creates if and label statements for each case in the provided switch statement and adds them to the private fields "caseIfStmts" and "caseLabels".
     * @param {joinpoint} $jp the switch statement
     */
    #computeIfAndLabels($jp) {
        const $switchCondition = $jp.condition;
        this.#caseLabels = new Map();
        this.#caseIfStmts = new Array();

        for (const $case of $jp.cases) {
            const labelName = this.#computeLabelName($case);
            const $labelDecl = ClavaJoinPoints.labelDecl(labelName);
            const $goto = ClavaJoinPoints.gotoStmt($labelDecl);

            const $labelStmt = ClavaJoinPoints.labelStmt($labelDecl);
            this.#caseLabels.set($case.astId, $labelStmt);
            
            if ($case.isDefault) {
                this.#caseIfStmts.push($goto);
                continue;
            }

            let $ifCondition;
            if ($case.values.length == 1)
                $ifCondition = ClavaJoinPoints.binaryOp("==", $switchCondition, $case.values[0], "boolean");
            else {
                const $binOpGE = ClavaJoinPoints.binaryOp(">=", $switchCondition, $case.values[0], "boolean");
                const $binOpLE = ClavaJoinPoints.binaryOp("<=", $switchCondition, $case.values[1], "boolean");
                $ifCondition = ClavaJoinPoints.binaryOp("&&", $binOpGE, $binOpLE, "boolean"); 
            }
            const $ifStmt = ClavaJoinPoints.ifStmt($ifCondition, $goto);
            this.#caseIfStmts.push($ifStmt);
        }
    }

    /**
     * Reorders the private field "caseIfStmts" by moving the goto statement of the intermediate default case to the end.
     */
    #moveDefaultToEnd() {
        const index = this.#caseIfStmts.findIndex($condition => $condition.instanceOf("gotoStmt"));
        if (index !== -1)
            this.#caseIfStmts.push(this.#caseIfStmts.splice(index, 1)[0]);
    }

    /**
     * Links the statements stored in the private field "caseIfStmts" by setting the body of their else as the next statement in the list
     */
    #linkIfStmts() {
        for (let i = 0; i < this.#caseIfStmts.length - 1; i++) {
            const $ifStmt = this.#caseIfStmts[i];
            const $nextIfStmt = this.#caseIfStmts[i + 1];
            $ifStmt.setElse($nextIfStmt);            
        }
    }

    /**
     * Replaces the break statements that refer to the exit of the provided switch statement with goto statements.
     * @param {joinpoint} $jp the switch statement
     * @param {joinpoint} $switchExitGoTo the goto statement that corresponds to the switch exit. This statement will be used to replace the break statements 
     */
    #replaceBreakWithGoto($jp, $switchExitGoTo) {
        const $breakStmts = Query.searchFromInclusive($jp, "break", {enclosingStmt: enclosingStmt => enclosingStmt.astId === $jp.astId});
        for (const $break of $breakStmts)
            $break.replaceWith($switchExitGoTo);
    }

    /**
     * Inserts the label and instructions of each case in the provided switch statement
     * @param {joinpoint} $jp the switch statement
     */
    #addLabelsAndInstructions($jp) {
        for (const $case of $jp.cases) { 
            const $caseLabel = this.#caseLabels.get($case.astId);
            $jp.insertBefore($caseLabel);

            for (const $inst of $case.instructions)
                $jp.insertBefore($inst);
        }
    }
}
