/**************************************************************
* 
*                       RemoveStruct
* 
**************************************************************/
function RemoveStruct(structObj, criteria)
{
    return structObj.filter(function(obj)
    		{
                return ! Object.keys(criteria).every(function(c)
                	{
                        if (typeof(obj[c]) === 'string')
                    	    return obj[c].indexOf(criteria[c])>-1;
                    	else
                        	return obj[c] === criteria[c];
                	});
            });
}