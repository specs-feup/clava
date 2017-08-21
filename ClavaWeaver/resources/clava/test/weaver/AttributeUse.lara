aspectdef AttributeUse

	println("VARREF:");
	select stmt.varref end
	apply
		println($varref.code + "->" + $varref.use);
	end

	println("ARRAY ACCESS:");
	select stmt.arrayAccess end
	apply
		println($arrayAccess.code + "->" + $arrayAccess.use);
	end
end
