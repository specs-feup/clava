import JavaTypes, { JavaClasses } from "lara-js/api/lara/util/JavaTypes.js";

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace ClavaJavaClasses {

  /* eslint-disable @typescript-eslint/no-empty-interface */
  export interface ClavaNodes extends JavaClasses.JavaClass {}
  export interface ClavaNode extends JavaClasses.JavaClass {}
  export interface CxxJoinpoints extends JavaClasses.JavaClass {}
  export interface BuiltinKind extends JavaClasses.JavaClass {}
  export interface CxxWeaver extends JavaClasses.JavaClass {}
  export interface CxxWeaverApi extends JavaClasses.JavaClass {}
  export interface CxxType extends JavaClasses.JavaClass {}
  export interface Standard extends JavaClasses.JavaClass {}
  export interface AstFactory extends JavaClasses.JavaClass {}
  export interface ArgumentsParser extends JavaClasses.JavaClass {}
  export interface ClavaWeaverLauncher extends JavaClasses.JavaClass {}
  export interface MathExtraApiTools extends JavaClasses.JavaClass {}
  export interface HighLevelSynthesisAPI extends JavaClasses.JavaClass {}
  /* eslint-enable @typescript-eslint/no-empty-interface */
}

/**
 * Static variables with class names of Java classes used in the Clava API.
 *
 */
export default class ClavaJavaTypes {
  static get ClavaNodes() {
    return JavaTypes.getType("pt.up.fe.specs.clava.ClavaNodes") as ClavaJavaClasses.ClavaNodes;
  }

  static get ClavaNode() {
    return JavaTypes.getType(
      "pt.up.fe.specs.clava.ClavaNode"
    ) as ClavaJavaClasses.ClavaNode;
  }

  static get CxxJoinPoints() {
    return JavaTypes.getType(
      "pt.up.fe.specs.clava.weaver.CxxJoinpoints"
    ) as ClavaJavaClasses.CxxJoinpoints;
  }

  static get BuiltinKind() {
    return JavaTypes.getType("pt.up.fe.specs.clava.ast.type.enums.BuiltinKind") as ClavaJavaClasses.BuiltinKind;
  }

  static get CxxWeaver() {
    return JavaTypes.getType("pt.up.fe.specs.clava.weaver.CxxWeaver") as ClavaJavaClasses.CxxWeaver;
  }

  static get CxxWeaverApi() {
    return JavaTypes.getType("pt.up.fe.specs.clava.weaver.CxxWeaverApi") as ClavaJavaClasses.CxxWeaverApi;
  }

  static get CxxType() {
    return JavaTypes.getType(
      "pt.up.fe.specs.clava.weaver.joinpoints.types.CxxType"
    ) as ClavaJavaClasses.CxxType;
  }

  static get Standard() {
    return JavaTypes.getType("pt.up.fe.specs.clava.language.Standard") as ClavaJavaClasses.Standard;
  }

  static get AstFactory() {
    return JavaTypes.getType(
      "pt.up.fe.specs.clava.weaver.importable.AstFactory"
    ) as ClavaJavaClasses.AstFactory;
  }

  static get ArgumentsParser() {
    return JavaTypes.getType(
      "pt.up.fe.specs.util.parsing.arguments.ArgumentsParser"
    ) as ClavaJavaClasses.ArgumentsParser;
  }

  static get ClavaWeaverLauncher() {
    return JavaTypes.getType("pt.up.fe.specs.clava.weaver.ClavaWeaverLauncher") as ClavaJavaClasses.ClavaWeaverLauncher;
  }

  static get MathExtraApiTools() {
    return JavaTypes.getType("pt.up.fe.specs.clava.weaver.MathExtraApiTools") as ClavaJavaClasses.MathExtraApiTools;
  }

  static get HighLevelSynthesisAPI() {
    return JavaTypes.getType(
      "pt.up.fe.specs.clava.weaver.hls.HighLevelSynthesisAPI"
    ) as ClavaJavaClasses.HighLevelSynthesisAPI;
  }

}
