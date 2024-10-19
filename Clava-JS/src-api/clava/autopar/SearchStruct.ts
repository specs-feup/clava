/**************************************************************
* 
*                       SearchStruct
* 
**************************************************************/
function SearchStruct(structObj, criteria)
{
    return structObj.filter(function(obj)
    		{
                return Object.keys(criteria).every(function(c)
                	{

                    	if (typeof(obj[c]) === 'string')
                        	//return obj[c].indexOf(criteria[c])>-1;
                            return obj[c].toUpperCase() === criteria[c].toUpperCase();
                        
                    	else
                            
	                        return obj[c] === criteria[c];
                });
            });
}

/*

function multiFilter(array, filters) {
  filterKeys = Object.keys(filters);
  // filters all elements passing the criteria
  return array.filter(function(item) {
    // dynamically validate all filter criteria
    return filterKeys.every(function(key){ !!~filters[key].indexOf(item[key])});
  });
}

function multiFilter(arr, filters) 
{
  filterKeys = Object.keys(filters);
  return arr.filter(function(eachObj) {
    return filterKeys.every(function(eachKey) {
      if (!filters[eachKey].length) {
        return true; // passing an empty filter means that filter is ignored.
      }
      return filters[eachKey].includes(eachObj[eachKey]);
    });
  });
}
*/