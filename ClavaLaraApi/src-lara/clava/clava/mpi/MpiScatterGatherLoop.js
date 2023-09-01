import ClavaCode from "../ClavaCode.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
import MpiUtils from "./MpiUtils.js";
import MpiAccessPatterns from "./patterns/MpiAccessPatterns.js";
/**
 * Applies an MPI scatter-gather strategy to loops.
 */
export default class MpiScatterGatherLoop {
    $loop;
    inputJps = [];
    inputAccesses = [];
    outputJps = [];
    outputAccesses = [];
    constructor($loop) {
        this.$loop = $loop;
        // Check if loop can be parallelize
        if (this.$loop.iterationsExpr === undefined) {
            throw "Could not determine expression with number of iterations of the loop. Check if the loop is in the Canonical Loop Form, according to the OpenMP standard.";
        }
    }
    addInput(varName, accessPattern) {
        this.addVariable(varName, accessPattern, this.inputJps, this.inputAccesses);
    }
    addOutput(varName, accessPattern) {
        this.addVariable(varName, accessPattern, this.outputJps, this.outputAccesses);
    }
    /**
     * Adapts code to use the MPI strategy.
     */
    execute() {
        const $mainFunction = ClavaCode.getFunctionDefinition("main", true);
        const $mainFile = $mainFunction.getAncestor("file");
        if ($mainFile == undefined) {
            throw "Could not find file of main function";
        }
        // Add include
        $mainFile.addInclude("mpi.h");
        $mainFile.addInclude("iostream", true);
        // Add global variables
        const $intType = ClavaJoinPoints.builtinType("int");
        $mainFile.addGlobal(MpiUtils.VAR_NUM_TASKS, $intType, "0");
        $mainFile.addGlobal(MpiUtils.VAR_NUM_WORKERS, $intType, "0");
        const $rankDecl = $mainFile.addGlobal(MpiUtils.VAR_RANK, $intType, "0");
        // Create decl
        const mpiWorkerFunction = ClavaJoinPoints.declLiteral(this.buildMpiWorker());
        // Add MPI Worker
        $rankDecl.insertAfter(mpiWorkerFunction);
        // Replace loop with MPI Master routine
        this.replaceLoop();
        // Add MPI initialization
        this.addMpiInit($mainFunction);
    }
    /** PRIVATE SECTION **/
    static FUNCTION_MPI_WORKER = "mpi_worker";
    static VAR_WORKER_NUM_ELEMS = "mpi_loop_num_elems";
    static VAR_MASTER_TOTAL_ITER = "clava_mpi_total_iter";
    replaceLoop() {
        let masterSend = "";
        for (let i = 0; i < this.inputJps.length; i++) {
            const $inputJp = this.inputJps[i];
            const accessPattern = this.inputAccesses[i];
            masterSend += accessPattern.sendMaster($inputJp, MpiScatterGatherLoop.VAR_MASTER_TOTAL_ITER);
            masterSend += "\n";
        }
        let masterReceive = "";
        for (let i = 0; i < this.outputJps.length; i++) {
            const $inputJp = this.outputJps[i];
            const accessPattern = this.outputAccesses[i];
            masterReceive += accessPattern.receiveMaster($inputJp, MpiScatterGatherLoop.VAR_MASTER_TOTAL_ITER);
            masterReceive += "\n";
        }
        this.$loop.replaceWith(MpiScatterGatherLoop.MpiMaster(MpiUtils.VAR_NUM_WORKERS, this.$loop.iterationsExpr.code, masterSend, masterReceive, MpiUtils._VAR_MPI_STATUS));
    }
    buildMpiWorker() {
        const workerLoopCode = this.getWorkerLoopCode();
        let workerReceive = "";
        for (let i = 0; i < this.inputJps.length; i++) {
            const $inputJp = this.inputJps[i];
            const accessPattern = this.inputAccesses[i];
            workerReceive += accessPattern.receiveWorker($inputJp, MpiScatterGatherLoop.VAR_WORKER_NUM_ELEMS);
            workerReceive += "\n";
        }
        let outputDecl = "";
        for (let i = 0; i < this.outputJps.length; i++) {
            const $outputJp = this.outputJps[i];
            const accessPattern = this.outputAccesses[i];
            outputDecl += accessPattern.outputDeclWorker($outputJp, MpiScatterGatherLoop.VAR_WORKER_NUM_ELEMS);
            outputDecl += "\n";
        }
        let workerSend = "";
        for (let i = 0; i < this.outputJps.length; i++) {
            const $outputJp = this.outputJps[i];
            const accessPattern = this.outputAccesses[i];
            workerSend += accessPattern.sendWorker($outputJp, MpiScatterGatherLoop.VAR_WORKER_NUM_ELEMS);
            workerSend += "\n";
        }
        return MpiScatterGatherLoop.MpiWorker(MpiScatterGatherLoop.FUNCTION_MPI_WORKER, MpiUtils._VAR_MPI_STATUS, MpiScatterGatherLoop.VAR_WORKER_NUM_ELEMS, workerReceive, outputDecl, workerLoopCode, workerSend);
    }
    getWorkerLoopCode() {
        // Copy loop
        const $workerLoop = this.$loop.copy();
        // Adjust start and end of loop
        $workerLoop.initValue = "0";
        $workerLoop.endValue = MpiScatterGatherLoop.VAR_WORKER_NUM_ELEMS;
        // TODO: Adapt loop body, if needed
        return $workerLoop.code;
    }
    addMpiInit($mainFunction) {
        // Add params to main, if no params
        if ($mainFunction.params.length === 0) {
            $mainFunction.setParamsFromStrings(["int argc", "char** argv"]);
        }
        const numMainParams = $mainFunction.params.length;
        if (numMainParams !== 2) {
            throw `Expected main() function to have 2 paramters, has '${numMainParams}'`;
        }
        const argc = $mainFunction.params[0].name;
        const argv = $mainFunction.params[1].name;
        $mainFunction.body.insertBegin(MpiScatterGatherLoop.MpiInit(argc, argv, MpiUtils.VAR_RANK, MpiUtils.VAR_NUM_TASKS, MpiUtils.VAR_NUM_WORKERS, MpiScatterGatherLoop.FUNCTION_MPI_WORKER));
    }
    addVariable(varName, accessPattern = MpiAccessPatterns.SCALAR_PATTERN, namesArray, accessesArray) {
        // Check if loop contains a reference to the variable
        let firstVarref = undefined;
        for (const $v of this.$loop.getDescendants("varref")) {
            const $varref = $v;
            if ($varref.name === varName) {
                firstVarref = $varref;
                break;
            }
        }
        if (firstVarref === undefined) {
            throw `Could not find a reference to the variable '${varName}' in the loop located at ${this.$loop.location}`;
        }
        namesArray.push(firstVarref);
        accessesArray.push(accessPattern);
    }
    /** CODEDEFS **/
    // TODO: std::cerr should not be hardcoded, lara.code.Logger should be used instead
    static MpiInit(argc, argv, rank, numTasks, numWorkers, mpiWorker) {
        return `
    MPI_Init(&${argc}, &${argv});
    MPI_Comm_rank(MPI_COMM_WORLD, &${rank});
    MPI_Comm_size(MPI_COMM_WORLD, &${numTasks});
    ${numWorkers} = ${numTasks} - 1;

    if(${numWorkers} == 0) {
        std::cerr << "This program does not support working with a single process." << std::endl;
        return 1;
    }

	if(${rank} > 0) {
		${mpiWorker}();
		MPI_Finalize();
		return 0;
	}
`;
    }
    static MpiWorker(functionName, status, numElems, receiveData, outputDecl, loop, sendData) {
        return `
void ${functionName}() {
    MPI_Status ${status};

	// Number of loop iterations
    int ${numElems};

    MPI_Recv(&${numElems}, 1, MPI_INT, 0, 1, MPI_COMM_WORLD, &${status});
	
	${receiveData}
	
	${outputDecl}
	
	${loop}
	
	${sendData}	
}
`;
    }
    static MpiMaster(numWorkers, numIterations, masterSend, masterReceive, status) {
        return `
	// Master routine
	
	// split iterations of the loop
	int clava_mpi_total_iter = ${numIterations};
	int clava_mpi_loop_limit = clava_mpi_total_iter;
	// A better distribution calculation could be used
	int clava_mpi_num_iter = clava_mpi_total_iter / ${numWorkers};
	int clava_mpi_num_iter_last = clava_mpi_num_iter + clava_mpi_total_iter % ${numWorkers};
	// int clava_mpi_num_iter_last = clava_mpi_num_iter + (clava_mpi_loop_limit - (clava_mpi_num_iter * ${numWorkers}));
	
	// send number of iterations
	for(int i=0; i<${numWorkers}-1; i++) {
		MPI_Send(&clava_mpi_num_iter, 1, MPI_INT, i+1, 1, MPI_COMM_WORLD);
	}
	MPI_Send(&clava_mpi_num_iter_last, 1, MPI_INT, ${numWorkers}, 1, MPI_COMM_WORLD);
	
	
	${masterSend}
	
	MPI_Status ${status};
	
	${masterReceive}
	
	MPI_Finalize();
`;
    }
}
//# sourceMappingURL=MpiScatterGatherLoop.js.map