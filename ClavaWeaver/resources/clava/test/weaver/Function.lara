aspectdef FunctionTest
	
	select function end
	apply
		var fName = $function.name;
		if(fName != "Max" && fName != "MaxNoInline") {
			continue;
		}
		
		println($function.name + " is inline: " + $function.isInline);
	end

	// Set name
	select file{"function.cpp"}.function end
	apply
	
		var fName = $function.name;
		if(fName != "defOnly" && fName != "declOnly" && fName != "declAndDef" && fName != "notCalled") {
			continue;
		}
		
		//println("FILE BEFORE SET:\n" + $file.code);
		var newName = fName + "New";
		$function.name = newName;	
		//println("FILE AFTER SET:\n" + $file.code);

		println("Function new name: " + $function.name);
	end
	
	select function{"caller"}.body end
	apply
		println("Caller body: " + $body.code);
	end
	

end
