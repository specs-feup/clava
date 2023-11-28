import Io from "lara-js/api/lara/Io.js";
import Platforms from "lara-js/api/lara/Platforms.js";
import IdGenerator from "lara-js/api/lara/util/IdGenerator.js";
import { Call, FileJp, FunctionJp } from "../../Joinpoints.js";
import OpenCLCallVariables from "./OpenCLCallVariables.js";

export default class OpenCLCall {
  $kernel: FunctionJp | undefined = undefined;
  deviceId: number = 1;

  setKernel($function: FunctionJp) {
    if (!($function.getAncestor("file") as FileJp).isOpenCL) {
      throw new Error(
        "OpenCLCall.setKernel: expected a function in an OpenCL file"
      );
    }

    this.$kernel = $function;
  }

  setDeviceId(deviceId: number) {
    this.deviceId = deviceId;
  }

  replaceCall($call: Call) {
    this.replaceCallPreconditions();

    // Add include
    this.addOpenCLInclude($call);

    // Generate id
    const id = IdGenerator.next("opencl_call_");
    const variables = new OpenCLCallVariables(id);

    this.loadKernelFile($call, variables);
    this.clInit($call, variables);
  }

  private replaceCallPreconditions() {
    if (this.$kernel == undefined) {
      throw new Error(
        "OpenCLCall._replaceCallPreconditions: Expected kernel to be set"
      );
    }
  }

  private addOpenCLInclude($call: Call) {
    const $file = $call.getAncestor("file") as FileJp;

    // If MacOS, add include <OpenCL/opencl.h>
    if (Platforms.isMac()) {
      $file.addInclude("OpenCL/opencl.h", true);
      return;
    }

    // Otherwise, <CL/cl.h>
    $file.addInclude("CL/cl.h", true);
  }

  private loadKernelFile($call: Call, variables: OpenCLCallVariables) {
    // Get necessary data
    const $kernelFile = $call.getAncestor("file") as FileJp;
    const kernelPath = $kernelFile.relativeFilepath;
    const kernelFileBytes: number = Io.getPath($kernelFile.filepath).length;

    // Insert before the call
    $call.insertBefore(
      `// Load the kernel source code into the array
FILE *${variables.getKernelFile()} = fopen("${kernelPath}", "r");
if (!${variables.getKernelFile()}) {
	fprintf(stdout, "Failed to load kernel.\n");
	exit(1);
}
char *${variables.getKernelString()} = (char*)malloc(${kernelFileBytes});
size_t ${variables.getKernelStringSize()} = fread(${variables.getKernelString()}, 1, ${kernelFileBytes}, ${variables.getKernelFile()});
fclose(${variables.getKernelFile()});`
    );
  }

  /**
   * This only needs to be done once per function
   */
  private clInit($call: Call, variables: OpenCLCallVariables) {
    // TODO: Set of functions where this has been called

    // Insert before the call
    $call.insertBefore(
      `cl_int ${variables.getErrorCode()};
cl_uint ${variables.getNumPlatforms()};
cl_platform_id ${variables.getPlatformId()};

// Check the number of platforms
${variables.getErrorCode()} = clGetPlatformIDs(0, NULL, &${variables.getNumPlatforms()});
if(${variables.getErrorCode()} != CL_SUCCESS) {
	fprintf(stderr, "[OpenCL] Error getting number of platforms\n");
	exit(1);
} else if(${variables.getNumPlatforms()} == 0) {
	fprintf(stderr, "[OpenCL] No platforms found.\n");
	exit(1);
} else {
	printf("[OpenCL] Number of platforms is %d\n",${variables.getNumPlatforms()});
}

${variables.getErrorCode()} = clGetPlatformIDs(${
        this.deviceId
      }, &${variables.getPlatformId()}, NULL);
if(${variables.getErrorCode()} != CL_SUCCESS) {
	fprintf(stderr, "[OpenCL] Error getting platform ID for device ${
    this.deviceId
  }.\n");
	exit(1);
}`
    );
  }
}
