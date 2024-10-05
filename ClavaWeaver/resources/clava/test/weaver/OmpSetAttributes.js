laraImport("weaver.Query");

// has previous value
SetNumThreads("parallel", "42");

// no previous value
SetNumThreads("parallel for", "var");

// can't set 'num_threads' value on a 'for' directive.
SetNumThreads("for", "xxx");

console.log("-----------------------------------");

// has previous value
SetProcBind("parallel", "spread");

// no previous value
SetProcBind("parallel for", "close");

// can't set
// update: disabled wrong values, now throws exception
//call SetProcBind('for', 'xxx');

// test setters of directive parallel
SetParallelAttributes();
// test setters of directive for
SetForAttributes();

// set whole omp pragma to custom value. Custom content is not compatible with setting attributes, done after attribute set testing
SetCustomContent("parallel", "parallel private(i)");

function SetNumThreads(kind, num) {
    console.log(kind);

    for (const $omp of Query.search("omp", { kind: kind })) {
        console.log("num_threads:" + $omp.numThreads);
        $omp.setNumThreads(num);
        console.log("num_threads:" + $omp.numThreads);
    }
}

function SetProcBind(kind, proc) {
    console.log(kind);

    for (const $omp of Query.search("omp", { kind: kind })) {
        console.log("proc_bind:" + $omp.procBind);
        $omp.setProcBind(proc);
        console.log("proc_bind:" + $omp.procBind);
    }
}

function SetCustomContent(kind, content) {
    console.log(kind);

    for (const $omp of Query.search("omp", { kind: kind })) {
        console.log("content:" + $omp.content);
        $omp.content = content;
        console.log("content:" + $omp.content);
    }
}

function SetParallelAttributes() {
    for (const $omp of Query.search("omp", "parallel")) {
        console.log("parallel content before:" + $omp.content);
        const sumReduction = $omp.getReduction("+");
        sumReduction.push("b");
        $omp.setReduction("+", sumReduction);
        $omp.firstprivate = ["first_a", "first_b"];
        $omp.shared = ["shared_a", "shared_b"];
        $omp.copyin = ["copyin_a", "copyin_b"];
        console.log("parallel content after:" + $omp.content);
    }
}

function SetForAttributes() {
    for (const $omp of Query.search("omp", "for")) {
        console.log("for content before:" + $omp.content);
        $omp.lastprivate = ["last_a", "last_b"];
        $omp.scheduleKind = "static";
        $omp.scheduleChunkSize = 4;
        $omp.scheduleModifiers = ["monotonic"];
        $omp.collapse = 2;
        $omp.setOrdered();
        console.log("for content after:" + $omp.content);
    }
}
