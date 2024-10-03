aspectdef ParentRegion

	select vardecl end
	apply
		var $currentRegion = $vardecl.currentRegion;
		var $parentRegion = $vardecl.parentRegion;
		
		printMessage($vardecl, $currentRegion, $parentRegion);
	end

	select varref end
	apply
		var $currentRegion = $varref.currentRegion;
		var $parentRegion = $varref.parentRegion;
		
		printMessage($varref, $currentRegion, $parentRegion);
	end
	

	select function end
	apply
		var $currentRegion = $function.currentRegion;
		var $parentRegion = $function.parentRegion;
		
		printMessage($function, $currentRegion, $parentRegion);
	end
	
end

var printMessage = function($jp, $currentRegion, $parentRegion) {
	var initMessage = $jp.joinPointType + " '"+$jp.name+"' is in region '" + $currentRegion.joinPointType + "' at line " + $jp.line;
	
	if($parentRegion === undefined) {
		println(initMessage +" and does not have a parentRegion");
		return;
	}
	
	println(initMessage +", parentRegion is a '" + $parentRegion.joinPointType + "' at line " + $parentRegion.line);
};