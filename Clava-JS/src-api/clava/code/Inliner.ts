import { LaraJoinPoint } from "@specs-feup/lara/api/LaraJoinPoint.js";
import { debug } from "@specs-feup/lara/api/lara/core/LaraCore.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import {
  BinaryOp,
  Call,
  DeclStmt,
  ExprStmt,
  Expression,
  FunctionJp,
  GotoStmt,
  Joinpoint,
  LabelDecl,
  LabelStmt,
  ParenExpr,
  ParenType,
  PointerType,
  ReturnStmt,
  Scope,
  Statement,
  StorageClass,
  Type,
  Vardecl,
  VariableArrayType,
  Varref,
} from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";

export interface InlinerOptions {
  prefix?: string;
}

type InlineData =
  | {
      type: "assignment";
      $target: Expression;
      $call: Call;
    }
  | {
      type: "call";
      $target: undefined;
      $call: Call;
    };

export default class Inliner {
  private options: InlinerOptions;
  private variableIndex: number = 0;
  private labelNumber: number = 0;

  /**
   *
   * @param  options - Object with options. Supported options: 'prefix' (default: "__inline"), the prefix that will be used in the name of variables inserted by the Inliner
   */
  constructor(options: InlinerOptions = { prefix: "__inline" }) {
    this.options = options;
  }

  private getInlinedVarName(originalVarName: string): string {
    const prefix = this.options["prefix"];
    return `${prefix}_${this.variableIndex}_${originalVarName}`;
  }

  private hasCycle(
    $function: FunctionJp,
    _path: Set<string> = new Set<string>()
  ): boolean {
    if (_path.has($function.name)) {
      return true;
    }

    _path.add($function.name);
    for (const $jp of $function.getDescendants("exprStmt")) {
      const $exprStmt = $jp as ExprStmt;

      const $expr = $exprStmt.expr;
      if (
        !(
          $expr instanceof BinaryOp &&
          $expr.isAssignment &&
          $expr.right instanceof Call
        ) &&
        !($expr instanceof Call)
      ) {
        continue;
      }

      const $call = $expr instanceof Call ? $expr : $expr.right;
      if (
        !($call instanceof Call) ||
        ($call instanceof Call && $call.definition == undefined)
      ) {
        continue;
      }
      if (this.hasCycle($call.definition, _path)) {
        return true;
      }
    }

    _path.delete($function.name);
    return false;
  }

  inlineFunctionTree(
    $function: FunctionJp,
    _visited: Set<string> = new Set<string>()
  ) {
    if ($function === undefined) {
      return false;
    }

    debug("InlineFunctionTree called on " + $function.signature);
    if (this.hasCycle($function)) {
      return false;
    }

    if (_visited.has($function.name)) {
      return true;
    }
    _visited.add($function.name);

    for (const $jp of $function.getDescendants("exprStmt")) {
      const $exprStmt = $jp as ExprStmt;
      const inlineData = this.checkInline($exprStmt);

      if (inlineData === undefined) {
        continue;
      }

      const $callee = inlineData.$call.definition;

      this.inlineFunctionTree($callee, _visited);
      this.inlinePrivate($exprStmt, inlineData);
    }

    return true;
  }

  private getInitStmts($varDecl: Vardecl, $expr: Expression): ExprStmt[] {
    const $assign = ClavaJoinPoints.assign(
      ClavaJoinPoints.varRef($varDecl),
      $expr
    );

    return [ClavaJoinPoints.exprStmt($assign)];
  }

  /**
   *
   * @param $exprStmt -
   * @returns An object with the properties below or undefined if this exprStmt cannot be inlined.
   * Only exprStmt that are an isolated call, or that are an assignment with a single call
   * in the right-hand side can be inlined.
   *
   * - type: a string with either the value 'call' or 'assign', indicating the type of inlining
   * that can be applied to the given exprStmt.
   * - $target: if the type is 'assign', contains the left-hand side of the assignment. Otherwise, is undefined.
   * - $call: the call to be inlined
   *
   */
  private extractInlineData($exprStmt: ExprStmt): InlineData | undefined {
    if (
      $exprStmt.expr instanceof BinaryOp &&
      $exprStmt.expr.isAssignment &&
      $exprStmt.expr.right instanceof Call
    ) {
      return {
        type: "assignment",
        $target: $exprStmt.expr.left,
        $call: $exprStmt.expr.right,
      };
    }

    if ($exprStmt.expr instanceof Call) {
      return {
        type: "call",
        $target: undefined,
        $call: $exprStmt.expr,
      };
    }

    return undefined;
  }

