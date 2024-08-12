import IdGenerator from "lara-js/api/lara/util/IdGenerator.js";
import Query from "lara-js/api/weaver/Query.js";
import {
  AdjustedType,
  ArrayType,
  BuiltinType,
  Call,
  Decl,
  DeclStmt,
  Expression,
  FileJp,
  FunctionJp,
  Joinpoint,
  Param,
  PointerType,
  ReturnStmt,
  Statement,
  Vardecl,
  Varref,
} from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";

export default class Outliner {
  private verbose: boolean = true;
  private defaultPrefix: string = "__outlined_function_";

  /**
   * Sets the verbosity of the outliner, with false being the equivalent of a silent mode (true is the default)
   * @param verbose - the verbosity of the outliner
   */
  setVerbosity(verbose: boolean): void {
    this.verbose = verbose;
  }

  /**
   * Sets the prefix used for the autogenerated names of the outlined functions
   * @param prefix - the prefix to be used
   */
  setDefaultPrefix(prefix: string): void {
    this.defaultPrefix = prefix;
  }

  /**
   * Applies function outlining to a code region delimited by two statements.
   * Function outlining is the process of removing a section of code from a function and placing it in a new function.
   * The beginning and end of the code region must be at the same scope level.
   * @param begin - the first statement of the outlining region
   * @param end - the last statement of the outlining region
   * @returns an array with the joinpoints of the outlined function and the call to it.
   * These values are merely references, and all changes have already been committed to the AST at this point
   */
  outline(begin: Statement, end: Statement): (FunctionJp | Call)[] | undefined {
    return this.outlineWithName(begin, end, this.generateFunctionName());
  }

  /**
   * Applies function outlining to a code region delimited by two statements.
   * Function outlining is the process of removing a section of code from a function and placing it in a new function.
   * The beginning and end of the code region must be at the same scope level.
   * @param begin - the first statement of the outlining region
   * @param end - the last statement of the outlining region
   * @param functionName - the name to give to the outlined function
   * @returns an array with the joinpoints of the outlined function and the call to it.
   * These values are merely references, and all changes have already been committed to the AST at this point
   */
  outlineWithName(
    begin: Statement,
    end: Statement,
    functionName: string
  ): (FunctionJp | Call)[] | undefined {
    this.printMsg(
      'Attempting to outline a region into a function named "' +
        functionName +
        '"'
    );

    //------------------------------------------------------------------------------
    if (!this.checkOutline(begin, end)) {
      this.printMsg("Provided code region is not outlinable! Aborting...");
      return;
    }
    this.printMsg("Provided code region is outlinable");

    //------------------------------------------------------------------------------
    const wrappers = this.wrapBeginAndEnd(begin, end);
    begin = wrappers[0];
    end = wrappers[1];
    this.printMsg("Wrapped outline region with begin and ending comments");

    //------------------------------------------------------------------------------
    const parentFun = this.findParentFunction(begin);
    if (parentFun == null) {
      this.printMsg("Could not find parent function! Aborting...");
      return;
    }
    const split = this.splitRegions(parentFun, begin, end);

    let region = split[1];
    const prologue = split[0];
    const epilogue = split[2];
    this.printMsg(
      "Found " + region.length + " statements for the outline region"
    );
    this.printMsg(
      "Prologue has " +
        prologue.length +
        " statements, and epilogue has " +
        epilogue.length
    );

    //------------------------------------------------------------------------------
    const globals = this.findGlobalVars();
    this.printMsg("Found " + globals.length + " global variable(s)");

    //------------------------------------------------------------------------------
    const callPlaceholder = ClavaJoinPoints.stmtLiteral(
      "//placeholder for the call to " + functionName
    );
    begin.insertBefore(callPlaceholder);
    this.printMsg("Created a placeholder call to the new function");

    //------------------------------------------------------------------------------
    const declareBefore = this.findDeclsWithDependency(region, epilogue);
    region = region.filter(
      (stmt) => !(stmt instanceof DeclStmt && declareBefore.includes(stmt))
    );
    for (let i = declareBefore.length - 1; i >= 0; i--) {
      const decl = declareBefore[i];
      decl.detach();
      begin.insertBefore(decl);
      prologue.push(decl);
    }
    this.printMsg(
      "Moved declarations from outline region to immediately before the region"
    );

    //------------------------------------------------------------------------------
    const referencedInRegion = this.findRefsInRegion(region);
    const funParams = this.createParams(referencedInRegion);
    const fun = this.createFunction(functionName, region, funParams);
    this.printMsg('Successfully created function "' + functionName + '"');

    //------------------------------------------------------------------------------
    const callArgs = this.createArgs(fun, prologue, parentFun);
    let call = this.createCall(callPlaceholder, fun, callArgs);
    this.printMsg('Successfully created call to "' + functionName + '"');

    //------------------------------------------------------------------------------
    // At this point, if the function has a premature return, it will be returning a value
    // of that type instead of void. We change the function to return void now at this point,
    // by doing a) adding a new parameter for the return value of the premature return; b) a new
    // boolean parameter that is set to true if the function returns prematurely; c) we declare these
    // two variables before the function call and d) if the boolean is set to 1 after the call, we
    // return from the caller with the value returned by the callee.
    const newCall = this.ensureVoidReturn(fun, call);
    if (newCall != null) {
      this.printMsg(
        "Ensured that the outlined function returns void by parameterizing the early return(s)"
      );
      call = newCall;
    } else {
      this.printMsg(
        "No need to ensure that the outlined function returns void, as it has no early returns"
      );
    }

    //------------------------------------------------------------------------------
    begin.detach();
    end.detach();
    this.printMsg("Outliner cleanup finished");

    return [fun, call];
  }

