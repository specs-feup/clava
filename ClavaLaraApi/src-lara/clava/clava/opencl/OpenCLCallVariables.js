/**
 * Contains information about the variables created when inserting an OpenCL call.
 */
export default class OpenCLCallVariables {
    id;
    constructor(id) {
        this.id = id;
    }
    getKernelFile() {
        return this.id + "_kernel_file";
    }
    getKernelString() {
        return this.id + "_kernel_string";
    }
    getKernelStringSize() {
        return this.id + "_kernel_string_size";
    }
    getErrorCode() {
        return "opencl_error_code";
    }
    getNumPlatforms() {
        return "opencl_num_platforms";
    }
    getPlatformId() {
        return "opencl_platform_id";
    }
}
//# sourceMappingURL=OpenCLCallVariables.js.map