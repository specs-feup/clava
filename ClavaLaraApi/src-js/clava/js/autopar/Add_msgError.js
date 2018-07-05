/**************************************************************
* 
*                       Add_msgError
* 
**************************************************************/
function Add_msgError(LoopOmpAttributes, $ForStmt, msgError )
{
    var loopindex = GetLoopIndex($ForStmt);

    if (LoopOmpAttributes[loopindex].msgError === undefined )
        LoopOmpAttributes[loopindex].msgError = [];

    if (typeof(msgError) === 'string')
    {
    	LoopOmpAttributes[loopindex].msgError.push(msgError);
    }
    else
    {
    	LoopOmpAttributes[loopindex].msgError = LoopOmpAttributes[loopindex].msgError.concat(msgError);
    }
}

