aspectdef Expression

	// MemberAccess chains
	select function{"analyse"}.memberAccess end
	apply
		println("MemberChain Types:" + $memberAccess.memberChain.map(m => m.joinPointType));
		println("MemberChain Names:" + $memberAccess.memberChainNames);
	end

	// Obtain corresponding declaration for each varref
	select function{"analyse"}.varref end
	apply
		//println("VARREF LOC:" + $varref.location);
		var vardecl = $varref.vardecl;
		println("varref:" + $varref.name);	
		println("has decl:" + (vardecl != undefined));	
	end

	select newExpr end
	apply
		println("NewExpr: " + $newExpr.code);
	end
	
	select deleteExpr end
	apply
		println("DeleteExpr: " + $deleteExpr.code);
	end	
	
end