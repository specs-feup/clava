import Query from "lara-js/api/weaver/Query.js";
import MemoiUtils from "./MemoiUtils.js";
export default class MemoiTarget {
    sig;
    $func;
    isUser;
    numInputs;
    numOutputs;
    inputTypes;
    outputTypes;
    numCallSites;
    constructor(sig, $func, isUser, numInputs = $func.params.length, numOuputs = 1, inputTypes, outputTypes, numCallSites) {
        this.sig = MemoiUtils.normalizeSig(sig);
        this.$func = $func;
        this.isUser = isUser;
        this.numInputs = numInputs;
        this.numOutputs = numOuputs;
        if (inputTypes === undefined || outputTypes === undefined) {
            [this.inputTypes, this.outputTypes] = this.findDataTypes();
        }
        else {
            this.inputTypes = inputTypes;
            this.outputTypes = outputTypes;
        }
        this.numCallSites = numCallSites ?? this.findNumCallSites();
        this.checkDataTypes();
    }
    static fromFunction($func) {
        const sig = MemoiUtils.normalizeSig($func.signature);
        const isUser = !MemoiUtils.isWhiteListed(sig);
        const numInputs = $func.params.length;
        const numOutputs = 1;
        // input types, output types, and num of call sites are found in the constructor
        return new MemoiTarget(sig, $func, isUser, numInputs, numOutputs, undefined, undefined);
    }
    static fromCall($call) {
        const $func = $call.function;
        if ($func === undefined) {
            throw `Could not find function of call '${$call.code}'`;
        }
        return MemoiTarget.fromFunction($func);
    }
    static fromSig(sig) {
        sig = MemoiUtils.normalizeSig(sig);
        const $func = Query.search("function", {
            signature: (signature) => sig === MemoiUtils.normalizeSig(signature),
        }).first();
        if ($func === undefined) {
            const $call = Query.search("call", {
                signature: (signature) => sig === MemoiUtils.normalizeSig(signature),
            }).first();
            if ($call === undefined) {
                throw `Could not find function of sig '${sig}'`;
            }
            return MemoiTarget.fromCall($call);
        }
        return MemoiTarget.fromFunction($func);
    }
    findNumCallSites() {
        return Query.search("call", {
            signature: (signature) => this.sig === MemoiUtils.normalizeSig(signature),
        }).get().length;
    }
    findDataTypes() {
        const inputTypes = [];
        const outputTypes = [];
        const $functionType = this.$func.functionType;
        if (this.numOutputs == 1) {
            outputTypes.push($functionType.returnType.code);
            $functionType.paramTypes.forEach(function (e) {
                inputTypes.push(e.code);
            });
        }
        else {
            const typeCodes = $functionType.paramTypes.map(function (e) {
                return e.code;
            });
            typeCodes.forEach((e, i) => {
                if (i < this.numInputs) {
                    inputTypes.push(e);
                }
                else {
                    outputTypes.push(e);
                }
            });
        }
        return [inputTypes, outputTypes];
    }
    checkDataTypes() {
        const normalTypes = ["double", "float", "int"];
        const pointerTypes = ["double *", "float *", "int *"];
        const inputsInvalid = this.inputTypes.some(function (e) {
            return !normalTypes.includes(e);
        });
        if (inputsInvalid) {
            throw (`The inputs of the target function '${this.sig}' are not supported.`);
        }
        const outputTestArray = this.numOutputs == 1 ? normalTypes : pointerTypes;
        const outputsInvalid = this.outputTypes.some(function (e) {
            return !outputTestArray.includes(e);
        });
        if (outputsInvalid) {
            throw (`The outputs of the target function '${this.sig}' are not supported.`);
        }
        // if the output are valid, drop the pointer from the type
        this.outputTypes = this.outputTypes.map(function (e) {
            return e.replace(/\*/, "").trim();
        });
    }
}
//# sourceMappingURL=MemoiTarget.js.map