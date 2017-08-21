var Types = {};

Types.builtin = function(typeString) {
	return AstFactory.builtinType(typeString);
};

Types.float = function() {
	return Types.builtin("float");
};
