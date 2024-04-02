export default class DecomposeResult {
    precedingStmts;
    $resultExpr;
    succeedingStmts;
    constructor(precedingStmts, $resultExpr, succeedingStmts = []) {
        this.precedingStmts = precedingStmts;
        this.$resultExpr = $resultExpr;
        this.succeedingStmts = succeedingStmts;
    }
    /**
     * Represents the statements to be placed before the use of the expression
     * @deprecated use `precedingStmts` instead
     */
    get stmts() {
        return this.precedingStmts;
    }
    toString() {
        const precedingCode = this.precedingStmts
            .map((stmt) => stmt.code)
            .join(" ");
        return `${precedingCode} -> ${this.$resultExpr.code}`;
    }
}
//# sourceMappingURL=DecomposeResult.js.map