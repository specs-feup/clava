import clava.mpi.MpiUtils;
import clava.mpi.patterns.MpiAccessPatterns;

import clava.Clava;
import clava.ClavaJoinPoints;
import clava.ClavaCode;

/**
 * Applies an MPI scatter-gather strategy to loops.
 * @class
 */
function MpiScatterGatherLoop($loop) {
	this._$loop = $loop;
	this._inputJps = [];
	this._inputAccesses = [];
	this._outputJps = [];	
	this._outputAccesses = [];
	
	// Check if loop can be parallelize
	if(this._$loop.iterationsExpr === undefined) {
		throw "Could not determine expression with number of iterations of the loop. Check if the loop is in the Canonical Loop Form, according to the OpenMP standard.";
	}
	// Inputs + access pattern
	// Outputs + access pattern
}

MpiScatterGatherLoop.prototype.addInput = function(varName, accessPattern) {
	this._addVariable(varName, accessPattern, this._inputJps, this._inputAccesses);
}


MpiScatterGatherLoop.prototype.addOutput = function(varName, accessPattern) {
	this._addVariable(varName, accessPattern, this._outputJps, this._outputAccesses);
}

/**
 * Adapts code to use the MPI strategy.
 */ 
MpiScatterGatherLoop.prototype.execute = function() {

	var $mainFunction = ClavaCode.getFunctionDefinition("main", true);
	var $mainFile = $mainFunction.ancestor("file");
	
	// Add include
	$mainFile.addInclude("mpi.h");
	$mainFile.addInclude("iostream", true);
	
	// Add global variables
	$intType = ClavaJoinPoints.builtinType("int");
	$mainFile.addGlobal(MpiUtils.VAR_NUM_TASKS, $intType, "0");
	$mainFile.addGlobal(MpiUtils.VAR_NUM_WORKERS, $intType, "0");
	var $rankDecl = $mainFile.addGlobal(MpiUtils.VAR_RANK, $intType, "0");

	// Create decl
	var mpiWorkerFunction = ClavaJoinPoints.declLiteral(this._buildMpiWorker());
	
	// Add MPI Worker
	$rankDecl.insertAfter(mpiWorkerFunction);	

	// Replace loop with MPI Master routine
	this._replaceLoop();
	
	// Add MPI initialization
	this._addMpiInit($mainFunction);
}


/** PRIVATE SECTION **/

MpiScatterGatherLoop._FUNCTION_MPI_WORKER = "mpi_worker";
MpiScatterGatherLoop._VAR_WORKER_NUM_ELEMS = "mpi_loop_num_elems";
MpiScatterGatherLoop._VAR_MASTER_TOTAL_ITER = "clava_mpi_total_iter";

MpiScatterGatherLoop.prototype._replaceLoop = function() {
//	println("Cond relation:" + this._$loop.condRelation);
//	println("Iterations expr:" + this._$loop.iterationsExpr.code);

	var masterSend = "";
	
	for(var i=0; i<this._inputJps.length; i++) {
		var $inputJp = this._inputJps[i];
		var accessPattern = this._inputAccesses[i];
		
		masterSend += accessPattern.sendMaster($inputJp, MpiScatterGatherLoop._VAR_MASTER_TOTAL_ITER);
		masterSend += "\n";
	}
	
	var masterReceive = "";
	
	for(var i=0; i<this._outputJps.length; i++) {
		var $inputJp = this._outputJps[i];
		var accessPattern = this._outputAccesses[i];
		
		masterReceive += accessPattern.receiveMaster($inputJp, MpiScatterGatherLoop._VAR_MASTER_TOTAL_ITER);
		masterReceive += "\n";
	}


	this._$loop.replaceWith(MpiMaster(MpiUtils.VAR_NUM_WORKERS, this._$loop.iterationsExpr.code, masterSend, masterReceive, MpiUtils._VAR_MPI_STATUS));
}

/*
MpiScatterGatherLoop.prototype._buildMpiMaster = function() {

	return MpiMaster(MpiUtils.VAR_NUM_WORKERS, "", "");
}
*/

MpiScatterGatherLoop.prototype._buildMpiWorker = function() {

	var workerLoopCode = this._getWorkerLoopCode();

	var workerReceive = "";
	
	for(var i=0; i<this._inputJps.length; i++) {
		var $inputJp = this._inputJps[i];
		var accessPattern = this._inputAccesses[i];
		
		workerReceive += accessPattern.receiveWorker($inputJp, MpiScatterGatherLoop._VAR_WORKER_NUM_ELEMS);
		workerReceive += "\n";
	}

	var outputDecl = "";
	for(var i=0; i<this._outputJps.length; i++) {
		var $outputJp = this._outputJps[i];
		var accessPattern = this._outputAccesses[i];
		
		outputDecl += accessPattern.outputDeclWorker($outputJp, MpiScatterGatherLoop._VAR_WORKER_NUM_ELEMS);
		outputDecl += "\n";
	}

	var workerSend = "";
	
	for(var i=0; i<this._outputJps.length; i++) {
		var $outputJp = this._outputJps[i];
		var accessPattern = this._outputAccesses[i];
		
		workerSend += accessPattern.sendWorker($outputJp, MpiScatterGatherLoop._VAR_WORKER_NUM_ELEMS);
		workerSend += "\n";
	}
	
	return MpiWorker(MpiScatterGatherLoop._FUNCTION_MPI_WORKER, MpiUtils._VAR_MPI_STATUS, MpiScatterGatherLoop._VAR_WORKER_NUM_ELEMS, workerReceive, outputDecl, workerLoopCode, workerSend);
}



