/**************************************************************
* 
*                       NormalizedBinaryOp
* 
**************************************************************/
aspectdef NormalizedBinaryOp
	
	select file.function.body.binaryOp end
	apply
			var Op = null;
			if ($binaryOp.kind === 'add')
				Op = '+';
			else if ($binaryOp.kind === 'sub')
				Op = '-';
			else if ($binaryOp.kind === 'mul')
				Op = '*';
			else if ($binaryOp.kind === 'div')
				Op = '/';

			if (Op !== null)
				$binaryOp.insert replace $binaryOp.left.code + '=' + $binaryOp.left.code  + Op + '(' +  $binaryOp.right.code + ')';
	end
	condition $binaryOp.astName === 'CompoundAssignOperator' end


end