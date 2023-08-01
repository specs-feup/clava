import JavaTypes from "lara-js/api/lara/util/JavaTypes.js";
/**
 * Static variables with class names of Java classes used in the Clava API.
 *
 */
export default class ClavaJavaTypes {
    static getClavaNodes() {
        return JavaTypes.getType("pt.up.fe.specs.clava.ClavaNodes");
    }
    static getClavaNode() {
        return JavaTypes.getType("pt.up.fe.specs.clava.ClavaNode");
    }
    static getCxxJoinPoints() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.CxxJoinpoints");
    }
    static getBuiltinKind() {
        return JavaTypes.getType("pt.up.fe.specs.clava.ast.type.enums.BuiltinKind");
    }
    static get CxxWeaver() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.CxxWeaver");
    }
    static get CxxWeaverApi() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.CxxWeaverApi");
    }
    static get CxxType() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.joinpoints.types.CxxType");
    }
    static get Standard() {
        return JavaTypes.getType("pt.up.fe.specs.clava.language.Standard");
    }
    static get AstFactory() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.importable.AstFactory");
    }
    static get ArgumentsParser() {
        return JavaTypes.getType("pt.up.fe.specs.util.parsing.arguments.ArgumentsParser");
    }
    static get ClavaWeaverLauncher() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.ClavaWeaverLauncher");
    }
}
//# sourceMappingURL=ClavaJavaTypes.js.map