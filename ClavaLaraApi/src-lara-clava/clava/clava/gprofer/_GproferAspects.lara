aspectdef GproferGetCxxFunction
	input signature end
	output $func end
	
	$func = undefined;
	
	/* decode name based on gprof format */
	select function{signature == signature} end
	apply
		$func = $function;
		return;
	end
	condition
		$function.hasDefinition
	end
end

aspectdef GproferGetCFunction
	input signature end
	output $func end
	
	$func = undefined;
	
	select function{signature} end
	apply
		$func = $function;
		return;
	end
	condition
		$function.hasDefinition
	end
end
