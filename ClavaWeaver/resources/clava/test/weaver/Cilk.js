aspectdef CilkTest

	select cilkFor end
	apply
		println("CilkFor: " + $cilkFor.location);
	end

	select cilkSpawn end
	apply
		println("CilkSpawn: " + $cilkSpawn.location);
	end

	select cilkSync end
	apply
		println("CilkSync: " + $cilkSync.location);
	end

end