  /**
   * Check if the given $exprStmt can be inlined or not. If it can, returns an object with information important for inlining,
   * otherwise returns undefined.
   *
   * A call can be inline if the following rules apply:
   * - The exprStmt is an isolated call, or an assignment with a single call in the right-hand side.
   * - The call has a definition/implementation available.
   * - The call is not a function that is part of the system headers.
   *
   * @param $exprStmt -
   * @returns An object with the properties below or undefined if this exprStmt cannot be inlined.
   *
   * - type: a string with either the value 'call' or 'assign', indicating the type of inlining
   * that can be applied to the given exprStmt.
   * - $target: if the type is 'assign', contains the left-hand side of the assignment. Otherwise, is undefined.
   * - $call: the call to be inlined
   *
   */
  checkInline($exprStmt: ExprStmt): InlineData | undefined {
    // Extract inline information
    const inlineData = this.extractInlineData($exprStmt);

    if (inlineData === undefined) {
      return undefined;
    }

    // Check if call has an implementation
    if (!inlineData.$call.function.isImplementation) {
      debug(
        `Inliner: call '${inlineData.$call.toString()}' not inlined because implementation was not found`
      );
      return undefined;
    }

    // Ignore functions that are part of the system headers
    if (inlineData.$call.function.isInSystemHeader) {
      debug(
        `Inliner: call '${inlineData.$call.toString()}' not inlined function belongs to a system header`
      );
      return undefined;
    }

    return inlineData;
  }

  inline($exprStmt: ExprStmt): boolean {
    const inlineData = this.checkInline($exprStmt);

    if (inlineData === undefined) {
      return false;
    }

    this.inlinePrivate($exprStmt, inlineData);
    return true;
  }

