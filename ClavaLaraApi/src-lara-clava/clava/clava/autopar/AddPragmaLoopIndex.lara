/**************************************************************
* 
*                       AddPragmaLoopIndex
* 
**************************************************************/

aspectdef AddPragmaLoopIndex

	select file.function.loop.body end
	apply
		var loopindex = GetLoopIndex($loop);
		//$body.insert before  '//#pragma Clava loopindex ' + loopindex;
		$body.insert before  '//loopindex ' + loopindex;
	end
	condition $loop.astName === 'ForStmt' end

	
end