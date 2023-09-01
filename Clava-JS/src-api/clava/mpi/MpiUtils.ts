import lara.Strings;

/**
 * Utility methods related to MPI.
 *
 * @class
 */
var MpiUtils = {};

MpiUtils.VAR_NUM_TASKS = "mpi_num_tasks";
MpiUtils.VAR_NUM_WORKERS = "mpi_num_workers";
MpiUtils.VAR_RANK = "mpi_rank";
MpiUtils._VAR_MPI_STATUS = "mpi_status";


MpiUtils.getMpiType = function($type) {
// Reference: https://msdn.microsoft.com/en-us/library/windows/desktop/dn473290(v=vs.85).aspx

	if($type.astName === 'BuiltinType') {
		var typeCode = $type.code.toUpperCase();
	
		return "MPI_" + Strings.replacer(typeCode, " ", "_");
	}
	
	throw "Not implemented for type '"+$type.astName+"'";
}



/**
 * Enables/disables library SpecsLogger for printing.
 * <p>
 * By default, is disabled.
 */
 /*
MpiUtils.getNumTaksVar = function() {
	
}
*/