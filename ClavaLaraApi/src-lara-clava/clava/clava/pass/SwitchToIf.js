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
        const $switchEndGoTo = ClavaJoinPoints.gotoStmt($switchEndLabel);
        $jp.insertAfter($switchEndLabel);

        // TODO: handle case stmts with more that one value
        // Create 'if' statement for each case
        let caseLabels = new Map();
        let switchConditions = new Array();
        for (const $case of $jp.cases) {
            const labelName = $case.isDefault ? "case_" + $case.astId : "case_" + $case.astId;
            const $labelDecl = ClavaJoinPoints.labelDecl(labelName);
            const $goto = ClavaJoinPoints.gotoStmt($labelDecl);
            caseLabels.set($case.astId, labelDecl);
            
            if ($case.isDefault) {
                switchConditions.push($goto);
                continue;
            }

            const $type = ClavaJoinPoints.type("boolean");
            const $binOp = ClavaJoinPoints.binaryOp("==", $switchCondition, $case.values[0], $type);
            const $ifStmt = ClavaJoinPoints.ifStmt($binOp, $goto);

            switchConditions.push($ifStmt);
        }

        $jp.insertBefore(switchConditions[0]);
        this.#moveDefaultToEnd(switchConditions);
        for (let i = 0; i < switchConditions.length - 1; i++) {
            const $ifStmt = switchConditions[i];
            const $nextIfStmt = switchConditions[i + 1];
            $ifStmt.setElse($nextIfStmt);            
        }

        for (const $case of $jp.cases) { 
            const $caseLabel = caseLabels.get($case.astId);
            $jp.insertBefore($caseLabel);

            for (const $inst of $case.instructions) {
                $jp.insertBefore($inst);

                const $breakStmts = Query.searchFromInclusive($inst, "break");
                for (const $break of $breakStmts) {
                    if($break.enclosingStmt.instanceOf("switch"))
                        $break.replaceWith($switchEndGoTo);
                }
            }
        }

        $jp.detach();
        //return new PassResult(this, $firstDeclStmt);
    }

    #moveDefaultToEnd(switchConditions) {
        const index = switchConditions.findIndex($condition => $condition.instanceOf("gotoStmt"));
        if (index !== -1)
            switchConditions.push(array.splice(index, 1)[0]);
    }
}
