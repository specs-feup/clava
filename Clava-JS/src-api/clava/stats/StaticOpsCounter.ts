import PrintOnce from "lara-js/api/lara/util/PrintOnce.js";
import Query from "lara-js/api/weaver/Query.js";
import {
  BinaryOp,
  BuiltinType,
  Call,
  Expression,
  FunctionJp,
  Joinpoint,
  Loop,
  Op,
  Param,
  Statement,
  Type,
  Vardecl,
  Varref,
} from "../../Joinpoints.js";
import OpsBlock from "./OpsBlock.js";

export default class StaticOpsCounter {
  // Whitelist of ops
  private static validOps = new Set<string>([
    "mul",
    "div",
    "rem",
    "add",
    "sub",
    "shl",
    "shr",
    "cmp",
    "and",
    "xor",
    "or",
    "l_and",
    "l_or",
    "mul_assign",
    "div_assign",
    "rem_assign",
    "add_assign",
    "sub_assign",
    "shl_assign",
    "shr_assign",
    "and_assign",
    "xor_assign",
    "or_assign",
    "post_inc",
    "post_dec",
    "pre_inc",
    "pre_dec",
  ]);

  private instrumentedFunctions = new Set<string>();
  private filterFunction: (op: Op) => boolean;

  constructor(filterFunction: (op: Op) => boolean = ($op: Op) => true) {
    this.filterFunction = filterFunction;
  }

  count($fn: Joinpoint, opsBlock: OpsBlock, includeOpKind: boolean = false) {
    const $function =
      $fn instanceof FunctionJp
        ? $fn
        : ($fn.getAncestor("function") as FunctionJp | undefined);

    if ($function === undefined) {
      PrintOnce.message(
        `StaticOpsCounter.count: Could not find function corresponding to the join point ${$fn.location}`
      );
      return;
    }

    const functionId = `${$function.name}@${$function.location}`;

    // Check if it is already instrumented
    if (this.instrumentedFunctions.has(functionId)) {
      // TODO: Support recursive function calls
      return;
    }

    this.instrumentedFunctions.add(functionId);

    console.log(
      "StaticOpsCounter.count: Estimating ops of function " + functionId
    );

    opsBlock ??= new OpsBlock(functionId);

    // Go statement-by-statement
    $function.body.children.forEach(($stmt) => {
      this.countOpStatic($stmt as Statement, opsBlock, includeOpKind);
    });

    return opsBlock;
  }

  private countOpStatic(
    $stmt: Statement,
    opsBlock: OpsBlock,
    includeOpKind: boolean
  ) {
    // If stmt is a loop, count new block, recursively

    if ($stmt == undefined) {
      return;
    }

    if ($stmt instanceof Loop) {
      if ($stmt.kind !== "for") {
        console.log(
          `Ignoring loops that are not 'fors' (location ${$stmt.location}) for now`
        );
        return;
      }

      const rank = $stmt.rank;
      const nestedId = `${opsBlock.id} => ${rank[rank.length - 1]}`;

      // Create block for loop
      const nestedOpsBlock = new OpsBlock(nestedId);

      this.countOpStatic($stmt.init, opsBlock, includeOpKind);
      this.countOpStatic($stmt.cond, nestedOpsBlock, includeOpKind);
      this.countOpStatic($stmt.step, nestedOpsBlock, includeOpKind);

      // Extract iterations
      const iter = $stmt.iterationsExpr;
      let replacementsMap: Record<string, Expression> = {};
      do {
        replacementsMap = this.analyseIterationsExpr(iter, $stmt);

        for (const rep in replacementsMap) {
          for (const $jp of iter.descendants) {
            if ($jp.code === rep) {
              $jp.replaceWith(replacementsMap[rep]); // TODO: Do calculation without altering the source code.
            }
          }
        }
      } while (Object.keys(replacementsMap).length > 0);
      nestedOpsBlock.repetitions = iter.code;

      // Add to nested blocks
      opsBlock.nestedOpsBlocks.push(nestedOpsBlock);

      // Go statement-by-statement
      $stmt.body.children.forEach(($nestedStmt) => {
        this.countOpStatic(
          $nestedStmt as Statement,
          nestedOpsBlock,
          includeOpKind
        );
      });

      return;
    }

    // If stmt is not a loop, count ops
    // Apply to all ops found in the stmt
    for (const $jp of Query.searchFrom($stmt, Op)) {
      const $op = $jp as Op;
      // If not a valid op, continue
      if (!this.isValidOp($op)) {
        continue;
      }

      // Calculate type and bitwidth
      const $builtinType = this.toBuiltinType($op.type);
      const counterType = this.getCounterType($builtinType);
      const bitwidth =
        $builtinType !== undefined ? String($op.bitWidth) : undefined;

      // Increment counter
      let opsId = `${counterType}-${bitwidth}`;
      if (includeOpKind) {
        opsId += `-${$op.kind}`;
      }
      opsBlock.add(opsId);
    }

    // Call function recursively when function calls are found
    for (const $jp of Query.searchFrom($stmt, Call)) {
      const $call = $jp as Call;
      const $funcDef = $call.definition;

      if ($funcDef === undefined) {
        continue;
      }

      this.count($funcDef, opsBlock, includeOpKind);
    }
  }

