/**************************************************************
* 
*                       GetLoopIndex
* 
**************************************************************/
var GetLoopIndex = function($loop)
{
	return $loop.getAstAncestor('FunctionDecl').name + '_' + $loop.rank.join('_');
	//return $loop.id;
}