  /**
   * Verifies if a code region can undergo function outlining.
   * This check is performed automatically by the outliner itself, but it can be invoked manually if desired.
   * @param begin - the first statement of the outlining region
   * @param end - the last statement of the outlining region
   * @returns true if the outlining region is valid, false otherwise
   */
  checkOutline(begin: Statement, end: Statement): boolean {
    let outlinable = true;
    if (this.findParentFunction(begin) == null) {
      this.printMsg(
        "Requirement not met: outlinable region must be inside a function"
      );
      outlinable = false;
    }
    if (begin.parent.astId != end.parent.astId) {
      this.printMsg(
        "Requirement not met: begin and end joinpoints are not at the same scope level"
      );
      outlinable = false;
    }
    return outlinable;
  }

  private ensureVoidReturn(fun: FunctionJp, call: Call): Call | null {
    const returnStmts = this.findNonvoidReturnStmts([fun]);

    if (returnStmts.length == 0) {
      return null;
    }

    // actions before the function call
    const type = returnStmts[0].children[0].type;
    const resId = IdGenerator.next("__rtr_val_");
    const resVar = ClavaJoinPoints.varDeclNoInit(resId, type);

    const boolId = IdGenerator.next("__rtr_flag_");
    const boolVar = ClavaJoinPoints.varDecl(
      boolId,
      ClavaJoinPoints.integerLiteral(0)
    );

    const resVarRef = resVar.varref();
    const boolVarRef = boolVar.varref();

    call.insertBefore(resVar);
    call.insertBefore(boolVar);

    // actions in the function itself
    const params = this.createParams([resVarRef, boolVarRef]);
    fun.addParam(params[0]);
    fun.addParam(params[1]);

    for (const ret of returnStmts) {
      const resVarParam = fun.params[fun.params.length - 2];
      const derefResVarParam = ClavaJoinPoints.unaryOp(
        "*",
        resVarParam.varref()
      );
      const retVal = ret.children[0];
      retVal.detach();
      const op1 = ClavaJoinPoints.binaryOp(
        "=",
        derefResVarParam,
        retVal as any,
        resVarParam.type
      );
      ret.insertBefore(ClavaJoinPoints.exprStmt(op1));

      const boolVarParam = fun.params[fun.params.length - 1];
      const newVarref = ClavaJoinPoints.varRef(boolVarParam);
      const derefBoolVarParam = ClavaJoinPoints.unaryOp("*", newVarref);
      const trueVal = ClavaJoinPoints.integerLiteral(1);
      const op2 = ClavaJoinPoints.binaryOp(
        "=",
        derefBoolVarParam,
        trueVal,
        boolVarParam.type
      );
      ret.insertBefore(op2);
    }
    fun.setType(ClavaJoinPoints.type("void"));

    // actions on the function call
    const resVarAddr = ClavaJoinPoints.unaryOp("&", resVarRef);
    const boolVarAddr = ClavaJoinPoints.unaryOp("&", boolVarRef);
    const allArgs = call.argList.concat([resVarAddr, boolVarAddr]);
    call = this.createCall(call, fun, allArgs);

    // actions after the function call
    const returnStmt = ClavaJoinPoints.returnStmt(resVarRef);
    const scope = ClavaJoinPoints.scope();
    scope.setFirstChild(returnStmt);
    const ifStmt = ClavaJoinPoints.ifStmt(boolVarRef, scope);
    call.insertAfter(ifStmt);

    return call;
  }