  private inlinePrivate($exprStmt: ExprStmt, inlineData: InlineData): void {
    debug(
      "InlinePrivate called on " + $exprStmt.code + "@" + $exprStmt.location
    );
    const $target = inlineData.$target;
    const $call = inlineData.$call;

    let args = $call.args;
    if (!Array.isArray(args)) {
      args = [args];
    }

    const $function = $call.function;

    if ($function.getDescendants("returnStmt").length > 1) {
      throw new Error(
        `'${$function.name}' cannot be inlined: more than one return statement`
      );
    }

    const params = $function.params;

    const newVariableMap = new Map<string, Vardecl | Expression>();
    const paramDeclStmts: Statement[] = [];

    // TODO: args can be greater than params, if varargs. How to deal with this?
    for (let i = 0; i < params.length; i++) {
      const $arg = args[i];
      const $param = params[i];

      // Arrays cannot be assigned
      // If param is array or pointer, there is no need to add declaration,
      // simply rename the param to the name of the arg
      //if ($param.type.isArray || $param.type.isPointer) {
      if ($param.type.isArray) {
        newVariableMap.set($param.name, $arg);
      } else {
        const newName = this.getInlinedVarName($param.name);
        const $varDecl = ClavaJoinPoints.varDeclNoInit(newName, $param.type);
        const $varDeclStmt = ClavaJoinPoints.declStmt($varDecl);

        const $initStmts = this.getInitStmts($varDecl, $arg);

        newVariableMap.set($param.name, $varDecl);
        paramDeclStmts.push($varDeclStmt, ...$initStmts);
      }
    }

    for (const jp of $function.body.getDescendants("declStmt")) {
      const stmt = jp as DeclStmt;

      const $varDecl = stmt.decls[0];
      if (!($varDecl instanceof Vardecl)) {
        continue;
      }

      const newName = this.getInlinedVarName($varDecl.name);
      const $newDecl = ClavaJoinPoints.varDeclNoInit(newName, $varDecl.type);
      newVariableMap.set($varDecl.name, $newDecl);
    }

    const $newNodes = $function.body.copy() as Scope;

    this.processBodyToInline($newNodes, newVariableMap, $call);

    // Remove/replace return statements
    if ($exprStmt.expr instanceof BinaryOp && $exprStmt.expr.isAssignment) {
      for (const $jp of $newNodes.getDescendants("returnStmt")) {
        const $returnStmt = $jp as ReturnStmt;

        if ($target === undefined) {
          throw new Error(
            "Expected $target to be defined when exprStmt is an assignment"
          );
        } else if (
          $returnStmt.returnExpr !== null &&
          $returnStmt.returnExpr !== undefined
        ) {
          $returnStmt.replaceWith(
            ClavaJoinPoints.exprStmt(
              ClavaJoinPoints.assign($target, $returnStmt.returnExpr)
            )
          );
        } else {
          $returnStmt.detach();
        }
      }
    } else if ($exprStmt.expr instanceof Call) {
      for (const $returnStmt of $newNodes.getDescendants("returnStmt")) {
        // Replace the return with a nop (i.e. empty statement), in case there is a label before. Otherwise, just remove return
        const left = $returnStmt.siblingsLeft;
        if (left.length > 0 && left[left.length - 1] instanceof LabelStmt) {
          $returnStmt.replaceWith(ClavaJoinPoints.emptyStmt());
        } else {
          $returnStmt.detach();
        }
      }
    }

    // For any calls inside $newNodes, add forward declarations before the function, if they have definition
    // TODO: this should be done for calls of functions that are on this file. For other files, the corresponding include
    // should be added
    const $parentFunction = $call.getAncestor("function") as FunctionJp;
    const addedDeclarations = new Set<string>();
    for (const $newCall of Query.searchFrom($newNodes, Call)) {
      // Ignore functions that are part of the system headers
      if ($newCall.function.isInSystemHeader) {
        continue;
      }

      if (addedDeclarations.has($newCall.function.id)) {
        continue;
      }

      addedDeclarations.add($newCall.function.id);

      const $newFunctionDecl = ClavaJoinPoints.functionDeclFromType(
        $newCall.function.name,
        $newCall.function.functionType
      );

      $parentFunction.insertBefore($newFunctionDecl);
    }

    // Let the function body be on its own scope
    // If the function uses local labels they must appear at the beginning of the scope
    const inlinedScope =
      paramDeclStmts.length === 0
        ? $newNodes
        : ClavaJoinPoints.scope(...paramDeclStmts, $newNodes);

    $exprStmt.replaceWith(inlinedScope);

    this.variableIndex++;
  }

  private processBodyToInline(
    $newNodes: Scope,
    newVariableMap: Map<string, Vardecl | Expression>,
    $call: Call
  ) {
    this.updateVarDecls($newNodes, newVariableMap);
    this.updateVarrefs($newNodes, newVariableMap, $call);
    this.updateVarrefsInTypes($newNodes, newVariableMap, $call);
    this.renameLabels();
  }

  /**
   * Labels need to be renamed, to avoid duplicated labels.
   */
  private renameLabels(): void {
    // Maps label names to new LabelDecl
    const newLabels: Record<string, LabelDecl> = {};

    // Visit all gotoStmt and labelStmt
    for (const jp of Query.search(Joinpoint, {
      self: ($jp: LaraJoinPoint) =>
        $jp instanceof GotoStmt || $jp instanceof LabelStmt,
    })) {
      const $jp = jp as GotoStmt | LabelStmt;

      // Get original label
      const $labelDecl = $jp instanceof GotoStmt ? $jp.label : $jp.decl;

      // Get new label, or create if it does not exist yet
      let $newLabelDecl: LabelDecl | undefined = newLabels[$labelDecl.name];
      if ($newLabelDecl === undefined) {
        const newLabelName = this.createNewLabelName($labelDecl.name);
        $newLabelDecl = ClavaJoinPoints.labelDecl(newLabelName);
        newLabels[$labelDecl.name] = $newLabelDecl;
      }
      // Replace label
      if ($jp instanceof GotoStmt) {
        $jp.label = $newLabelDecl;
      } else {
        $jp.decl = $newLabelDecl;
      }
    }

    // If there are any label decls, rename them
    for (const $labelDecl of Query.search(LabelDecl)) {
      const $newLabelDecl = newLabels[$labelDecl.name];
      $labelDecl.replaceWith($newLabelDecl);
    }
  }

