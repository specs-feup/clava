laraImport("weaver.Query");

// Instrument code
OmpThreadsExplore("loop1", ["a"], 1, 3, 1);

const $file = Query.search("file", "omp_threads_explore.cpp").first();
if ($file) {
    console.log("File omp_threads_explore.cpp");
    console.log($file.code);
}

function OmpThreadsExplore(
    markerName, // Name of the LARA marker
    variables, // Array with name of variables to report
    threadStart = 1, // Starting #threads
    threadEnd = 8, // Final #threads
    threadInterval = 1 // #threads increment
) {
    const ompThreadMeasure = "omp_thread_measure_" + markerName;

    let commaSeparatedVariables = "";
    for (const variable of variables) {
        commaSeparatedVariables = commaSeparatedVariables + "," + variable;
    }

    for (const chain of Query.search("file")
        .search("marker", {
            id: markerName,
        })
        .chain()) {
        const $file = chain["file"];
        const $marker = chain["marker"];
        const $scope = $marker.contents;

        // Necessary includes
        $file.addInclude("omp.h", true);
        $file.addInclude("vector", true);
        $file.addInclude("fstream", true);
        $file.addInclude("iostream", true);

        // Before the pragma
        $marker.insertBefore(
            beforeMarker(
                ompThreadMeasure,
                commaSeparatedVariables,
                threadStart,
                threadEnd,
                threadInterval
            )
        );

        // At the beginning of the scope
        $scope.insertBegin(scopeEntry(ompThreadMeasure));

        // At the end of the scope
        $scope.insertEnd(scopeExit(ompThreadMeasure, variables));

        // After the scope
        $scope.insertAfter(afterScope(ompThreadMeasure));
    }
}

function scopeEntry(ompThreadMeasure) {
    return `
std::cout << "[OpenMP_Measure] Setting number of threads to " << omp_thread_measure << std::endl;
omp_set_num_threads(omp_thread_measure);
${ompThreadMeasure} << omp_thread_measure;
    `;
}

function scopeExit(ompThreadMeasure, variables) {
    let streamCode = "";
    for (variable of variables) {
        streamCode = streamCode + '<< "," << ' + variable;
    }

    return `
${ompThreadMeasure} ${streamCode};
${ompThreadMeasure} << std::endl;
    `;
}

function beforeMarker(
    ompThreadMeasure,
    commaSeparateVariables,
    threadStart,
    threadEnd,
    threadInterval
) {
    return `
    std::ofstream ${ompThreadMeasure};
    ${ompThreadMeasure}.open("${ompThreadMeasure}.txt", std::ofstream::out | std::fstream::trunc);
    ${ompThreadMeasure} << "threads${commaSeparateVariables}" << std::endl;
    for(int omp_thread_measure = ${threadStart}; omp_thread_measure <= ${threadEnd}; omp_thread_measure+=${threadInterval})
`;
}

function afterScope(ompThreadMeasure) {
    return `
    ${ompThreadMeasure}.close();
    std::cout << "[OpenMP_Measure] File '${ompThreadMeasure}.txt' written" << std::endl;
`;
}
