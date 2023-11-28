import { BuiltinType } from "../../Joinpoints.js";
/**
 * Utility methods related to MPI.
 *
 */
export default class MpiUtils {
    static VAR_NUM_TASKS = "mpi_num_tasks";
    static VAR_NUM_WORKERS = "mpi_num_workers";
    static VAR_RANK = "mpi_rank";
    static _VAR_MPI_STATUS = "mpi_status";
    static getMpiType($type) {
        // Reference: https://msdn.microsoft.com/en-us/library/windows/desktop/dn473290(v=vs.85).aspx
        if ($type instanceof BuiltinType) {
            return "MPI_" + $type.code.toUpperCase().replace(" ", "_");
        }
        throw "Not implemented for type '" + $type.astName + "'";
    }
}
//# sourceMappingURL=MpiUtils.js.map