  static LABEL_PREFIX_REGEX = /^inliner_\d+_.+$/;

  private createNewLabelName(previousName: string): string {
    // Check if has inliner prefix
    if (!Inliner.LABEL_PREFIX_REGEX.test(previousName)) {
      const labelNumber = this.labelNumber;
      this.labelNumber += 1;
      return "inliner_" + labelNumber + "_" + previousName;
    }

    // Label has already been generated by this function, update number
    const newName = previousName.substring("inliner_".length);

    // Get number
    const underscoreIndex = newName.indexOf("_");
    const labelNumber = this.labelNumber;
    this.labelNumber += 1;
    // Generate new label
    return (
      "inliner_" + labelNumber + "_" + newName.substring(underscoreIndex + 1)
    );
  }

  private updateVarDecls(
    $newNodes: Scope,
    newVariableMap: Map<string, Vardecl | Expression>
  ): void {
    // Replace decl stmts of old vardecls with vardecls of new names (params are not included)
    for (const $jp of $newNodes.getDescendants("declStmt")) {
      const $declStmt = $jp as DeclStmt;

      const decls = $declStmt.decls;

      for (const $varDecl of decls) {
        if (!($varDecl instanceof Vardecl)) {
          continue;
        }

        const newVar = newVariableMap.get($varDecl.name);

        // If not found, just continue
        if (newVar === undefined) {
          debug(`Could not find variable ${$varDecl.name} in variable map`);
          continue;
        } else if (!(newVar instanceof Vardecl)) {
          throw new Error(
            `Expected newVar to be a Vardecl, but it is a ${newVar.joinPointType}`
          );
        }

        // Replace decl
        $declStmt.replaceWith(ClavaJoinPoints.declStmt(newVar));
      }
    }
  }

  private updateVarrefs(
    $newNodes: Scope,
    newVariableMap: Map<string, Vardecl | Expression>,
    $call: Call
  ): void {
    // Update varrefs
    for (const $jp of $newNodes.getDescendants("varref")) {
      const $varRef = $jp as Varref;

      if ($varRef.kind === "function_call") {
        continue;
      }

      const $varDecl = $varRef.decl as Vardecl;

      // If global variable, will not be in the variable map
      if ($varDecl !== undefined && $varDecl.isGlobal) {
        // Copy vardecl to work over it
        const $varDeclNoInit = $varDecl.copy() as Vardecl;

        // Remove initialization
        $varDeclNoInit.removeInit(false);

        // Change storage class to extern
        $varDeclNoInit.storageClass = StorageClass.EXTERN;

        ($call.getAncestor("function") as FunctionJp).insertBefore(
          $varDeclNoInit
        );
        continue;
      }

      // Verify if there is a mapping
      const newVar = newVariableMap.get($varDecl.name);
      if (newVar === undefined) {
        throw new Error(
          "Could not find variable " +
            $varDecl.name +
            "@" +
            $varRef.location +
            " in variable map"
        );
      }

      // If vardecl, map contains reference to old vardecl, create a varref from the new vardecl
      if (newVar instanceof Vardecl) {
        $varRef.replaceWith(ClavaJoinPoints.varRef(newVar));
      }
      // If expression, simply replace varref with the expression
      else if (newVar instanceof Expression) {
        const $adaptedVar =
          // If varref, does not need parenthesis
          newVar instanceof Varref
            ? newVar
            : // For other expressions, if parent is already a parenthesis, does not need to add a new one
              $varRef.parent instanceof ParenExpr
              ? newVar
              : // Add parenthesis
                ClavaJoinPoints.parenthesis(newVar);

        $varRef.replaceWith($adaptedVar);
      } else {
        throw new Error(
          "Not defined when newVar is of type '" +
            (newVar as Joinpoint).joinPointType +
            "'"
        );
      }
    }
  }

