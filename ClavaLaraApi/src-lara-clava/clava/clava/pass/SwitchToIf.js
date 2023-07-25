laraImport("lara.pass.Pass");
laraImport("weaver.Query");
laraImport("clava.ClavaJoinPoints");
laraImport("lara.Strings");

class SwitchToIf extends SimplePass {
    /**
     * @return {string} Name of the pass
     * @override
     */
    get name() {
        return "SwitchToIf";
    }

    matchJoinpoint($jp) {
        return $jp.instanceOf("switch");
    }

    transformJoinpoint($jp) {
        const $switchCondition = $jp.condition;
        const $switchEndLabel = ClavaJoinPoints.labelDecl("end_switch_" + $jp.astId);
        const $switchEndLabelStmt = ClavaJoinPoints.labelStmt($switchEndLabel);
        const $switchEndGoTo = ClavaJoinPoints.gotoStmt($switchEndLabel);

        // Create 'if' statement for each case
        let caseLabels = new Map();
        let switchConditions = new Array();
        for (const $case of $jp.cases) {
            const labelName = $case.isDefault ? "case_" + $case.astId : "case_" + $case.astId;
            const $labelDecl = ClavaJoinPoints.labelDecl(labelName);
            const $goto = ClavaJoinPoints.gotoStmt($labelDecl);

            const $labelStmt = ClavaJoinPoints.labelStmt($labelDecl);
            caseLabels.set($case.astId, $labelStmt);
            
            if ($case.isDefault) {
                switchConditions.push($goto);
                continue;
            }

            let $ifCondition;
            if($case.values.length == 1)
                $ifCondition = ClavaJoinPoints.binaryOp("==", $switchCondition, $case.values[0], "boolean");
            else {
                const $binOpGE = ClavaJoinPoints.binaryOp(">=", $switchCondition, $case.values[0], "boolean");
                const $binOpLE = ClavaJoinPoints.binaryOp("<=", $switchCondition, $case.values[1], "boolean");
                $ifCondition = ClavaJoinPoints.binaryOp("&&", $binOpGE, $binOpLE, "boolean"); 
            }
            const $ifStmt = ClavaJoinPoints.ifStmt($ifCondition, $goto);

            switchConditions.push($ifStmt);
        }

        $jp.insertBefore(switchConditions[0]);

        // Move intermediate default to the end
        this.#moveDefaultToEnd(switchConditions);

        // Connect the switch conditions
        for (let i = 0; i < switchConditions.length - 1; i++) {
            const $ifStmt = switchConditions[i];
            const $nextIfStmt = switchConditions[i + 1];
            $ifStmt.setElse($nextIfStmt);            
        }

        // Replace break statements with goto statements
        const $breakStmts = Query.searchFromInclusive($jp, "break", {enclosingStmt: enclosingStmt => enclosingStmt.instanceOf("switch")});
        for (const $break of $breakStmts)
            $break.replaceWith($switchEndGoTo);

        // Insert label and instructions of each case statement
        for (const $case of $jp.cases) { 
            const $caseLabel = caseLabels.get($case.astId);
            $jp.insertBefore($caseLabel);

            for (const $inst of $case.instructions)
                $jp.insertBefore($inst);
        }

        $jp.insertAfter($switchEndLabelStmt);
        if ($switchEndLabelStmt.isLast)
            $switchEndLabelStmt.insertAfter(ClavaJoinPoints.emptyStmt());
        $jp.detach();
        return new PassResult(this, switchConditions[0]);
    }

    #moveDefaultToEnd(switchConditions) {
        const index = switchConditions.findIndex($condition => $condition.instanceOf("gotoStmt"));
        if (index !== -1)
            switchConditions.push(switchConditions.splice(index, 1)[0]);
    }
}