  private getCounterType($builtinType?: BuiltinType) {
    if ($builtinType === undefined) {
      return undefined;
    }

    if ($builtinType.isFloat) {
      return "flops";
    } else if ($builtinType.isInteger) {
      return "iops";
    } else {
      PrintOnce.message(
        `StaticOpsCounter: could not determine if builtinType ${$builtinType.kind} is integer or float`
      );
      return undefined;
    }
  }

  private toBuiltinType($type: Type) {
    if ($type instanceof BuiltinType) {
      return $type;
    }

    PrintOnce.message(
      `StaticOpsCounter: could not determine builtinType of ${$type.joinPointType}`
    );
    return undefined;
  }

  private isValidOp($op: Op) {
    const isValid = StaticOpsCounter.validOps.has($op.kind);

    if (!isValid) {
      return false;
    }

    if (!this.filterFunction($op)) {
      return false;
    }

    return true;
  }

  private analyseIterationsExpr(
    $expr: Expression,
    $source: Loop
  ): Record<string, Expression> {
    const result: Record<string, Expression> = {};

    for (const $jp of Query.searchFromInclusive($expr, Varref)) {
      const $varref = $jp as Varref;
      if (result[$varref.name] !== undefined) {
        continue;
      }
      if ($varref.decl instanceof Param) {
        console.log(`Var ${$varref.name} is a parameter`);
        continue;
      }
      console.log(`REFS of ${$varref.name}`);
      const $lastWrite = this.getLastWrite($source, $varref.vardecl);

      if ($lastWrite === undefined) {
        console.log("Could not find last write");
        continue;
      }

      console.log(`Last write of ${$varref.vardecl.name}: ${$lastWrite.code}`);
      result[$varref.name] = $lastWrite;
    }
    return result;
  }

  private getLastWrite(
    $currentJp: Joinpoint | undefined,
    $vardecl: Vardecl
  ): Expression | undefined {
    if ($currentJp === undefined) {
      console.log("Could not find declaration");
      return undefined;
    }

    // Get siblings on the left
    const siblLeft = $currentJp.siblingsLeft;

    // Go back until the variable declaration/parameter is found
    for (let i = siblLeft.length - 1; i >= 0; i--) {
      const sibl = siblLeft[i];

      // For each sibling, find write references to the variable
      const refs = (sibl.getDescendantsAndSelf("varref") as Varref[]).filter(
        (varref) => varref.name === $vardecl.name
      );

      for (const $ref of refs) {
        // Ignore
        if ($ref.use === "read") {
          continue;
        }

        // Not supported yet
        if ($ref.use === "readwrite") {
          console.log("Readwrite not supported yet");
          return undefined;
        }

        // Check if assignment
        const $refParent = $ref.parent as Op;
        if ($refParent.kind !== "assign") {
          console.log("Not supported when not an assignment");
          return undefined;
        }

        if ($refParent instanceof BinaryOp) {
          return $refParent.right;
        }
      }
      // Check vardecl
      const decls = (sibl.getDescendantsAndSelf("vardecl") as Vardecl[]).filter(
        (vardecl) => vardecl.equals($vardecl)
      );
      for (const $decl of decls) {
        // Found decl
        if (!$decl.hasInit) {
          console.log(
            `Variable declaration for ${$decl.name} has no initialization`
          );
          return undefined;
        }

        return $decl.init;
      }
    }
    // Did not find declaration yet, call on parent
    return this.getLastWrite($currentJp.parent, $vardecl);
  }
}
