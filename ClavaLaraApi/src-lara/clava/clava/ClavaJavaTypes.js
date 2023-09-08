import JavaTypes from "lara-js/api/lara/util/JavaTypes.js";
/**
 * Static variables with class names of Java classes used in the Clava API.
 *
 */
export default class ClavaJavaTypes {
    static get ClavaNodes() {
        return JavaTypes.getType("pt.up.fe.specs.clava.ClavaNodes");
    }
    static get ClavaNode() {
        return JavaTypes.getType("pt.up.fe.specs.clava.ClavaNode");
    }
    static get CxxJoinPoints() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.CxxJoinpoints");
    }
    static get BuiltinKind() {
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
    static get MathExtraApiTools() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.MathExtraApiTools");
    }
    static get HighLevelSynthesisAPI() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.hls.HighLevelSynthesisAPI");
    }
    static get MemoiReport() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.memoi.MemoiReport");
    }
    static get MemoiReportsMap() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.memoi.MemoiReportsMap");
    }
    static get MemoiCodeGen() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.memoi.MemoiCodeGen");
    }
    static get ClavaPetit() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.util.ClavaPetit");
    }
    static get ClavaPlatforms() {
        return JavaTypes.getType("pt.up.fe.specs.clava.weaver.importable.ClavaPlatforms");
    }
}
//# sourceMappingURL=ClavaJavaTypes.js.map