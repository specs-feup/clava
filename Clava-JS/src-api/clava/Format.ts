import JavaTypes from "lara-js/api/lara/util/JavaTypes.js";

export default class Format {
    static addPrefix(str: string, prefix: string): string {
        return str.split("\n").map(line => prefix + line).join("\n");
    }

    static addSuffix(str: string, suffix: string): string {
        return str.split("\n").map(line => line + suffix).join("\n");
    }

    static addPrefixAndSuffix(str: string, prefix: string, suffix: string): string {
        return Format.addSuffix(Format.addPrefix(str, prefix), suffix);
    }

    static escape(str: string): string {
        return JavaTypes.SpecsStrings.escapeJson(str);
    }
}