MpiScatterGatherLoop.prototype._getWorkerLoopCode = function() {

	// Copy loop
	var $workerLoop = this._$loop.copy();

	// Adjust start and end of loop
	$workerLoop.initValue = "0";
	$workerLoop.endValue = MpiScatterGatherLoop._VAR_WORKER_NUM_ELEMS;
	
	// TODO: Adapt loop body, if needed

	return $workerLoop.code;
}

MpiScatterGatherLoop.prototype._addMpiInit = function($mainFunction) {

	// Add params to main, if no params
	if($mainFunction.params.length === 0) {
		$mainFunction.paramsFromStrings = ["int argc", "char** argv"];
 	}

	var numMainParams = $mainFunction.params.length;
	checkTrue(numMainParams === 2, "Expected main() function to have 2 paramters, has '" + numMainParams +"'");

	var argc = $mainFunction.params[0].name;
	var argv = $mainFunction.params[1].name;

	$mainFunction.body.insertBegin(MpiInit(argc, argv, MpiUtils.VAR_RANK, MpiUtils.VAR_NUM_TASKS, MpiUtils.VAR_NUM_WORKERS, MpiScatterGatherLoop._FUNCTION_MPI_WORKER));

}

MpiScatterGatherLoop.prototype._addVariable = function(varName, accessPattern, namesArray, accessesArray) {

	// Check if loop contains a reference to the variable
	var firstVarref = undefined;
	for(var $varref of this._$loop.descendants('varref')) {
		if($varref.name === varName) {
			firstVarref = $varref;
			break;
		}
	}
	
	if(firstVarref === undefined) {
		throw "Could not find a reference to the variable '"+varName+"' in the loop located at " + this._$loop.location;
	}

	//println("Varref type: " + $varref.type.code);
	
	if(accessPattern === undefined) {
		accessPattern = MpiAccessPatterns.SCALAR_PATTERN;
		// TODO: Verify varref type is a scalar
	}
	
	namesArray.push(firstVarref);
	accessesArray.push(accessPattern);
}


/** CODEDEFS **/

// TODO: std::cerr should not be hardcoded, lara.code.Logger should be used instead
codedef MpiInit(argc, argv, rank, numTasks, numWorkers, mpiWorker) %{
    MPI_Init(&[[argc]], &[[argv]]);
    MPI_Comm_rank(MPI_COMM_WORLD, &[[rank]]);
    MPI_Comm_size(MPI_COMM_WORLD, &[[numTasks]]);
    [[numWorkers]] = [[numTasks]] - 1;

    if([[numWorkers]] == 0) {
        std::cerr << "This program does not support working with a single process." << std::endl;
        return 1;
    }

	if([[rank]] > 0) {
		[[mpiWorker]]();
		MPI_Finalize();
		return 0;
	}
}% end

codedef MpiWorker(functionName, status, numElems, receiveData, outputDecl, loop, sendData) %{
void [[functionName]]() {
    MPI_Status [[status]];

	// Number of loop iterations
    int [[numElems]];

    MPI_Recv(&[[numElems]], 1, MPI_INT, 0, 1, MPI_COMM_WORLD, &[[status]]);
	
	[[receiveData]]
	
	[[outputDecl]]
	
	[[loop]]
	
	[[sendData]]	
}
}% end


codedef MpiMaster(numWorkers, numIterations, masterSend, masterReceive, status) %{
	// Master routine
	
	// split iterations of the loop
	int clava_mpi_total_iter = [[numIterations]];
	int clava_mpi_loop_limit = clava_mpi_total_iter;
	// A better distribution calculation could be used
	int clava_mpi_num_iter = clava_mpi_total_iter / [[numWorkers]];
	int clava_mpi_num_iter_last = clava_mpi_num_iter + clava_mpi_total_iter % [[numWorkers]];
	// int clava_mpi_num_iter_last = clava_mpi_num_iter + (clava_mpi_loop_limit - (clava_mpi_num_iter * [[numWorkers]]));
	
	// send number of iterations
	for(int i=0; i<[[numWorkers]]-1; i++) {
		MPI_Send(&clava_mpi_num_iter, 1, MPI_INT, i+1, 1, MPI_COMM_WORLD);
	}
	MPI_Send(&clava_mpi_num_iter_last, 1, MPI_INT, [[numWorkers]], 1, MPI_COMM_WORLD);
	
	
	[[masterSend]]
	
	MPI_Status [[status]];
	
	[[masterReceive]]
	
	MPI_Finalize();
}% end