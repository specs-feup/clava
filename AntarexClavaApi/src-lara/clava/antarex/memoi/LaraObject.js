/**
 * 		This is a utility class that provides a few useful containers to be
 *	used with LARA aspects. The idea behind this, is to use tuples captured
 * 	with SELECTS, such as <caller, callee> or <function, var>, and use them
 *	as identifiers to store information.
*
 *	This class has the following methods:
 *		getId
 *		set
 *		get
 *		inc
 *		push
 *
 *		The methods getId() and push(), should not be used with any other
 *	methods. The methods get(), set() and inc() can be used together to
 *	store and retrieve information of a particular tuple.
 *
 *		Examples:
 *			... 
 *			... 
 *			... 
 *			... 
 */


 /**
 * 	Syntax:
 * 		var obj = new LaraObject();
 * 		
 * 
 * 	Description:
 * 
 *		This is the LaraObject constructor.
 * 
 *		getTotal() and newId() are previliged methods, they are public but
 *	they can access and modify private variables. They are not enumerable.
 */
function LaraObject(start){
    
	var init = start || 0;
    /* The counter used to assign unique IDs to tuples. */
	var uid_counter = 0;
	
	/* The previliged methods. */
	this.getTotal = function () {
	
		return uid_counter.toString();
	};
	Object.defineProperty(this, "getTotal", {enumerable: false});
	
	this.newId = function () {
		var ret = init + uid_counter;
		uid_counter++;
		return ret.toString();
	};
	Object.defineProperty(this, "newId", {enumerable: false});
};


/**
 * 	Syntax:
 * 		var id = obj.getId(tuple);
 * 		
 * 	Parameters:
 * 		tuple: any object tuple
 * 
 * 	Description:
 * 
 *		Gets an ID for the provided tuple.
 * 
 *		If the tuple was already inserted, the ID will be retrieved. If
 * 	this is the first time we insert the tuple, a new ID will be
 * 	created. This is transparent to the user.
 */
LaraObject.prototype.getId = function (){

    if(arguments.length == 0) {		
		return undefined;
	}
	
    var lastProperty = this;
	
    for(var i = 0; i< arguments.length-1; i++){
        
        var currentProperty = arguments[i];
        if(lastProperty[currentProperty] == undefined) {

            lastProperty[currentProperty] = {};
        }

        lastProperty = lastProperty[currentProperty];
    }

    var currentProperty = arguments[arguments.length-1];
    
    if(lastProperty[currentProperty] == undefined) {

        lastProperty[currentProperty] = this.newId();
    }
    
    return lastProperty[currentProperty];
};
Object.defineProperty(LaraObject.prototype, "getId", {enumerable: false});

/**
 * 	Syntax:
 * 		obj.set(tuple, value);
 * 		
 * 	Parameters:
 * 		tuple: any object tuple
 *		value: the value of the tuple
 * 
 * 	Description:
 * 
 *		Sets the value for the provided tuple.
 * 
 *		If the tuple was already inserted, it will be updated. If this is
 *	the first time we insert the tuple, it will be created. This is
 *	transparent to the user. The first N-1 arguments are the tuple, the Nth
 * argument is the element to be pushed.
 */
LaraObject.prototype.set = function (){

    if(arguments.length == 0) {
        return;
    }
    
    var lastProperty = this;
    
    for(var i = 0; i< arguments.length-2; i++){

        var currentProperty = arguments[i];
        if(lastProperty[currentProperty] == undefined) {
            lastProperty[currentProperty] = {};
        }
        lastProperty = lastProperty[currentProperty];
    }
    
    var currentProperty = arguments[arguments.length-2];

    lastProperty[currentProperty] = arguments[arguments.length-1];
};
Object.defineProperty(LaraObject.prototype, "set", {enumerable: false});


/**
 * 	Syntax:
 * 		obj.sum(tuple, value);
 * 		
 * 	Parameters:
 * 		tuple: any object tuple
 *		value: the value of the tuple
 * 
 * 	Description:
 * 
 *		Adds the value for the provided tuple to the current stored value.
 * 
 *		If the tuple was already inserted, it will be updated. If this is
 *	the first time we insert the tuple, it will be created with the given value.
 * This is ransparent to the user. The first N-1 arguments are the tuple, the
 * Nth argument is the element to be pushed.
 */
