aspectdef FileRebuild



	// Add literal stmt
	select file{"file_rebuild.h"} end
	apply
		$file.insert after "void foo2();";
	end
	
	println("file_rebuild.h before:");
	select file{"file_rebuild.h"}.function end
	apply
		println("Function: " + $function.signature);
	end

	// Rebuild file
	select file{"file_rebuild.h"} end
	apply		
		$file.rebuild();
	end
	
	println("file_rebuild.h after:");
	select file{"file_rebuild.h"}.function end
	apply
		println("Function: " + $function.signature);
	end

end
