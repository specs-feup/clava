import Query from "lara-js/api/weaver/Query.js";
import {
  ArrayAccess,
  BinaryOp,
  Expression,
  FunctionJp,
  Joinpoint,
  Loop,
  MemberAccess,
  Param,
  Scope,
  Statement,
  UnaryOp,
  Vardecl,
  Varref,
} from "../../Joinpoints.js";
import Logger from "../../lara/code/Logger.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";

const interfaces: Record<string, string[]> = {};
const locals: Record<string, string[]> = {};
const defCounters = ["const", "temp", "op", "mux", "ne"];
const CM = '\\"';
const NL = "\\n";
let logger: Logger;

/**
 * Source code assumptions:
 *  - Arrays have predetermined size
 *  - No pointers
 */
export default class TraceInstrumentation {
  static instrument(funName: string): void {
    //Get function root
    let root;
    const filename = funName + ".dot";
    logger = new Logger(undefined, filename);

    for (const elem of Query.search(FunctionJp).chain()) {
      if ((elem["function"] as FunctionJp).name == funName)
        root = elem["function"];
    }
    if (root == undefined) {
      console.log("Function " + funName + " not found, terminating...");
      return;
    }

    //Get scope and interface
    let scope;
    for (let i = 0; i < root.children.length; i++) {
      const child = root.children[i];
      if (child instanceof Scope) {
        scope = child;
      }
      if (child instanceof Param) {
        registerInterface(child);
      }
    }

    if (scope == undefined) {
      console.log("Function " + funName + " has no scope, terminating...");
      return;
    }

    const children = scope.children;

    //Get global vars as interfaces
    for (const elem of Query.search(Vardecl)) {
      if ((elem as Vardecl).isGlobal) registerInterface(elem as Vardecl);
    }

    //Get local vars
    for (const elem of Query.search(FunctionJp, { name: funName }).search(
      Vardecl
    )) {
      registerLocal(elem as Vardecl);
    }

    //Begin graph and create counters
    const firstOp = children[0];
    logger.text("Digraph G {\\n").log(firstOp, true);

    createSeparator(firstOp);
    createDefaultCounters(firstOp);
    for (const entry in locals) {
      declareLocalCounter(firstOp, entry);
    }
    for (const entry in interfaces) {
      declareInterfaceCounter(firstOp, entry);
    }

    //Load all interfaces
    for (const inter in interfaces) {
      initializeInterface(firstOp, inter);
    }
    createSeparator(firstOp);

    //Go through each statement and generate nodes and edges
    explore(children);

    //Close graph
    logger.text("}").log(children[scope.children.length - 1], true);
  }
}

function explore(children: Joinpoint[]): void {
  for (let i = 0; i < children.length; i++) {
    const child = children[i];

    if (child instanceof Statement) {
      if (child.children.length == 1) {
        handleStatement(child.children[0]);
      }
      if (child.children.length > 1) {
        if (child.children[0] instanceof Vardecl) {
          for (let j = 0; j < child.children.length; j++)
            handleStatement(child.children[j]);
        }
      }
    }
    if (child instanceof Loop) {
      explore(child.children[3].children.slice());
    }
  }
}

//--------------------
// Counters
//--------------------
function createDefaultCounters(node: Joinpoint): void {
  for (let i = 0; i < defCounters.length; i++)
    node.insertBefore(
      ClavaJoinPoints.stmtLiteral("int n_" + defCounters[i] + " = 0;")
    );
}

function declareInterfaceCounter(node: Joinpoint, name: string): void {
  const info = interfaces[name];
  const init = info.length == 1 ? " = 0;" : " = {0};";
  node.insertBefore(
    ClavaJoinPoints.stmtLiteral("int " + getCounterOfVar(name, info) + init)
  );
}

function declareLocalCounter(node: Joinpoint, name: string): void {
  const info = locals[name];
  const init = info.length == 1 ? " = 0;" : " = {0};";
  node.insertBefore(
    ClavaJoinPoints.stmtLiteral("int " + getCounterOfVar(name, info) + init)
  );
}

function splitMulti(str: string, tokens: string[]): string[] {
  const tempChar = tokens[0];
  for (let i = 1; i < tokens.length; i++) {
    str = str.split(tokens[i]).join(tempChar);
  }
  const strParts = str.split(tempChar);
  const newStr = [];
  for (let i = 0; i < strParts.length; i++) {
    if (strParts[i] != "") newStr.push(strParts[i]);
  }
  return newStr;
}

function filterCommonKeywords(
  value: string,
  index: number,
  arr: string[]
): boolean {
  const common = ["const", "static", "unsigned"];
  return !common.includes(value);
}