LaraObject.prototype.sum = function (){

    if(arguments.length == 0) {
        return;
    }
    
    var lastProperty = this;
    
    for(var i = 0; i< arguments.length-2; i++){

        var currentProperty = arguments[i];
        if(lastProperty[currentProperty] == undefined) {
            lastProperty[currentProperty] = {};
        }
        lastProperty = lastProperty[currentProperty];
    }
    
    var currentProperty = arguments[arguments.length-2];

    if(lastProperty[currentProperty] == undefined) {
        lastProperty[currentProperty] = 0;
    }
    lastProperty[currentProperty] += arguments[arguments.length-1];
};
Object.defineProperty(LaraObject.prototype, "sum", {enumerable: false});

/**
 * 	Syntax:
 * 		var value = obj.get(tuple);
 * 		
 * 	Parameters:
 * 		tuple: any object tuple
 * 
 * 	Description:
 * 
 *		Gets the value for the provided tuple.
 * 
 *		If the tuple is not present, an empty object will be returned.
 */
LaraObject.prototype.get = function (){

    if(arguments.length == 0) {
        return;
    }
    
    var lastProperty = this;
    
    for(var i = 0; i< arguments.length; i++){

        var currentProperty = arguments[i];
        if(lastProperty[currentProperty] == undefined) {
            lastProperty[currentProperty] = {};
        }
        lastProperty = lastProperty[currentProperty];
    }
    
    return lastProperty;
};
Object.defineProperty(LaraObject.prototype, "get", {enumerable: false});

/**
 * 	Syntax:
 * 		var value = obj.getOrElseZero(tuple);
 * 		
 * 	Parameters:
 * 		tuple: any object tuple
 * 
 * 	Description:
 * 
 *		Gets the value for the provided tuple.
 * 
 *		If the tuple is not present, returns zero.
 */
 /*
LaraObject.prototype.getOrZero = function (){
	return this.get(arguments) || 0;
	
	//var result = this.get(arguments);
	
	//if(result === undefined) {
	//	return 0;
	//}
	
	//return result;
	
}
Object.defineProperty(LaraObject.prototype, "getOrZero", {enumerable: false});
*/

/**
 * 	Syntax:
 * 		obj.inc(tuple);
 * 		
 * 	Parameters:
 * 		tuple: any object tuple
 * 
 * 	Description:
 *
 * 		Increments the value of the provided tuple.
 *
 *		If the tuple was already inserted, its value will be incremented.
 *	If this is the first time we increment the tuple, its value will be 1.
 *	This is transparent to the user.
 */
LaraObject.prototype.inc = function (){


    if(arguments.length == 0)
    return;
    var lastProperty = this;
    for(var i = 0; i< arguments.length-1; i++){
        var currentProperty = arguments[i];
        if(lastProperty[currentProperty] == undefined) {
            lastProperty[currentProperty] = {};
        }
        lastProperty = lastProperty[currentProperty];
    }
    var currentProperty = arguments[arguments.length-1];
    if(lastProperty[currentProperty] == undefined) {
        lastProperty[currentProperty] = 1;
    }else{
        lastProperty[currentProperty]++;
    }
};
Object.defineProperty(LaraObject.prototype, "inc", {enumerable: false});

/**
 * 	Syntax:
 * 		obj.push(tuple, element);
 * 		
 * 	Parameters:
 * 		tuple: any object tuple
 * 		element: the element to be pushed
 * 
 * 	Description:
 *
 *		Pushes and element to an array on the provided tuple.
 * 
 * 		The first N-1 arguments are the tuple, the Nth argument is the
 *	element to be pushed. If this is the first time the tuple is being 
 *	used, a new array will be created. This is transparent to the user.
 */
LaraObject.prototype.push = function (){

    if(arguments.length <= 1) {
        return;
    }

    var lastProperty = this;

    for(var i = 0; i< arguments.length-2; i++){
        var currentProperty = arguments[i];
        if(lastProperty[currentProperty] == undefined) {
            lastProperty[currentProperty] = {};
        }
        lastProperty = lastProperty[currentProperty];
    }

    var currentProperty = arguments[arguments.length-2];

    if(lastProperty[currentProperty] == undefined) {

        lastProperty[currentProperty] = [];
    }

 lastProperty[currentProperty].push(arguments[arguments.length-1]);
};
Object.defineProperty(LaraObject.prototype, "push", {enumerable: false});

