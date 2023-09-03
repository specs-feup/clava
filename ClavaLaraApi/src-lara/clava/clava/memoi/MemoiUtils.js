/**
 * The C data types the memoization instrumentation library can handle.
 */
export var MemoiDataType;
(function (MemoiDataType) {
    MemoiDataType["INT"] = "INT";
    MemoiDataType["DOUBLE"] = "DOUBLE";
    MemoiDataType["FLOAT"] = "FLOAT";
})(MemoiDataType || (MemoiDataType = {}));
export default class MemoiUtils {
    static mathFunctionSigs = new Set([
        /* double, 1 parameter */
        "acos(double)",
        "acosh(double)",
        "asin(double)",
        "asinh(double)",
        "atan(double)",
        "atanh(double)",
        "cbrt(double)",
        "ceil(double)",
        "cos(double)",
        "cosh(double)",
        "erf(double)",
        "erfc(double)",
        "exp(double)",
        "exp2(double)",
        "expm1(double)",
        "fabs(double)",
        "floor(double)",
        "j0(double)",
        "j1(double)",
        "lgamma(double)",
        "log(double)",
        "log10(double)",
        "log1p(double)",
        "log2(double)",
        "logb(double)",
        "nearbyint(double)",
        "rint(double)",
        "round(double)",
        "sin(double)",
        "sinh(double)",
        "sqrt(double)",
        "tan(double)",
        "tanh(double)",
        "tgamma(double)",
        "trunc(double)",
        /* float, 1 parameter */
        "acosf(float)",
        "acoshf(float)",
        "asinf(float)",
        "asinhf(float)",
        "atanf(float)",
        "atanhf(float)",
        "cbrtf(float)",
        "ceilf(float)",
        "cosf(float)",
        "coshf(float)",
        "erfcf(float)",
        "erff(float)",
        "exp2f(float)",
        "expf(float)",
        "expm1f(float)",
        "fabsf(float)",
        "floorf(float)",
        "lgammaf(float)",
        "log10f(float)",
        "log1pf(float)",
        "log2f(float)",
        "logbf(float)",
        "logf(float)",
        "nearbyintf(float)",
        "rintf(float)",
        "roundf(float)",
        "sinf(float)",
        "sinhf(float)",
        "sqrtf(float)",
        "tanf(float)",
        "tanhf(float)",
        "tgammaf(float)",
        "truncf(float)",
        /* double,2 parameters */
        "atan2(double,double)",
        "copysign(double,double)",
        "fdim(double,double)",
        "fmax(double,double)",
        "fmin(double,double)",
        "fmod(double,double)",
        "hypot(double,double)",
        "nextafter(double,double)",
        "pow(double,double)",
        "remainder(double,double)",
        "scalb(double,double)",
        /* float, 2 parameters */
        "atan2f(float,float)",
        "copysignf(float,float)",
        "fdimf(float,float)",
        "fmaxf(float,float)",
        "fminf(float,float)",
        "fmodf(float,float)",
        "hypotf(float,float)",
        "nextafterf(float,float)",
        "powf(float,float)",
        "remainderf(float,float)",
    ]);
    /**
     * 		Tests whether the function with the given signature is whitelisted.
     * */
    static isWhiteListed(sig) {
        if (MemoiUtils.isMathFunction(sig)) {
            return true;
        }
        return false;
    }
    /**
     * 		Tests whether the function with the given signature is a math.h function.
     * */
    static isMathFunction(sig) {
        return MemoiUtils.mathFunctionSigs.has(sig);
    }
    static cSig(sig) {
        return sig
            .replace(/\(/g, "_")
            .replace(/\*/g, "_p")
            .replace(/ /g, "_")
            .replace(/,/g, "_")
            .replace(/\)/g, "_");
    }
    static normalizeSig(sig) {
        return sig.replace(/ /g, "");
    }
    /**
     * @deprecated Use javascript's array.includes method instead
     *
     */
    static arrayContains(a, e) {
        return a.includes(e);
    }
    static average(values, count = values.length) {
        return values.reduce((a, b) => a + b, 0) / count;
    }
}
//# sourceMappingURL=MemoiUtils.js.map