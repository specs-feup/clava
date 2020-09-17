import lara.Strings;

var MemoiUtils = {};

/**
 * 		The C data types the memoization instrumentation library can handle.
 * 
 * @enum
 * */
var MemoiDataType = new Enumeration({INT: 'INT', DOUBLE: 'DOUBLE', FLOAT: 'FLOAT'});

/**
 * 		Tests whether the function with the given signature is whitelisted.
 * */
MemoiUtils.isWhiteListed = function(sig) {
	
	if(MemoiUtils.isMathFunction(sig)) {
		return true;
	}
	
	return false;
}

/**
 * 		Tests whether the function with the given signature is a math.h function.
 * */
MemoiUtils.isMathFunction = function (sig) {

	var mathFunctionSigs =[ 
		/* double, 1 parameter */ 
		"acos(double)", "acosh(double)", "asin(double)", "asinh(double)", "atan(double)", "atanh(double)", 
		"cbrt(double)", "ceil(double)", "cos(double)", "cosh(double)", "erf(double)", "erfc(double)", "exp(double)", "exp2(double)", "expm1(double)", 
		"fabs(double)", "floor(double)", "j0(double)", "j1(double)", "lgamma(double)", "log(double)", "log10(double)", "log1p(double)", "log2(double)", 
		"logb(double)", "nearbyint(double)", "rint(double)", "round(double)", "sin(double)", "sinh(double)", "sqrt(double)", "tan(double)", "tanh(double)", "tgamma(double)", "trunc(double)", 
		/* float, 1 parameter */
		"acosf(float)", "acoshf(float)", "asinf(float)", "asinhf(float)", "atanf(float)", "atanhf(float)", 
		"cbrtf(float)", "ceilf(float)", "cosf(float)", "coshf(float)", "erfcf(float)", "erff(float)", "exp2f(float)", "expf(float)", "expm1f(float)", 
		"fabsf(float)", "floorf(float)", "lgammaf(float)", "log10f(float)", "log1pf(float)", "log2f(float)", "logbf(float)", "logf(float)", "nearbyintf(float)", 
		"rintf(float)", "roundf(float)", "sinf(float)", "sinhf(float)", "sqrtf(float)", "tanf(float)", "tanhf(float)", "tgammaf(float)", "truncf(float)", 
		/* double,2 parameters */
		"atan2(double,double)", "copysign(double,double)", "fdim(double,double)", "fmax(double,double)", "fmin(double,double)", "fmod(double,double)", "hypot(double,double)", "nextafter(double,double)", "pow(double,double)", "remainder(double,double)", "scalb(double,double)",
		/* float, 2 parameters */
		"atan2f(float,float)", "copysignf(float,float)", "fdimf(float,float)", "fmaxf(float,float)", "fminf(float,float)", "fmodf(float,float)", "hypotf(float,float)", "nextafterf(float,float)", "powf(float,float)", "remainderf(float,float)"
	];
	
	return mathFunctionSigs.indexOf(sig) !== -1;
}

MemoiUtils.cSig = function(sig) {
	
	var tmp = Strings.replacer(sig, /\(/g, '_');
	tmp = Strings.replacer(tmp, /\*/g, '_p');
	tmp = Strings.replacer(tmp, / /g, '_');
	tmp = Strings.replacer(tmp, /,/g, '_');
	return Strings.replacer(tmp, /\)/g, '_');
}

MemoiUtils.normalizeSig = function(sig) {
	
	return Strings.replacer(sig, / /g, '');
}

MemoiUtils.arrayContains = function(a, e) {
	return a.indexOf(e) != -1;
}

MemoiUtils.mean = function(values, count) {
	
	var sum = 0;
	
	for(var value of values) {
		
		sum += value;
	}
	
	if(count === undefined) {
		return sum / values.length;
	} else {
		return sum / count;
	}
}
