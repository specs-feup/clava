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