function registerInterface(param: Param): void {
  let tokens = splitMulti(param.code, [" ", "]", "["]);
  if (tokens.indexOf("=") != -1) tokens = tokens.slice(0, tokens.indexOf("="));
  tokens = tokens.filter(filterCommonKeywords);
  const interName = tokens[1];
  tokens.splice(1, 1);
  if (interfaces[interName] != undefined) return;
  interfaces[interName] = tokens;
}

function registerLocal(local: Vardecl): void {
  let tokens = splitMulti(local.code, [" ", "]", "["]);
  if (tokens.indexOf("=") != -1) tokens = tokens.slice(0, tokens.indexOf("="));
  tokens = tokens.filter(filterCommonKeywords);
  const locName = tokens[1];
  tokens.splice(1, 1);
  if (interfaces[locName] != undefined) return;
  if (locals[locName] != undefined) return;
  locals[locName] = tokens;
}

function getCounterOfVar(name: string, info: string[]): string {
  let str = "n_" + name;
  for (let i = 1; i < info.length; i++) str += "[" + info[i] + "]";
  return str;
}

function isLocal(varName: string): boolean {
  return locals[varName] != undefined;
}

function isInterface(varName: string): boolean {
  return interfaces[varName] != undefined;
}

//--------------------
// Create nodes
//--------------------
function refCounter(name: string): Varref {
  name = "n_" + name;
  return ClavaJoinPoints.varRef(name, ClavaJoinPoints.builtinType("int"));
}

function refArrayCounter(name: string, indexes: string[]): Statement {
  name = "n_" + name;
  for (let i = 0; i < indexes.length; i++) name += "[" + indexes[i] + "]";
  return ClavaJoinPoints.stmtLiteral(name);
}

function refAnyExpr(code: string): Statement {
  return ClavaJoinPoints.stmtLiteral(code);
}

function incrementCounter(
  node: Joinpoint,
  variable: string,
  indexes?: string[]
): void {
  if (indexes != undefined) {
    let access = "";
    for (let i = 0; i < indexes.length; i++) {
      access += "[" + indexes[i] + "]";
    }
    node.insertBefore(
      ClavaJoinPoints.stmtLiteral("n_" + variable + access + "++;")
    );
  } else {
    node.insertBefore(ClavaJoinPoints.stmtLiteral("n_" + variable + "++;"));
  }
}

function storeVar(node: Joinpoint, variable: string): void {
  incrementCounter(node, variable);
  const att1 = "var";
  let att2 = "";
  let att3 = "";
  if (isLocal(variable)) {
    att2 = "loc";
    att3 = locals[variable][0];
  }
  if (isInterface(variable)) {
    att2 = "inte";
    att3 = interfaces[variable][0];
  }
  logger
    .text(CM + variable + "_")
    .int(refCounter(variable))
    .text(
      CM +
        " [label=" +
        CM +
        variable +
        CM +
        ", att1=" +
        att1 +
        ", att2=" +
        att2 +
        ", att3=" +
        att3 +
        "];" +
        NL
    )
    .log(node, true);
}

function storeArray(
  node: Joinpoint,
  variable: string,
  indexes: string[]
): void {
  incrementCounter(node, variable, indexes);
  const att1 = "var";
  let att2 = "";
  let att3 = "";
  if (isLocal(variable)) {
    att2 = "loc";
    att3 = locals[variable][0];
  }
  if (isInterface(variable)) {
    att2 = "inte";
    att3 = interfaces[variable][0];
  }
  logger.text(CM + variable);
  for (let i = 0; i < indexes.length; i++) {
    logger.text("[").int(refAnyExpr(indexes[i])).text("]");
  }
  logger
    .text("_")
    .int(refArrayCounter(variable, indexes))
    .text("_l" + CM);
  logger.text(" [label=" + CM + variable);
  for (let i = 0; i < indexes.length; i++) {
    logger.text("[").int(refAnyExpr(indexes[i])).text("]");
  }
  logger
    .text(
      CM + ", att1=" + att1 + ", att2=" + att2 + ", att3=" + att3 + "];" + NL
    )
    .log(node, true);
}

function createOp(node: Joinpoint, op: string): void {
  incrementCounter(node, "op");
  logger
    .text(CM + "op")
    .int(refCounter("op"))
    .text(CM + " [label=" + CM + op + CM + ", att1=op];" + NL)
    .log(node, true);
}

function createMux(node: Joinpoint): void {
  //TODO: include OP attribute?
  incrementCounter(node, "mux");
  logger
    .text(CM + "mux")
    .int(refCounter("mux"))
    .text(CM + " [label=" + CM + "mux")
    .int(refCounter("mux"))
    .text(CM + ", att1=mux];" + NL)
    .log(node, true);
}

