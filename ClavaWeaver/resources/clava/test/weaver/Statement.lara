aspectdef Statement

	select loop.cond.childExpr end
	apply
	
		if($childExpr.joinPointType !== "binaryOp") {
			continue;
		}
		
		println("Condition Kind:" + $childExpr.kind);
	end

end
