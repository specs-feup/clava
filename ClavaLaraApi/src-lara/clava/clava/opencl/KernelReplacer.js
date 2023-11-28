import Io from "lara-js/api/lara/Io.js";
import Strings from "lara-js/api/lara/Strings.js";
import { BuiltinType, } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
export default class KernelReplacer {
    call;
    function;
    stmt;
    file;
    bufferSizes = new Map();
    inBuffers;
    outBuffers = new Map();
    inOutBuffers = new Map();
    kernelCode;
    kernelName;
    deviceType;
    errorHandling;
    localSize;
    iter;
    constructor($call, kernelName, kernelCodePath, bufferSizes, localSize, numIters) {
        // TODO: verify all parameters
        // join points
        this.call = $call;
        this.function = $call.definition;
        this.stmt = $call.getAncestor("statement");
        this.file = $call.getAncestor("file");
        // buffer information
        for (const key of Object.keys(bufferSizes)) {
            this.bufferSizes.set(key, bufferSizes[key]);
        }
        this.inBuffers = this.makeInBuffers();
        // kernel name and source
        const kernelFile = Io.getPath(this.file.path, kernelCodePath);
        if (!Io.isFile(kernelFile)) {
            throw ("[KernelReplacer] Cannot read OpenCL file in location " +
                kernelFile.toString() +
                " (" +
                kernelCodePath +
                ")");
        }
        this.kernelCode = Io.readFile(kernelFile);
        this.kernelName = kernelName;
        // device type
        this.deviceType = DeviceType.CL_DEVICE_TYPE_ALL; // TODO: make a setter for this
        // error handling
        this.errorHandling = ErrorHandling.EXIT; // TODO: make setter for this
        // local size and number of iterations (global size)
        if (localSize instanceof Array) {
            this.localSize = localSize;
        }
        else {
            this.localSize = [localSize];
        }
        if (numIters instanceof Array) {
            this.iter = numIters;
        }
        else {
            this.iter = [numIters];
        }
        if (this.localSize.length !== this.iter.length) {
            throw new Error("KernelReplacer(): localSize and numIters must have the same number of dimensions");
        }
    }
    /* ------------------------- PUBLIC METHODS ------------------------- */
    setOutput(paramName) {
        const inBuf = this.inBuffers.get(paramName);
        if (inBuf == undefined) {
            throw new Error(`No input buffer found for parameter '${paramName}'`);
        }
        this.outBuffers.set(paramName, inBuf);
        this.outBuffers.get(paramName)._kind = BufferKind.OUTPUT;
        this.inBuffers.delete(paramName);
    }
    replaceCall() {
        const $parentFile = this.file;
        $parentFile.addInclude("CL/cl.hpp", true);
        $parentFile.insertBegin("#define __CL_ENABLE_EXCEPTIONS");
        const type = ClavaJoinPoints.typeLiteral("const char *");
        const sourceStringName = this.kernelName + "_source_code";
        $parentFile.addGlobal(sourceStringName, type, this.makeKernelCode());
        const code = this.makeCode(sourceStringName);
        const $codeStmt = ClavaJoinPoints.stmtLiteral(code);
        this.stmt.replaceWith($codeStmt);
    }
    /* ------------------------- PRIVATE METHODS ------------------------ */
    makeKernelCode() {
        return '"' + Strings.escapeJson(this.kernelCode) + '"';
    }
    makeCode(sourceStringName) {
        let code = "// start of OpenCL code\n";
        code += "cl::Program program;\n";
        code += "std::vector<cl::Device> devices;\n";
        code += "try {\n";
        code += KernelReplacer.SetupCode(this.deviceType, this.makeErrorHandlingCode());
        code += this.makeBuffersCode();
        code += KernelReplacer.KernelCreation(sourceStringName, this.kernelName);
        code += this.makeArgBindCode();
        code += KernelReplacer.SizesDecl(this.localSize.join(", "), this.makeGlobalSizeCode());
        code += KernelReplacer.EnqueueKernel();
        code += this.makeOutputBuffersCode();
        code += KernelReplacer.ExceptionCode();
        code += "\n// end of OpenCL code\n\n";
        return code;
    }
    makeGlobalSizeCode() {
        const codes = [];
        let code = "cl::NDRange globalSize(";
        for (let i = 0; i < this.localSize.length; i++) {
            const local = this.localSize[i];
            const global = this.iter[i];
            codes.push("(int)(ceil(" + global + "/(float)" + local + ")*" + local + ")");
        }
        code += codes.join(", ");
        code += ");";
        return code;
    }
    makeOutputBuffersCode() {
        let code = "\n// Read back buffers\n";
        this.inOutBuffers.forEach((inOutBuf) => {
            code += KernelReplacer.BufferCopyOut(inOutBuf._bufferName, inOutBuf._size, inOutBuf._argName);
        });
        this.outBuffers.forEach((outBuf) => {
            code += KernelReplacer.BufferCopyOut(outBuf._bufferName, outBuf._size, outBuf._argName);
        });
        return code;
    }
    makeArgBindCode() {
        let code = "";
        for (let index = 0; index < this.call.args.length; index++) {
            const $arg = this.call.args[index];
            const paramName = this.function.params[index].name;
            const inTry = this.inBuffers.get(paramName);
            if (inTry !== undefined) {
                code += KernelReplacer.ArgBind(String(index), inTry._bufferName);
            }
            else {
                const inOutTry = this.inOutBuffers.get(paramName);
                if (inOutTry !== undefined) {
                    code += KernelReplacer.ArgBind(String(index), inOutTry._bufferName);
                }
                else {
                    const outTry = this.outBuffers.get(paramName);
                    if (outTry !== undefined) {
                        code += KernelReplacer.ArgBind(String(index), outTry._bufferName);
                    }
                    else {
                        code += KernelReplacer.ArgBind(String(index), $arg.code);
                    }
                }
            }
        }
        return "\n// Bind kernel arguments to kernel\n" + code;
    }
    makeBuffersCode() {
        let code = "\n// Create device memory buffers\n";
        this.inBuffers.forEach((inBuf) => {
            code += KernelReplacer.BufferDecl(inBuf._bufferName, inBuf._kind, inBuf._size);
        });
        this.outBuffers.forEach((outBuf) => {
            code += KernelReplacer.BufferDecl(outBuf._bufferName, outBuf._kind, outBuf._size);
        });
        this.inOutBuffers.forEach((inOutBuf) => {
            code += KernelReplacer.BufferDecl(inOutBuf._bufferName, inOutBuf._kind, inOutBuf._size);
        });
        code += "\n// Bind memory buffers\n";
        this.inBuffers.forEach((inBuf) => {
            code += KernelReplacer.BufferCopyIn(inBuf._bufferName, inBuf._size, inBuf._argName);
        });
        this.inOutBuffers.forEach((inOutBuf) => {
            code += KernelReplacer.BufferCopyIn(inOutBuf._bufferName, inOutBuf._size, inOutBuf._argName);
        });
        return code;
    }
    makeErrorHandlingCode() {
        switch (this.errorHandling) {
            case ErrorHandling.EXIT:
                return "exit(EXIT_FAILURE);";
            default:
                return "exit(EXIT_FAILURE);";
        }
    }
    makeInBuffers() {
        const buffers = new Map();
        // iterate over function parameters
        const params = this.function.params;
        for (let i = 0; i < params.length; i++) {
            const $param = params[i];
            // pick arrays/pointers
            if ($param.type.isArray || $param.type.isPointer) {
                const bufferSize = this.getBufferSize($param.name);
                const $baseType = this.getBaseType($param.type);
                const argName = this.call.args[i].code;
                const info = new Buffer(BufferKind.INPUT, $param.name, $baseType, i, bufferSize, argName, $param.name + "_buffer");
                buffers.set($param.name, info);
            }
        }
        return buffers;
    }
    getBufferSize(paramName) {
        const bufferSize = this.bufferSizes.get(paramName);
        if (bufferSize == undefined) {
            throw new Error(`Ǹo buffer size found for parameter '${paramName}'`);
        }
        return bufferSize;
    }
    getBaseType($type) {
        let $newType = $type;
        while (!($newType instanceof BuiltinType)) {
            $newType = $newType.unwrap;
        }
        return $newType;
    }
    /* ---------------------------- CODEDEFS ---------------------------- */
    static SetupCode(deviceType, errorHandling) {
        return `// Query platforms
std::vector<cl::Platform> platforms;
cl::Platform::get(&platforms);
if (platforms.size() == 0) {
  std::cout << "Platform size 0\n";
  ${errorHandling}
}

// Get list of devices on default platform and create context
cl_context_properties properties[] =
 { CL_CONTEXT_PLATFORM, (cl_context_properties)(platforms[0])(),
       0};
cl::Context context(${deviceType}, properties);
devices = context.getInfo<CL_CONTEXT_DEVICES>();

// Create command queue for first device
cl::CommandQueue queue(context, devices[0], 0);

`;
    }
    static ExceptionCode() {
        return `} catch (cl::Error err) {
    std::cerr << "ERROR: "<<err.what()<<"("<<err.err()<<")"<<std::endl;
    if (err.err() == CL_BUILD_PROGRAM_FAILURE) {
        for (cl::Device dev : devices) {
            // Check the build status
            cl_build_status status = program.getBuildInfo<CL_PROGRAM_BUILD_STATUS>(dev);
            if (status != CL_BUILD_ERROR)
                continue;
            
            // Get the build log
            std::string name = dev.getInfo<CL_DEVICE_NAME>();
            std::string buildlog = program.getBuildInfo<CL_PROGRAM_BUILD_LOG>(dev);
            std::cerr << "Build log for " << name << ":" << std::endl << buildlog << std::endl;
        }
    } else {
        throw err;
    }
}
`;
    }
    static BufferCopyOut(bufferName, bufferSize, argName) {
        return `queue.enqueueReadBuffer(${bufferName}, CL_TRUE, 0, ${bufferSize}, ${argName});

`;
    }
    static EnqueueKernel() {
        return `// Enqueue kernel
cl::Event event;
queue.enqueueNDRangeKernel(
    kernel,
    cl::NullRange,
    globalSize,
    localSize,
    NULL,
    &event);

// Block until kernel completion
event.wait();

`;
    }
    static SizesDecl(localsize, globalsizeCode) {
        return `// Number of work items in each local work group
cl::NDRange localSize(${localsize});
// Number of total work items - localSize must be devisor
${globalsizeCode}

`;
    }
    static ArgBind(index, arg) {
        return `kernel.setArg(${index}, ${arg});

`;
    }
    static KernelCreation(sourceString, kernelName) {
        return `//Build kernel from source string
cl::Program::Sources source(1,
  std::make_pair(${sourceString},strlen(${sourceString})));
program = cl::Program(context, source);
program.build(devices);

// Create kernel object
cl::Kernel kernel(program, "${kernelName}");

`;
    }
    static BufferDecl(bufferName, bufferKind, bufferSize) {
        return `cl::Buffer ${bufferName} = cl::Buffer(context, ${bufferKind}, ${bufferSize});

`;
    }
    static BufferCopyIn(bufferName, bufferSize, argName) {
        return `queue.enqueueWriteBuffer(${bufferName}, CL_TRUE, 0, ${bufferSize}, ${argName});

`;
    }
}
/* ------------------------- PRIVATE CLASSES ------------------------ */
class Buffer {
    _kind;
    _paramName;
    _baseType;
    _index;
    _size;
    _argName;
    _bufferName;
    constructor(kind, paramName, baseType, index, size, argName, bufferName) {
        this._kind = kind;
        this._paramName = paramName;
        this._baseType = baseType;
        this._index = index;
        this._size = size;
        this._argName = argName;
        this._bufferName = bufferName;
    }
}
/* ------------------------------ ENUMS ----------------------------- */
var BufferKind;
(function (BufferKind) {
    BufferKind["INPUT"] = "CL_MEM_READ_ONLY";
    BufferKind["OUTPUT"] = "CL_MEM_WRITE_ONLY";
    BufferKind["INPUT_OUTPUT"] = "CL_MEM_READ_WRITE";
})(BufferKind || (BufferKind = {}));
var DeviceType;
(function (DeviceType) {
    DeviceType["CL_DEVICE_TYPE_ALL"] = "CL_DEVICE_TYPE_ALL";
    DeviceType["CL_DEVICE_TYPE_CPU"] = "CL_DEVICE_TYPE_CPU";
    DeviceType["CL_DEVICE_TYPE_GPU"] = "CL_DEVICE_TYPE_GPU";
    DeviceType["CL_DEVICE_TYPE_ACCELERATOR"] = "CL_DEVICE_TYPE_ACCELERATOR";
    DeviceType["CL_DEVICE_TYPE_DEFAULT"] = "CL_DEVICE_TYPE_DEFAULT";
})(DeviceType || (DeviceType = {}));
var ErrorHandling;
(function (ErrorHandling) {
    ErrorHandling[ErrorHandling["EXIT"] = 0] = "EXIT";
    ErrorHandling[ErrorHandling["RETURN"] = 1] = "RETURN";
    ErrorHandling[ErrorHandling["USER"] = 2] = "USER";
})(ErrorHandling || (ErrorHandling = {}));
//# sourceMappingURL=KernelReplacer.js.map