  private wrapBeginAndEnd(begin: Joinpoint, end: Joinpoint): Statement[] {
    const beginWrapper = ClavaJoinPoints.stmtLiteral(
      "//begin of the outline region"
    );
    const endWrapper = ClavaJoinPoints.stmtLiteral(
      "//end of the outline region"
    );
    begin.insertBefore(beginWrapper);
    end.insertAfter(endWrapper);
    return [beginWrapper, endWrapper];
  }

  private findParentFunction(jp: Joinpoint): FunctionJp | null {
    while (!(jp instanceof FunctionJp)) {
      if (jp instanceof FileJp) {
        return null;
      }
      jp = jp.parent;
    }
    return jp;
  }

  private findGlobalVars(): Vardecl[] {
    const globals: Vardecl[] = [];
    for (const decl of Query.search(Vardecl)) {
      if (decl.isGlobal) {
        globals.push(decl);
      }
    }
    return globals;
  }

  private createCall(
    placeholder: Joinpoint,
    fun: FunctionJp,
    args: Expression[]
  ): Call {
    const call = ClavaJoinPoints.call(fun, ...args);
    placeholder.replaceWith(call);
    return call;
  }

  private createArgs(
    fun: FunctionJp,
    prologue: Statement[],
    parentFun: Joinpoint
  ): Expression[] {
    const decls: Vardecl[] = [];
    // get decls from the prologue
    for (const stmt of prologue) {
      for (const decl of Query.searchFrom(stmt, Vardecl)) {
        decls.push(decl);
      }
    }
    // get decls from the parent function params
    for (const param of Query.searchFrom(parentFun, Param)) {
      decls.push(param.definition);
    }
    // no need to handle global vars - they are not parameters

    const args = [];
    for (const param of fun.params) {
      for (const decl of decls) {
        if (decl.name === param.name) {
          const ref = ClavaJoinPoints.varRef(decl);

          if (
            param.type instanceof PointerType &&
            ref.type instanceof BuiltinType
          ) {
            const addressOfScalar = ClavaJoinPoints.unaryOp("&", ref);
            args.push(addressOfScalar);
          } else {
            args.push(ref);
          }
          break;
        }
      }
    }

    return args;
  }

  private createFunction(
    name: string,
    region: Statement[],
    params: Param[]
  ): FunctionJp {
    let oldFun: Joinpoint = region[0];
    while (!(oldFun instanceof FunctionJp)) {
      oldFun = oldFun.parent;
    }

    let retType = ClavaJoinPoints.type("void");
    const returnStmts = this.findNonvoidReturnStmts(region);
    if (returnStmts.length > 0) {
      retType = returnStmts[0].children[0].type;
      this.printMsg(
        `Found ${returnStmts.length} return statement(s) in the outline region`
      );
    }

    const fun = ClavaJoinPoints.functionDecl(name, retType, ...params);
    oldFun.insertBefore(fun);
    const scope = ClavaJoinPoints.scope();
    fun.setBody(scope);

    for (const stmt of region) {
      stmt.detach();
      scope.insertEnd(stmt);
    }

    // make sure scalar refs are now dereferenced pointers to params
    this.scalarsToPointers(region, params);
    return fun;
  }

  private findNonvoidReturnStmts(startingPoints: Joinpoint[]): ReturnStmt[] {
    const returnStmts = [];
    for (const stmt of startingPoints) {
      for (const ret of Query.searchFrom(stmt, ReturnStmt)) {
        if (ret.numChildren > 0) {
          returnStmts.push(ret);
        }
      }
    }
    return returnStmts;
  }

