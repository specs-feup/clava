import JavaTypes from "lara-js/api/lara/util/JavaTypes.js";
export default class Format {
    static addPrefix(str, prefix) {
        return str.split("\n").map(line => prefix + line).join("\n");
    }
    static addSuffix(str, suffix) {
        return str.split("\n").map(line => line + suffix).join("\n");
    }
    static addPrefixAndSuffix(str, prefix, suffix) {
        return Format.addSuffix(Format.addPrefix(str, prefix), suffix);
    }
    static escape(str) {
        return JavaTypes.SpecsStrings.escapeJson(str);
    }
}
//# sourceMappingURL=Format.js.map