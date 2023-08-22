/**************************************************************
* 
*                       GetLoopIndex
* 
**************************************************************/
var GetLoopIndex = function($loop)
{
	return $loop.getAncestor('function').name + '_' + $loop.rank.join('_');
	//return $loop.id;
}