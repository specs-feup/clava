import MpiAccessPattern from "../MpiAccessPattern.js";
import MpiUtils from "../MpiUtils.js";
import ClavaJoinPoints from "../../ClavaJoinPoints.js";
import { ArrayType, PointerType, Type, Varref } from "../../../Joinpoints.js";

/**
 * Array that is accessed using only the iteration variable directly, without modifications.
 */
export default class IterationVariablePattern extends MpiAccessPattern {
  sendMaster($varJp: Varref, totalIterations: string): string {
    const varName = $varJp.name;
    const numIterations = IterationVariablePattern.NumIterations(
      totalIterations,
      MpiUtils.VAR_NUM_WORKERS
    );
    const numIterationsLast = IterationVariablePattern.NumIterationsLast(
      numIterations,
      totalIterations,
      MpiUtils.VAR_NUM_WORKERS
    );

    const $adjustedType = IterationVariablePattern.adjustType(
      $varJp.type,
      totalIterations
    );

    // Type must be an array
    if (!($adjustedType instanceof ArrayType)) {
      throw `Expected type to be an array, is '${$adjustedType.astName}'`;
    }

    const mpiType = MpiUtils.getMpiType($adjustedType.elementType);

    return IterationVariablePattern.SendMaster(
      varName,
      MpiUtils.VAR_NUM_WORKERS,
      numIterations,
      numIterationsLast,
      mpiType
    );
  }

  receiveMaster($varJp: Varref, totalIterations: string): string {
    const varName = $varJp.name;
    const numIterations = IterationVariablePattern.NumIterations(
      totalIterations,
      MpiUtils.VAR_NUM_WORKERS
    );
    const numIterationsLast = IterationVariablePattern.NumIterationsLast(
      numIterations,
      totalIterations,
      MpiUtils.VAR_NUM_WORKERS
    );

    const $adjustedType = IterationVariablePattern.adjustType(
      $varJp.type,
      totalIterations
    );

    // Type must be an array
    if (!($adjustedType instanceof ArrayType)) {
      throw "Expected type to be an array, is '" + $adjustedType.astName + "'";
    }

    const mpiType = MpiUtils.getMpiType($adjustedType.elementType);

    return IterationVariablePattern.ReceiveMaster(
      varName,
      MpiUtils.VAR_NUM_WORKERS,
      numIterations,
      numIterationsLast,
      mpiType,
      MpiUtils._VAR_MPI_STATUS
    );
  }

  sendWorker($varJp: Varref, totalIterations: string): string {
    const $adjustedType = IterationVariablePattern.adjustType(
      $varJp.type,
      totalIterations
    );

    // Type must be an array
    if (!($adjustedType instanceof ArrayType)) {
      throw "Expected type to be an array, is '" + $adjustedType.astName + "'";
    }

    const mpiType = MpiUtils.getMpiType($adjustedType.elementType);

    return `MPI_Send(&${$varJp.name}, ${totalIterations}, ${mpiType}, 0, 1, MPI_COMM_WORLD);\n`;
  }

  receiveWorker($varJp: Varref, totalIterations: string): string {
    // Adjust type, if needed
    const $adjustedType = IterationVariablePattern.adjustType(
      $varJp.type,
      totalIterations
    );

    // Type must be an array
    if (!($adjustedType instanceof ArrayType)) {
      throw "Expected type to be an array, is '" + $adjustedType.astName + "'";
    }

    // Declare type
    const $varDecl = ClavaJoinPoints.varDeclNoInit($varJp.name, $adjustedType);
    const mpiType = MpiUtils.getMpiType($adjustedType.elementType);

    return `${$varDecl.code};\nMPI_Recv(&${$varJp.name}, ${totalIterations}, ${mpiType}, 0, 1, MPI_COMM_WORLD, &${MpiUtils._VAR_MPI_STATUS});\n`;
  }

  outputDeclWorker($varJp: Varref, totalIterations: string): string {
    // Adjust type, if needed
    const $adjustedType = IterationVariablePattern.adjustType(
      $varJp.type,
      totalIterations
    );

    // Declare type
    const $varDecl = ClavaJoinPoints.varDeclNoInit($varJp.name, $adjustedType);

    return $varDecl.code + ";\n";
  }

  private static adjustType($type: Type, totalIterations: string) {
    if ($type instanceof ArrayType) {
      // TODO: Support for multidimensional arrays
      const $sizeExpr = ClavaJoinPoints.exprLiteral(totalIterations, "int");
      return ClavaJoinPoints.variableArrayType(
        ($type.normalize as ArrayType).elementType,
        $sizeExpr
      );
    }

    // Pointer types that have this kind of access pattern are considered arrays
    if ($type instanceof PointerType) {
      // TODO: Support for multidimensional arrays
      const $sizeExpr = ClavaJoinPoints.exprLiteral(totalIterations, "int");
      return ClavaJoinPoints.variableArrayType($type.pointee, $sizeExpr);
    }

    return $type;
  }

  private static NumIterations(totalIterations: string, numWorkers: string) {
    return `(${totalIterations} / ${numWorkers})`;
  }

  private static NumIterationsLast(
    numIterations: string,
    totalIterations: string,
    numWorkers: string
  ) {
    return `(${numIterations} + ${totalIterations} % ${numWorkers})`;
  }

  private static SendMaster(
    varName: string,
    numWorkers: string,
    numIterations: string,
    numIterationsLast: string,
    mpiType: string
  ) {
    return `// send input ${varName} - elements: iteration_space
for(int i=0; i<${numWorkers}-1; i++) {
    MPI_Send(&${varName}[i*${numIterations}], ${numIterations}, ${mpiType}, i + 1,1, MPI_COMM_WORLD);
}
MPI_Send(&${varName}[(${numWorkers}-1)*${numIterations}], ${numIterationsLast}, ${mpiType}, ${numWorkers},1, MPI_COMM_WORLD);
`;
  }

  private static ReceiveMaster(
    varName: string,
    numWorkers: string,
    numIterations: string,
    numIterationsLast: string,
    mpiType: string,
    status: string
  ) {
    return `// receive output ${varName} - elements: iteration_space
for(int i=0; i<${numWorkers}-1; i++) {
    MPI_Recv(&${varName}[i*${numIterations}], ${numIterations}, ${mpiType}, i + 1, 1, MPI_COMM_WORLD, &${status});
}
MPI_Recv(&${varName}[(${numWorkers}-1)*${numIterations}], ${numIterationsLast}, ${mpiType}, ${numWorkers}, 1, MPI_COMM_WORLD, &${status});
`;
  }
}
