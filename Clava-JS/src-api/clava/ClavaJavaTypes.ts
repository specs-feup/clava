import lara._JavaTypes;

/**
 * Static variables with class names of Java classes used in the Clava API.
 * @class
 */
var _ClavaJavaTypes = {};


_ClavaJavaTypes.getClavaNodes = function() {
	return _JavaTypes.getType("pt.up.fe.specs.clava.ClavaNodes");
}

_ClavaJavaTypes.getClavaNode = function() {
	return _JavaTypes.getType("pt.up.fe.specs.clava.ClavaNode");
}

_ClavaJavaTypes.getCxxJoinPoints = function() {
	return _JavaTypes.getType("pt.up.fe.specs.clava.weaver.CxxJoinpoints");
}

_ClavaJavaTypes.getBuiltinKind = function() {
	return _JavaTypes.getType("pt.up.fe.specs.clava.ast.type.enums.BuiltinKind");
}