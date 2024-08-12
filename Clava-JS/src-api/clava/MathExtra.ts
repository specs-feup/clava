import JavaTypes from "lara-js/api/lara/util/JavaTypes.js";
import { Expression } from "../Joinpoints.js";
import ClavaJavaTypes from "./ClavaJavaTypes.js";

export default class MathExtra {
  /**
   * Attempts to simplify a mathematical expression.
   *
   * @param expression - The expression to simplify.
   * @param constants - An object that maps variable names to constants.
   *
   * @returns Simplified expression
   */
  static simplify(
    expression: Expression | string,
    constants?: Record<string, string | number>
  ): string {
    if (expression instanceof Expression) {
      expression = expression.code;
    }

    const map = new JavaTypes.HashMap();

    if (constants !== undefined) {
      for (const p in constants) {
        map.put(p, constants[p]);
      }
    }

    return ClavaJavaTypes.MathExtraApiTools.simplifyExpression(expression, map);
  }

  /**
   * Attempts to convert a mathematical expression to valid C code (e.g., converts ^ to a call to pow()).
   *
   * @param expression - The expression to simplify.
   *
   * @returns Simplified expression as C code
   */
  static convertToC(expression: Expression | string): string {
    return ClavaJavaTypes.MathExtraApiTools.convertToC(expression);
  }

  /**
   * Attempts to simplify a mathematical expression, returning a string that represents C code.
   *
   * @param expression - The expression to simplify.
   * @param constants - An object that maps variable names to constants.
   *
   * @returns Simplified expression as C code
   */
  static simplifyToC(
    expression: Expression | string,
    constants?: Record<string, string | number>
  ): string {
    const simplifiedExpr = MathExtra.simplify(expression, constants);
    return MathExtra.convertToC(simplifiedExpr);
  }
}
