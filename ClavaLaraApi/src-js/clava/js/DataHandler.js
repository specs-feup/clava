/**
 * Build a proxy for the Clava data object.
 */
function _buildClavaProxy(obj, astNode, pragma) {
	
	// Make _node non-enumerable
	Object.defineProperty(obj, '_node',{
    	value : astNode,
    	writable: true,
		enumerable: false
	});

	// Make _pragma non-enumerable
	Object.defineProperty(obj, '_pragma',{
    	value : pragma,
    	writable: true, 
		enumerable: false
	});
	
	// If hasPragma is undefined, this means the node can never have a pragma.
	// If hasPragma is false, this means the node can have a pragma, but currently has none.	
	const acceptsPragmas = Java.type("pt.up.fe.specs.clava.ClavaNodes").acceptsPragmas(astNode);
	const hasPragma = pragma !== undefined ? true :
					acceptsPragmas ? false : undefined;



	// Make _hasPragma non-enumerable
	Object.defineProperty(obj, '_hasPragma',{
    	value : hasPragma,
		writable: true,     	
		enumerable: false
	});


	// Define clear
	Object.defineProperty(obj, '_clear',{
    	value : function(){ 
			// for enumerable properties
			for (var key in this) {
    			delete this[key];
			}
		},
		writable: true,     	
		enumerable: false
	});
	
	// Define assign
	Object.defineProperty(obj, '_assign',{
    	value : function(source){ 
			// for enumerable properties
			for (var key in source) {
    			this[key] = source[key];
			}
		},
		writable: true,     	
		enumerable: false
	});


	let proxy = new Proxy(obj, _CLAVA_DATA_HANDLER);
	
	
	return proxy;
}

const _CLAVA_DATA_PROPS = ['_node', '_pragma', '_hasPragma', "_clear", "_assign"]; 


/**
 * Handler of the Clava data object.
 */
const _CLAVA_DATA_HANDLER = {
	set: function(obj, prop, value) {

	// Check if attribute is allowed
	if (_CLAVA_DATA_PROPS.includes(prop)) {
        throw new Error('Clava data objects cannot set the property ' + prop);
    }
	
	

    // The default behavior to store the value
    obj[prop] = value;

	// If hasPragma not undefined, update pragma
	if(obj._hasPragma !== undefined) {
		// If it has not pragma but supports it, create pragma
		if(!obj._hasPragma) {
			const newDataPragma = Java.type("pt.up.fe.specs.clava.ast.pragma.ClavaData").buildClavaData(obj._node);
			//println("OBG BEFORE: " + obj._pragma);
			obj._pragma = newDataPragma;
			//println("OBG AFTER: " + obj._pragma);
			obj._hasPragma = true;	
			//println("NEW PRAGMA: " + newDataPragma);
		}
		
		//println("OB PRAGMA: " + obj._pragma);
		
		// Create json string
		let jsonString = JSON.stringify(obj);
		
		/*
		// Remove trailing {}
		jsonString = jsonString.substring(1, jsonString.length-1);
		*/
/*
		if(jsonString.startsWith("{")) {
			jsonString = jsonString.substring(1);
		}

		if(jsonString.endsWith("}")) {
			jsonString = jsonString.substring(0, jsonString.length-1);
		}		
*/	

		obj._pragma.setContent("data " + jsonString);
	}
	
	//console.log("Updating pragmas based on node " + obj._node.getDataClassName() + "\n");
	//println("Has pragma: " + obj._hasPragma);


    // Indicate success
    return true;
  }
};


let _CLAVA_DATA_CACHE = {}; 


function _getClavaData(astNode, obj, pragma) {
	const id = astNode.getId();
	let data = _CLAVA_DATA_CACHE[id];
	
	if(data === undefined) {
		obj = obj !== undefined ? obj : {};
		data = _buildClavaProxy(obj, astNode, pragma);
		_CLAVA_DATA_CACHE[id] = data;
	}
	
	return data;
}

function _hasClavaData(astNode) {
	const id = astNode.getId();
	
	return _CLAVA_DATA_CACHE.hasOwnProperty(id);
}

function _clearClavaDataCache(ids) {
	// No ids defined, clear all
	if(ids === undefined) {
		_CLAVA_DATA_CACHE = {};
		return;
	}

	for(let id of ids) {
		if(_CLAVA_DATA_CACHE.hasOwnProperty(id)) {
			delete _CLAVA_DATA_CACHE[id];		
		}
	}

}


