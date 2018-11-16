/**************************************************************
* 
*                       orderedVarrefs3
* 
**************************************************************/
var orderedVarrefs3 = function($jp) 
{
    var varrefs = [];
    if($jp.instanceOf("expression") || $jp.joinpointType === "statement")
    {
        return orderedVarrefsBase3($jp);
    }
    
    for(var i=0; i<$jp.astNumChildren; i++)
	{
        var astChild = $jp.astChild(i);
        if(astChild === undefined)
        {
            continue;
        }
        varrefs = varrefs.concat( orderedVarrefs3(astChild) );
    }
    return varrefs;
};
var orderedVarrefsBase3 = function($stmt)
{
    varrefTable = {};
    var maxLevel = varrefUsageOrder3($stmt, 0, varrefTable);
    var orderedExprs = [];
    for(var i=maxLevel; i>=0; i--)
    {
        var exprList = varrefTable[i];
        if(exprList === undefined)
        {
            continue;
        }            
        orderedExprs = orderedExprs.concat(exprList);
    }
    return orderedExprs; 
};
var varrefUsageOrder3 = function($jp, currentLevel, varrefTable)
{   
    if($jp.joinpointType === "binaryOp") 
    {
        if($jp.kind === "assign") 
        {
            var rightLevel = varrefUsageOrder3($jp.astChild(1), currentLevel+1, varrefTable);
            var leftLevel = varrefUsageOrder3($jp.astChild(0), currentLevel+1, varrefTable);
            return rightLevel > leftLevel ? rightLevel : leftLevel;
        }
        else
        {
            var leftLevel = varrefUsageOrder3($jp.astChild(0), currentLevel+1, varrefTable);
            var rightLevel = varrefUsageOrder3($jp.astChild(1), currentLevel+1, varrefTable);

            return rightLevel > leftLevel ? rightLevel : leftLevel;
        }

    } 
    else if(
            $jp.joinpointType === "varref" && $jp.isFunctionCall === false
            )
    {        
        
        var currentVarrefs = varrefTable[currentLevel];
        if(currentVarrefs === undefined)
        {
            currentVarrefs = [];
            varrefTable[currentLevel] = currentVarrefs;
        }
        currentVarrefs.push($jp);
        return currentLevel;
    }
    else
    {
        if (['arrayAccess','memberAccess'].indexOf($jp.joinpointType) !== -1)
        {
            var maxLevel = currentLevel;
            for(var i=0; i<$jp.astNumChildren; i++)
            {
                var lastLevel = varrefUsageOrder3($jp.astChild(i), currentLevel, varrefTable);
                if(lastLevel > maxLevel)
                {
                    maxLevel = lastLevel;
                }
            }


            var currentVarrefs = varrefTable[currentLevel];
            if(currentVarrefs === undefined)
            {
                currentVarrefs = [];
                varrefTable[currentLevel] = currentVarrefs;
            }
            currentVarrefs.push($jp);

            return maxLevel;
        }
        else
            {
            var maxLevel = currentLevel;
            for(var i=0; i<$jp.astNumChildren; i++)
            {
                var lastLevel = varrefUsageOrder3($jp.astChild(i), currentLevel+1, varrefTable);
                if(lastLevel > maxLevel)
                {
                    maxLevel = lastLevel;
                }
            }
            return maxLevel;
        }

    }
};


