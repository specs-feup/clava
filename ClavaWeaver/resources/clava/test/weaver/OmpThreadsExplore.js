aspectdef Launcher

	// Instrument code
	call OmpThreadsExplore("loop1", ["a"], 1, 3, 1);
	
	select file{"omp_threads_explore.cpp"} end
	apply
		println("File omp_threads_explore.cpp");
		println($file.code);
	end

end

aspectdef OmpThreadsExplore	
	input
		markerName, // Name of the LARA marker
		variables, // Array with name of variables to report
		threadStart=1, // Starting #threads
		threadEnd=8, // Final #threads
		threadInterval=1 // #threads increment
	end

	var ompThreadMeasure = "omp_thread_measure_" + markerName;

	var commaSeparatedVariables = "";
	for(variable of variables) {
		commaSeparatedVariables = commaSeparatedVariables + "," + variable;			
	}


	select file.marker{id == markerName} end
	apply
		var $scope = $marker.contents;
	
		// Necessary includes
		$file.addInclude("omp.h", true);
		$file.addInclude("vector", true);
		$file.addInclude("fstream", true);
		$file.addInclude("iostream", true);

		// Before the pragma
		$marker.insert before beforeMarker(ompThreadMeasure, commaSeparatedVariables,
			threadStart,threadEnd,threadInterval);
		
		// At the beginning of the scope
		$scope.exec insertBegin(OmpThreadsExploreCode.scopeEntry(ompThreadMeasure));

		// At the end of the scope
		$scope.exec insertEnd(OmpThreadsExploreCode.scopeExit(ompThreadMeasure, variables));
		
		// After the scope
		$scope.insert after afterScope(ompThreadMeasure);
	end

end

aspectdef OmpThreadsExploreCode

	static 
	
	function scopeEntry(ompThreadMeasure){return %{
std::cout << "[OpenMP_Measure] Setting number of threads to " << omp_thread_measure << std::endl;
omp_set_num_threads(omp_thread_measure);
[[ompThreadMeasure]] << omp_thread_measure;
	}%;}

	function scopeExit(ompThreadMeasure, variables){

	var streamCode = "";
	for (variable of variables) {
  		streamCode = streamCode + '<< "," << ' + variable;
	}
	
	return %{
[[ompThreadMeasure]] [[streamCode]];
[[ompThreadMeasure]] << std::endl;
	}%;}
	
	end	

end


codedef beforeMarker(ompThreadMeasure, commaSeparateVariables, threadStart, threadEnd, threadInterval) %{
	std::ofstream [[ompThreadMeasure]];
	[[ompThreadMeasure]].open("[[ompThreadMeasure]].txt", std::ofstream::out | std::fstream::trunc);
	[[ompThreadMeasure]] << "threads[[commaSeparateVariables]]" << std::endl;
	for(int omp_thread_measure = [[threadStart]]; omp_thread_measure <= [[threadEnd]]; omp_thread_measure+=[[threadInterval]])
}% end

codedef afterScope(ompThreadMeasure) %{
	[[ompThreadMeasure]].close();
	std::cout << "[OpenMP_Measure] File '[[ompThreadMeasure]].txt' written" << std::endl;
}% end