function createConst(node: Joinpoint, num: string): void {
  incrementCounter(node, "const");
  logger
    .text(CM + "const")
    .int(refCounter("const"))
    .text(CM + " [label=" + CM + parseInt(num) + CM + ", att1=const];" + NL)
    .log(node, true);
}

function createTemp(node: Joinpoint, type: string): void {
  incrementCounter(node, "temp");
  logger
    .text(CM + "temp")
    .int(refCounter("temp"))
    .text(CM + " [label=" + CM + "temp")
    .int(refCounter("temp"))
    .text(CM + ", att1=var, att2=loc, att3=" + type + "];" + NL)
    .log(node, true);
}

function createSeparator(node: Joinpoint): void {
  const separator = "//---------------------";
  node.insertBefore(ClavaJoinPoints.stmtLiteral(separator));
}

//--------------------
// Create edges
//--------------------
function createEdge(
  node: Joinpoint,
  source: string[],
  dest: string[],
  pos?: string,
  offset?: string
): void {
  incrementCounter(node, "ne");
  logger.text(CM);
  if (source.length == 1) {
    if (defCounters.includes(source[0])) {
      if (source[0] == "temp" && offset != undefined)
        logger.text(source[0]).int(refAnyExpr("n_temp" + offset));
      else logger.text(source[0]).int(refCounter(source[0]));
    } else logger.text(source[0]).text("_").int(refCounter(source[0]));
  } else {
    logger.text(source[0]);
    for (let i = 1; i < source.length; i++)
      logger.text("[").int(refAnyExpr(source[i])).text("]");
    logger
      .text("_")
      .int(refArrayCounter(source[0], source.slice(1)))
      .text("_l");
  }
  logger.text(CM + " -> " + CM);
  if (dest.length == 1) {
    if (defCounters.includes(dest[0])) {
      if (dest[0] == "temp" && offset != undefined)
        logger.text(dest[0]).int(refAnyExpr("n_temp" + offset));
      else logger.text(dest[0]).int(refCounter(dest[0]));
    } else logger.text(dest[0]).text("_").int(refCounter(dest[0]));
  } else {
    logger.text(dest[0]);
    for (let i = 1; i < dest.length; i++)
      logger.text("[").int(refAnyExpr(dest[i])).text("]");
    logger
      .text("_")
      .int(refArrayCounter(dest[0], dest.slice(1)))
      .text("_l");
  }
  logger
    .text(CM + " [label=" + CM)
    .int(refCounter("ne"))
    .text(CM + ", ord=" + CM)
    .int(refCounter("ne"));
  if (pos != undefined) logger.text(CM + ", pos=" + CM + pos);
  logger.text(CM + "];" + NL).log(node, true);
}

//--------------------
// Handle statements
//--------------------
function handleStatement(node: Joinpoint): void {
  createSeparator(node);
  if (node instanceof Vardecl) handleVardecl(node);
  if (node instanceof BinaryOp) handleAssign(node);
  if (node instanceof UnaryOp) handleUnaryOp(node);
  createSeparator(node);
}

function handleVardecl(node: Vardecl): void {
  //Not contemplating arrays, TODO if necessary
  if (node.children.length != 1) return;
  const info = getInfo(node.children[0]);
  storeVar(node, node.name);
  createEdge(node, info, [node.name]);
}

function handleAssign(node: BinaryOp): void {
  //Build rhs node(s)
  let rhsInfo = getInfo(node.right);

  //Build lhs node
  const lhsInfo = getInfo(node.left);

  //If assignment, load extra node and build op
  if (node.kind.includes("_")) {
    const opKind = mapOperation(node.kind);
    createOp(node, opKind);
    if (rhsInfo[0] == "temp" && lhsInfo[0] == "temp")
      createEdge(node, rhsInfo, ["op"], "r", "-1");
    else createEdge(node, rhsInfo, ["op"], "r");
    createEdge(node, lhsInfo, ["op"], "l");
    rhsInfo = ["op"];
  }

  if (node.left instanceof ArrayAccess) {
    storeArray(node, lhsInfo[0], lhsInfo.slice(1));
  }
  if (node.left instanceof Varref) {
    storeVar(node, lhsInfo[0]);
  }

  //Create assignment edge
  createEdge(node, rhsInfo, lhsInfo);
}

