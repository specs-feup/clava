aspectdef RemoveInclude

	//select file end
	select include end
	apply
		if($include.name === "remove_include_1.h" ) {
			$include.detach();
		}
	end

	select file end
	apply
		$file.addInclude("remove_include_2.h");
	end


	select file{"remove_include.c"} end
	apply
		println($file.code);
	end
end