  private updateVarrefsInTypes(
    $newNodes: Scope,
    newVariableMap: Map<string, Vardecl | Expression>,
    $call: Call
  ): void {
    // Update varrefs inside types
    for (const $jp of $newNodes.descendants) {
      // If no type, ignore

      if (!$jp.hasType) {
        continue;
      }

      const type = $jp.type;

      const updatedType = this.updateType(type, $call, newVariableMap);

      if (updatedType !== type) {
        $jp.type = updatedType;
      }
    }
  }

  private updateType(
    type: Type,
    $call: Call,
    newVariableMap: Map<string, Vardecl | Expression>
  ): Type {
    // Since any type node can be shared, any change must be made in copies

    // If pointer type, check pointee
    if (type instanceof PointerType) {
      const original = type.pointee;
      const updated = this.updateType(original, $call, newVariableMap);

      if (original !== updated) {
        const newType = type.copy() as PointerType;
        newType.pointee = updated;
        return newType;
      }
    }

    if (type instanceof ParenType) {
      const original = type.innerType;
      const updated = this.updateType(original, $call, newVariableMap);

      if (original !== updated) {
        const newType = type.copy() as ParenType;
        newType.innerType = updated;
        return newType;
      }
    }

    if (type instanceof VariableArrayType) {
      // Has to track changes both for element type and its own array expression
      // Either was, has to update this type
      let hasChanges = false;

      // Element type
      const original = type.elementType;
      const updated = this.updateType(original, $call, newVariableMap);

      if (original !== updated) {
        hasChanges = true;
      }

      // TODO: I have no idea if this type cast is correct.
      const $sizeExprCopy = type.sizeExpr.copy() as Varref;

      // Update any children of sizeExpr
      for (const $varRef of Query.searchFrom($sizeExprCopy, Varref)) {
        const $newVarref = this.updateVarRef($varRef, $call, newVariableMap);
        if ($newVarref !== $varRef) {
          hasChanges = true;
          $varRef.replaceWith($newVarref);
        }
      }

      // Update top expr, if needed
      const $newVarref = this.updateVarRef(
        $sizeExprCopy,
        $call,
        newVariableMap
      );

      if ($newVarref !== $sizeExprCopy) {
        hasChanges = true;
      }

      if (hasChanges) {
        const newType = type.copy() as VariableArrayType;
        newType.elementType = updated;
        newType.sizeExpr = $newVarref;

        return newType;
      }
    }

    // By default, return type with no changes
    return type;
  }

  private updateVarRef(
    $varRef: Varref,
    $call: Call,
    newVariableMap: Map<string, Vardecl | Expression>
  ) {
    const $varDecl = $varRef.decl as Vardecl;

    // If global variable, will not be in the variable map
    if ($varDecl !== undefined && $varDecl.isGlobal) {
      // Copy vardecl to work over it
      const $varDeclNoInit = $varDecl.copy() as Vardecl;

      // Remove initialization
      $varDeclNoInit.removeInit(false);

      // Change storage class to extern
      $varDeclNoInit.storageClass = StorageClass.EXTERN;

      ($call.getAncestor("function") as FunctionJp).insertBefore(
        $varDeclNoInit
      );
      return $varRef;
    }

    const newVar = newVariableMap.get($varDecl.name);

    // If not found, just return
    if (newVar === undefined) {
      return $varRef;
    }

    // If vardecl, create a new varref
    if (newVar instanceof Vardecl) {
      return ClavaJoinPoints.varRef(newVar);
    }

    // If expression, return expression
    if (newVar instanceof Expression) {
      return newVar;
    }

    throw new Error(
      `Case not supported, newVar of type '${(newVar as Joinpoint).joinPointType}'`
    );
  }
}
