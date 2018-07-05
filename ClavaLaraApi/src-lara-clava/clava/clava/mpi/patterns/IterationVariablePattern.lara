import clava.mpi.MpiAccessPattern;
import clava.mpi.MpiUtils;

/**
 * Array that is accessed using only the iteration variable directly, without modifications.
 */
function IterationVariablePattern() {
    // Parent constructor
    MpiAccessPattern.call(this);
}
// Inheritance
IterationVariablePattern.prototype = Object.create(MpiAccessPattern.prototype);


IterationVariablePattern.prototype.sendMaster = function($varJp, totalIterations) {
	var varName = $varJp.name;
	var numIterations = NumIterations(totalIterations, MpiUtils.VAR_NUM_WORKERS);
	var numIterationsLast = NumIterationsLast(numIterations, totalIterations, MpiUtils.VAR_NUM_WORKERS);

	var $adjustedType = this._adjustType($varJp.type, totalIterations);
	
	// Type must be an array
	if(!$adjustedType.isArray) {
//	if(!$varJp.type.isArray) {
		throw "Expected type to be an array, is '"+$adjustedType.astName+"'";
		//throw "Expected type to be an array, is '"+$varJp.type.astName+"'";
	}
			
	var mpiType = MpiUtils.getMpiType($adjustedType.elementType);
	//var mpiType = MpiUtils.getMpiType($varJp.type.normalize.elementType);

	return SendMaster(varName, MpiUtils.VAR_NUM_WORKERS, numIterations, numIterationsLast, mpiType);
}

IterationVariablePattern.prototype.receiveMaster = function($varJp, totalIterations) {
	var varName = $varJp.name;
	var numIterations = NumIterations(totalIterations, MpiUtils.VAR_NUM_WORKERS);
	var numIterationsLast = NumIterationsLast(numIterations, totalIterations, MpiUtils.VAR_NUM_WORKERS);

	var $adjustedType = this._adjustType($varJp.type, totalIterations);
	
	// Type must be an array
	if(!$adjustedType.isArray) {
		throw "Expected type to be an array, is '"+$adjustedType.astName+"'";
	}
	//if(!$varJp.type.isArray) {
	//	throw "Expected type to be an array, is '"+$varJp.type.astName+"'";
	//}
			
	//var mpiType = MpiUtils.getMpiType($varJp.type.normalize.elementType);
	var mpiType = MpiUtils.getMpiType($adjustedType.elementType);

	return ReceiveMaster(varName, MpiUtils.VAR_NUM_WORKERS, numIterations, numIterationsLast, mpiType, MpiUtils._VAR_MPI_STATUS);
}

IterationVariablePattern.prototype.sendWorker = function($varJp, totalIterations) {
	//var mpiType = MpiUtils.getMpiType($varJp.type.normalize.elementType);
	var mpiType = MpiUtils.getMpiType(this._adjustType($varJp.type, totalIterations).elementType);
	
	return "MPI_Send(&"+$varJp.name+", "+totalIterations+", "+mpiType+", 0, 1, MPI_COMM_WORLD);\n";
}

IterationVariablePattern.prototype.receiveWorker = function($varJp, totalIterations) {

	// Adjust type, if needed
	var $adjustedType = this._adjustType($varJp.type, totalIterations);
	//println("ORIGINAL TYPE:" + $varJp.type.ast);
	//println("ADJUSTED TYPE:" + $adjustedType.ast);
	
	// Declare type
	var $varDecl = ClavaJoinPoints.varDeclNoInit($varJp.name, $adjustedType);
	//var mpiType = MpiUtils.getMpiType($varJp.type.normalize.elementType);
	var mpiType = MpiUtils.getMpiType($adjustedType.elementType);
	
	var code = $varDecl.code + ";\n";
	code = code + "MPI_Recv(&"+$varJp.name+", "+totalIterations+", "+mpiType+", 0, 1, MPI_COMM_WORLD, &"+MpiUtils._VAR_MPI_STATUS+");\n";
//    double a[mpi_loop_num_elems];
//    MPI_Recv(&a, mpi_loop_num_elems, MPI_DOUBLE, 0, 1, MPI_COMM_WORLD, &status);


	return code;
} 

IterationVariablePattern.prototype.outputDeclWorker = function($varJp, totalIterations) {

	// Adjust type, if needed
	var $adjustedType = this._adjustType($varJp.type, totalIterations);
	
	// Declare type
	var $varDecl = ClavaJoinPoints.varDeclNoInit($varJp.name, $adjustedType);

	return $varDecl.code + ";\n";

}


/** PRIVATE FUNCTIONS **/

IterationVariablePattern.prototype._adjustType = function($type, totalIterations) {
	if($type.isArray) {
		// TODO: Support for multidimensional arrays
		var $sizeExpr = ClavaJoinPoints.exprLiteral(totalIterations, "int");
		return ClavaJoinPoints.variableArrayType($type.normalize.elementType, $sizeExpr);
	}
	
	// Pointer types that have this kind of access pattern are considered arrays
	if($type.isPointer) {
		// TODO: Support for multidimensional arrays
		var $sizeExpr = ClavaJoinPoints.exprLiteral(totalIterations, "int");
		return ClavaJoinPoints.variableArrayType($type.pointee, $sizeExpr);
	}
	
	return $type;
}



/** CODEDEFS **/

codedef NumIterations(totalIterations, numWorkers) %{
([[totalIterations]] / [[numWorkers]])
}% end

codedef NumIterationsLast(numIterations, totalIterations, numWorkers) %{
([[numIterations]] + [[totalIterations]] % [[numWorkers]])
}% end

codedef SendMaster(varName, numWorkers, numIterations, numIterationsLast, mpiType) %{
// send input [[varName]] - elements: iteration_space
for(int i=0; i<[[numWorkers]]-1; i++) {
	MPI_Send(&[[varName]][i*[[numIterations]]], [[numIterations]], [[mpiType]], i + 1,1, MPI_COMM_WORLD);
}
MPI_Send(&[[varName]][([[numWorkers]]-1)*[[numIterations]]], [[numIterationsLast]], [[mpiType]], [[numWorkers]],1, MPI_COMM_WORLD);
}% end

codedef ReceiveMaster(varName, numWorkers, numIterations, numIterationsLast, mpiType, status) %{
// receive output [[varName]] - elements: iteration_space
for(int i=0; i<[[numWorkers]]-1; i++) {
	MPI_Recv(&[[varName]][i*[[numIterations]]], [[numIterations]], [[mpiType]], i + 1, 1, MPI_COMM_WORLD, &[[status]]);
}
MPI_Recv(&[[varName]][([[numWorkers]]-1)*[[numIterations]]], [[numIterationsLast]], [[mpiType]], [[numWorkers]], 1, MPI_COMM_WORLD, &[[status]]);
}% end


