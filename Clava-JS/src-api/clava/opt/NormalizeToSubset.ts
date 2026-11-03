import Query from "@specs-feup/lara/api/weaver/Query.js";
import type { Joinpoint } from "../../Joinpoints.js";
import { BinaryOp } from "../../Joinpoints.js";
import SimplifyAssignment from "../code/SimplifyAssignment.js";
import StatementDecomposer from "../code/StatementDecomposer.js";
import DecomposeDeclStmt from "../pass/DecomposeDeclStmt.js";
import DecomposeVarDeclarations from "../pass/DecomposeVarDeclarations.js";
import LocalStaticToGlobal from "../pass/LocalStaticToGlobal.js";
import SimplifyLoops from "../pass/SimplifyLoops.js";
import SimplifyReturnStmts from "../pass/SimplifyReturnStmts.js";
import SimplifySelectionStmts from "../pass/SimplifySelectionStmts.js";

/**
 * Normalizes code to a simpler subset of C/C++.
 * 
 * @param $startJp - Starting join point for normalization
 * @param options - Configuration options for normalization
 */
export default function NormalizeToSubset(
  $startJp: Joinpoint,
  options: { simplifyLoops?: { forToWhile: boolean }, useGlobalIds?: boolean } = {}
) {
  const _options = {
    simplifyLoops: { forToWhile: true },
    useGlobalIds: false,
    ...options
  };

  const declStmt = new DecomposeDeclStmt();
  const varDecls = new DecomposeVarDeclarations();
  const statementDecomposer = new StatementDecomposer(
    "decomp_", 
    0, 
    _options.useGlobalIds
  );
  const simplifyLoops = new SimplifyLoops(
    statementDecomposer,
    _options.simplifyLoops
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
