import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";
import Query from "lara-js/api/weaver/Query.js";
import { BinaryOp, Joinpoint } from "../../Joinpoints.js";
import SimplifyAssignment from "../code/SimplifyAssignment.js";
import StatementDecomposer from "../code/StatementDecomposer.js";
import DecomposeDeclStmt from "../pass/DecomposeDeclStmt.js";
import DecomposeVarDeclarations from "../pass/DecomposeVarDeclarations.js";
import LocalStaticToGlobal from "../pass/LocalStaticToGlobal.js";
import SimplifyLoops from "../pass/SimplifyLoops.js";
import SimplifyReturnStmts from "../pass/SimplifyReturnStmts.js";
import SimplifySelectionStmts from "../pass/SimplifySelectionStmts.js";

/**
 *
 * @param $startJp -
 * @param options - Object with options. See default value for supported options.
 */
export default function NormalizeToSubset(
  $startJp: Joinpoint,
  options = { simplifyLoops: { forToWhile: true } }
) {
  const _options = options;

  const declStmt = new DecomposeDeclStmt();
  const varDecls = new DecomposeVarDeclarations();
  const statementDecomposer = new StatementDecomposer();
  const simplifyLoops = new SimplifyLoops(
    statementDecomposer,
    _options["simplifyLoops"]
  );
  const simplifyIfs = new SimplifySelectionStmts(statementDecomposer);
  const simplifyReturns = new SimplifyReturnStmts(statementDecomposer);
  const localStaticToGlobal = new LocalStaticToGlobal();

  simplifyLoops.apply($startJp);
  simplifyIfs.apply($startJp);
  simplifyReturns.apply($startJp);

  declStmt.apply($startJp);
  varDecls.apply($startJp);
  localStaticToGlobal.apply($startJp);

  for (const $jp of Query.searchFrom($startJp, "binaryOp", {
    self: (self: LaraJoinPoint) =>
      self instanceof BinaryOp && self.isAssignment && self.operator !== "=",
  })) {
    const $assign = $jp as BinaryOp;
    SimplifyAssignment($assign);
  }
}