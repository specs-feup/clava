import clava.ClavaJoinPoints;

aspectdef AddGlobal

	select file end
	apply
		var type = ClavaJoinPoints.builtinType("int");
		$file.exec addGlobal('num_threads', type, "16");
		println($file.code);
		println("---------------");
	end
end