  private scalarsToPointers(region: Statement[], params: Param[]): void {
    for (const stmt of region) {
      for (const varref of Query.searchFrom(stmt, Varref)) {
        for (const param of params) {
          if (
            param.name === varref.name &&
            varref.type instanceof BuiltinType
          ) {
            const newVarref = ClavaJoinPoints.varRef(param);
            const op = ClavaJoinPoints.unaryOp("*", newVarref);
            varref.replaceWith(op);
          }
        }
      }
    }
  }

  private createParams(varrefs: Varref[]): Param[] {
    const params: Param[] = [];

    for (const ref of varrefs) {
      const name = ref.name;
      const varType = ref.type;

      if (
        varType instanceof ArrayType ||
        varType instanceof AdjustedType ||
        varType instanceof PointerType
      ) {
        const param = ClavaJoinPoints.param(name, varType);
        params.push(param);
      } else if (varType instanceof BuiltinType) {
        const newType = ClavaJoinPoints.pointer(varType);
        const param = ClavaJoinPoints.param(name, newType);
        params.push(param);
      } else {
        console.log("Unsuported param type: " + varType.joinPointType);
      }
    }
    this.printMsg(
      "Created " + params.length + " param(s) for the outlined function"
    );
    return params;
  }

  private findRefsInRegion(region: Statement[]): Varref[] {
    const declsNames: string[] = [];
    for (const stmt of region) {
      for (const decl of Query.searchFrom(stmt, Decl)) {
        declsNames.push((decl as any).name as string);
      }
    }

    const varrefs: Varref[] = [];
    const varrefsNames: string[] = [];
    for (const stmt of region) {
      for (const varref of Query.searchFrom(stmt, Varref)) {
        // may need to filter for other types, like macros, etc
        // select all varrefs with no matching decl in the region, except globals
        if (
          !varrefsNames.includes(varref.name) &&
          !varref.isFunctionCall &&
          !declsNames.includes(varref.name) &&
          !(varref.decl as Vardecl).isGlobal
        ) {
          varrefs.push(varref);
          varrefsNames.push(varref.name);
        }
      }
    }
    this.printMsg(
      "Found " +
        varrefsNames.length +
        " external variable references inside outline region"
    );
    return varrefs;
  }

  private findDeclsWithDependency(
    region: Statement[],
    epilogue: Statement[]
  ): DeclStmt[] {
    const regionDecls: DeclStmt[] = [];
    const regionDeclsNames: string[] = [];
    for (const stmt of region) {
      if (stmt instanceof DeclStmt) {
        regionDecls.push(stmt);
        regionDeclsNames.push((stmt.children[0] as any).name as string);
      }
    }

    const epilogueVarrefsNames: string[] = [];
    for (const stmt of epilogue) {
      // also gets function names... could it cause an issue?
      for (const varref of Query.searchFrom(stmt, Varref)) {
        epilogueVarrefsNames.push(varref.name);
      }
    }

    const declsWithDependency = [];
    for (let i = 0; i < regionDecls.length; i++) {
      const varName = regionDeclsNames[i];
      if (epilogueVarrefsNames.includes(varName)) {
        declsWithDependency.push(regionDecls[i]);
      }
    }
    this.printMsg(
      "Found " +
        declsWithDependency.length +
        " declaration(s) referenced after the outline region"
    );
    return declsWithDependency;
  }

  private splitRegions(
    fun: FunctionJp,
    begin: Statement,
    end: Statement
  ): Statement[][] {
    const prologue: Statement[] = [];
    const region: Statement[] = [];
    const epilogue: Statement[] = [];

    let inPrologue: boolean = true;
    let inRegion: boolean = false;
    for (const stmt of Query.searchFrom(fun, Statement)) {
      if (inPrologue) {
        if (stmt.astId == begin.astId) {
          region.push(stmt);
          inPrologue = false;
          inRegion = true;
        } else {
          prologue.push(stmt);
        }
      }
      if (inRegion) {
        // we only want statements at the scope level, we can get the children later
        if (stmt.parent.astId == begin.parent.astId) {
          region.push(stmt);
          if (stmt.astId == end.astId) {
            inRegion = false;
          }
        }
      }
      if (!inPrologue && !inRegion) {
        epilogue.push(stmt);
      }
    }
    return [prologue, region, epilogue];
  }

  private printMsg(msg: string) {
    if (this.verbose) console.log("[Outliner] " + msg);
  }

  private generateFunctionName() {
    return IdGenerator.next(this.defaultPrefix);
  }
}