function handleUnaryOp(node: UnaryOp): string[] | void {
  if (node.kind.includes("pre") || node.kind.includes("post")) {
    createOp(node, mapOperation(node.kind));
    const info = handleVarref(node.children[0] as Varref);
    createConst(node, "1");
    createEdge(node, ["const"], ["op"], "r");
    createEdge(node, info, ["op"], "l");
    storeVar(node, info[0]);
    createEdge(node, ["op"], info);
  } else return ["const"];
}

function handleVarref(node: Varref): string[] {
  return [node.name];
}

function handleArrayAccess(node: ArrayAccess): string[] {
  const info = [(node.arrayVar as Varref | MemberAccess).name];
  for (let i = 0; i < node.subscript.length; i++)
    info.push(node.subscript[i].code);
  return info;
}

function handleBinaryOp(node: BinaryOp): string[] {
  //Build rhs
  const rhsInfo = getInfo(node.right);

  //Build lhs
  const lhsInfo = getInfo(node.left);

  //Build op
  const opKind = mapOperation(node.kind);
  createOp(node, opKind);
  if (rhsInfo[0] == "temp" && lhsInfo[0] == "temp")
    createEdge(node, rhsInfo, ["op"], "r", "-1");
  else createEdge(node, rhsInfo, ["op"], "r");
  createEdge(node, lhsInfo, ["op"], "l");

  //Build temp
  createTemp(node, "float");
  createEdge(node, ["op"], ["temp"]);
  return ["temp"];
}

function getInfo(node: Joinpoint): string[] {
  let info = ["null"];
  if (node instanceof Varref) info = handleVarref(node);
  if (node instanceof BinaryOp) info = handleBinaryOp(node);
  if (node instanceof ArrayAccess) info = handleArrayAccess(node);
  if (node instanceof Expression) info = handleExpression(node);
  return info;
}

function handleExpression(node: Joinpoint): string[] {
  if (node.children.length == 3 && node.children[0] instanceof Expression) {
    //Build comparison
    const cmpInfo = getInfo(node.children[0].children[0]);

    //Build true value
    const trueInfo = getInfo(node.children[1]);

    //Build false value
    const falseInfo = getInfo(node.children[2]);

    //Build multiplexer
    createMux(node);
    createEdge(node, cmpInfo, ["mux"], "sel");
    createEdge(node, trueInfo, ["mux"], "t");
    createEdge(node, falseInfo, ["mux"], "f");
    return ["mux"];
  }
  if (node.children.length == 1) return getInfo(node.children[0]);
  if (node.children.length == 0) {
    createConst(node, node.code);
    return ["const"];
  }
  return ["null"];
}

//--------------------
// Utils
//--------------------
function mapOperation(op: string): string {
  switch (op) {
    case "mul":
      return "*";
    case "div":
      return "/";
    case "rem":
      return "%";
    case "add":
      return "+";
    case "sub":
      return "-";
    case "shl":
      return "<<";
    case "shr":
      return ">>";
    case "cmp":
      return "cmp";
    case "lt":
      return "<";
    case "gt":
      return ">";
    case "le":
      return "<=";
    case "ge":
      return ">=";
    case "eq":
      return "==";
    case "ne":
      return "!=";
    case "and":
      return "&";
    case "xor":
      return "^";
    case "or":
      return "|";
    case "l_and":
      return "&&";
    case "l_or":
      return "||";
    case "assign":
      return "=";
    case "mul_assign":
      return "*";
    case "rem_assign":
      return "%";
    case "add_assign":
      return "+";
    case "sub_assign":
      return "-";
    case "shl_assign":
      return "<<";
    case "shr_assign":
      return ">>";
    case "and_assign":
      return "&&";
    case "xor_assign":
      return "^";
    case "or_assign":
      return "||";
    case "post_int":
      return "+";
    case "post_dec":
      return "-";
    case "pre_inc":
      return "+";
    case "pre_dec":
      return "-";
  }
  return op;
}

function initializeInterface(node: Joinpoint, variable: string): void {
  let level = 0;
  let stmt = "";
  const info = [];
  const indexes = ["_i", "_j", "_k", "_l", "_m", "_n"];
  for (let i = 1; i < interfaces[variable].length; i++) {
    stmt +=
      "for (int " +
      indexes[level] +
      " = 0; " +
      indexes[level] +
      " < " +
      interfaces[variable][i] +
      "; " +
      indexes[level] +
      "++) {\n";
    info.push(indexes[level]);
    level++;
  }
  node.insertBefore(ClavaJoinPoints.stmtLiteral(stmt));

  if (level != 0) storeArray(node, variable, info);
  else storeVar(node, variable);

  stmt = "";
  for (let i = 0; i < level; i++) stmt += "}\n";
  node.insertBefore(ClavaJoinPoints.stmtLiteral(stmt));
}
