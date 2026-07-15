import Query from "@specs-feup/lara/api/weaver/Query.ts";
import { BinaryOp, Joinpoint } from "../../Joinpoints.ts";
import SimplifyAssignment from "../code/SimplifyAssignment.ts";
import StatementDecomposer from "../code/StatementDecomposer.ts";
import DecomposeDeclStmt from "../pass/DecomposeDeclStmt.ts";
import DecomposeVarDeclarations from "../pass/DecomposeVarDeclarations.ts";
import LocalStaticToGlobal from "../pass/LocalStaticToGlobal.ts";
import SimplifyLoops from "../pass/SimplifyLoops.ts";
import SimplifyReturnStmts from "../pass/SimplifyReturnStmts.ts";
import SimplifySelectionStmts from "../pass/SimplifySelectionStmts.ts";

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

  for (const $assign of Query.searchFrom(
    $startJp,
    BinaryOp,
    (jp) => jp.isAssignment && jp.operator !== "="
  )) {
    SimplifyAssignment($assign);
  }
}
