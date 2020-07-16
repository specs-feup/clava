/**************************************************************
* 
*                       GetLoopIndex
* 
**************************************************************/
var GetLoopIndex = function($loop)
{
	return $loop.astAncestor('FunctionDecl').name + '_' + $loop.rank.join('_');
	//return $loop.id;
}