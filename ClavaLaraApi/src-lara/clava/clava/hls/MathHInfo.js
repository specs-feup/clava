import Io from "@specs-feup/lara/api/lara/Io.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import { Call, FileJp, FunctionJp } from "../../Joinpoints.js";
import Clava from "../Clava.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
export default class MathHInfo {
    static getInfo() {
        // Save current AST
        Clava.pushAst();
        // Clear AST
        for (const $file of Query.search(FileJp)) {
            $file.detach();
        }
        // Prepare source file that will test math.h
        const mathTestCode = `
        #include <math.h>
        double foo() {return abs(-1);}		
    `;
        const $testFile = ClavaJoinPoints.file("math_h_test.c");
        $testFile.insertBegin(mathTestCode);
        // Compile example code that will allow us to get path to math.h
        Query.root().addFile($testFile);
        Clava.rebuild();
        // Seach for the abs call and obtain math.h file where it was declared
        const $absCall = Query.search(Call, "abs").first();
        const mathIncludeFile = $absCall.declaration.filepath;
        // Clear AST
        for (const $file of Query.search(FileJp)) {
            $file.detach();
        }
        // Add math.h to the AST
        const $mathFile = ClavaJoinPoints.file("math_copy.h");
        $mathFile.insertBegin(Io.readFile(mathIncludeFile));
        Query.root().addFile($mathFile);
        Clava.rebuild();
        const results = [];
        for (const $fn of Query.search(FileJp, "math_copy.h").search(FunctionJp)) {
            const paramTypes = [];
            for (const $param of $fn.params) {
                paramTypes.push($param.type.code);
            }
            const mathInfo = {
                name: $fn.name,
                returnType: $fn.type.code,
                paramTypes: paramTypes,
            };
            results.push(mathInfo);
        }
        // Restore original AST
        Clava.popAst();
        return results;
    }
    static hardcodedFallback = [
        { name: "acos", returnType: "Double", paramTypes: ["Double"] },
        { name: "asin", returnType: "Double", paramTypes: ["Double"] },
        { name: "atan", returnType: "Double", paramTypes: ["Double"] },
        { name: "atan2", returnType: "Double", paramTypes: ["Double", "Double"] },
        { name: "cos", returnType: "Double", paramTypes: ["Double"] },
        { name: "cosh", returnType: "Double", paramTypes: ["Double"] },
        { name: "sin", returnType: "Double", paramTypes: ["Double"] },
        { name: "sinh", returnType: "Double", paramTypes: ["Double"] },
        { name: "tanh", returnType: "Double", paramTypes: ["Double"] },
        { name: "exp", returnType: "Double", paramTypes: ["Double"] },
        { name: "frexp", returnType: "Double", paramTypes: ["Double", "Int"] },
        { name: "ldexp", returnType: "Double", paramTypes: ["Double", "Int"] },
        { name: "log", returnType: "Double", paramTypes: ["Double"] },
        { name: "log10", returnType: "Double", paramTypes: ["Double"] },
        { name: "modf", returnType: "Double", paramTypes: ["Double", "Double"] },
        { name: "pow", returnType: "Double", paramTypes: ["Double", "Double"] },
        { name: "sqrt", returnType: "Double", paramTypes: ["Double"] },
        { name: "ceil", returnType: "Double", paramTypes: ["Double"] },
        { name: "fabs", returnType: "Double", paramTypes: ["Double"] },
        { name: "floor", returnType: "Double", paramTypes: ["Double"] },
        { name: "fmod", returnType: "Double", paramTypes: ["Double", "Double"] },
        { name: "acosf", returnType: "Float", paramTypes: ["Float"] },
        { name: "asinf", returnType: "Float", paramTypes: ["Float"] },
        { name: "atanf", returnType: "Float", paramTypes: ["Float"] },
        { name: "atan2f", returnType: "Float", paramTypes: ["Float", "Float"] },
        { name: "cosf", returnType: "Float", paramTypes: ["Float"] },
        { name: "coshf", returnType: "Float", paramTypes: ["Float"] },
        { name: "sinf", returnType: "Float", paramTypes: ["Float"] },
        { name: "sinhf", returnType: "Float", paramTypes: ["Float"] },
        { name: "tanhf", returnType: "Float", paramTypes: ["Float"] },
        { name: "expf", returnType: "Float", paramTypes: ["Float"] },
        { name: "frexpf", returnType: "Float", paramTypes: ["Float", "Int"] },
        { name: "ldexpf", returnType: "Float", paramTypes: ["Float", "Int"] },
        { name: "logf", returnType: "Float", paramTypes: ["Float"] },
        { name: "log10f", returnType: "Float", paramTypes: ["Float"] },
        { name: "modff", returnType: "Float", paramTypes: ["Float", "Float"] },
        { name: "powf", returnType: "Float", paramTypes: ["Float", "Float"] },
        { name: "sqrtf", returnType: "Float", paramTypes: ["Float"] },
        { name: "ceilf", returnType: "Float", paramTypes: ["Float"] },
        { name: "fabsf", returnType: "Float", paramTypes: ["Float"] },
        { name: "floorf", returnType: "Float", paramTypes: ["Float"] },
        { name: "fmodf", returnType: "Float", paramTypes: ["Float", "Float"] },
    ];
}
//# sourceMappingURL=MathHInfo.js.map