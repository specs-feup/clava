import clava.ClavaJoinPoints;
import lara.Io;
import lara.Strings;

function KernelReplacer($call, kernelName, kernelCodePath, bufferSizes, localSize, numIters) {

	// TODO: verify all parameters

	// join points
	this._call = $call;
	this._function = $call.definition;
	this._stmt = $call.getAncestor('statement');
	this._file = $call.getAncestor('file');

	// buffer information
	this._bufferSizes = bufferSizes; // maps param index to buffer information TODO: improve how this is stored and used
	this._inBuffers = this._makeInBuffers();
	this._outBuffers = {};
	this._inOutBuffers = {};

	// kernel name and source
	var kernelFile = Io.getPath(this._file.path, kernelCodePath);
	if(!Io.isFile(kernelFile)) {
		throw "[KernelReplacer] Cannot read OpenCL file in location " + kernelFile + " (" + kernelCodePath + ")";
	}
	this._kernelCode = readFile(kernelFile);
	this._sourceStringName = undefined;
	this._kernelName = kernelName;

	// device type
	this._deviceType = DeviceType.CL_DEVICE_TYPE_ALL; // TODO: make a setter for this

	// error handling
	this._errorHandling = ErrorHandling.EXIT; // TODO: make setter for this

	// local size and number of iterations (global size)
	if(isArray(localSize)) {
		this._localSize = localSize;
	} else {
		this._localSize = [localSize];
	}
	
	if(isArray(numIters)) {
		this._iter = numIters;
	} else {
		this._iter = [numIters];
	}
	
	checkTrue(this._localSize.length === this._iter.length, 'localSize and numIters must have the same number of dimensions', 'KernelReplacer()');
}

/* ------------------------- PUBLIC METHODS ------------------------- */

KernelReplacer.prototype.setOutput = function(paramName) {
	// TODO: check it exists
	this._outBuffers[paramName] = this._inBuffers[paramName];
	this._outBuffers[paramName]._kind = BufferKind.OUTPUT;
	delete this._inBuffers[paramName];
};

KernelReplacer.prototype.replaceCall = function() {

	var $parentFile = this._file;
	
	$parentFile.exec addInclude("CL/cl.hpp", true);
	$parentFile.exec insertBegin('#define __CL_ENABLE_EXCEPTIONS');
	
	var type = ClavaJoinPoints.typeLiteral('const char *');
	this._sourceStringName = this._kernelName + '_source_code';
	$parentFile.exec addGlobal(this._sourceStringName, type, this._makeKernelCode());

	var code = this._makeCode();
	var $codeStmt = ClavaJoinPoints.stmtLiteral(code);
	this._stmt.replaceWith($codeStmt);
};

/* ------------------------- PRIVATE METHODS ------------------------ */

KernelReplacer.prototype._makeKernelCode = function() {
	
	return '"' + Strings.escapeJson(this._kernelCode) + '"';
};

KernelReplacer.prototype._makeCode = function() {
	
	var code = '// start of OpenCL code\n';
	
	code += 'cl::Program program;\n';
	code += 'std::vector<cl::Device> devices;\n';
	code += 'try {\n';
	
	code += SetupCode(this._deviceType, this._makeErrorHandlingCode());
	code += this._makeBuffersCode();
	code += KernelCreation(this._sourceStringName, this._kernelName);
	code += this._makeArgBindCode();
	code += SizesDecl(this._localSize.join(', '), this._makeGlobalSizeCode());
	code += EnqueueKernel();
	code += this._makeOutputBuffersCode();
	code += ExceptionCode();

	code += '\n// end of OpenCL code\n\n';
	
	return code;
};

KernelReplacer.prototype._makeGlobalSizeCode = function() {

	var codes = [];
	var code = 'cl::NDRange globalSize(';
	for(var i in this._localSize) {
		var local = this._localSize[i];
		var global = this._iter[i];
		codes.push('(int)(ceil(' + global + '/(float)' + local + ')*' + local + ')');
	}
	code += codes.join(', ');
	code += ');';
	return code;
};

KernelReplacer.prototype._makeOutputBuffersCode = function() {

	var code = '\n// Read back buffers\n';
	for (inOutBuf of getValues(this._inOutBuffers)) {
		code += BufferCopyOut(inOutBuf._bufferName, inOutBuf._size, inOutBuf._argName);
	}
	for (outBuf of getValues(this._outBuffers)) {
		code += BufferCopyOut(outBuf._bufferName, outBuf._size, outBuf._argName);
	}

	return code;
};

KernelReplacer.prototype._makeArgBindCode = function() {

	var code = '';

	for(var index in this._call.args) {
		var $arg = this._call.args[index];
	
		var paramName = this._function.params[index].name;

		var inTry = this._inBuffers[paramName];
		if(inTry !== undefined) {
			code += ArgBind(index, inTry._bufferName);
		} else {
			var inOutTry = this._inOutBuffers[paramName];
			if(inOutTry !== undefined) {
				code += ArgBind(index, inOutTry._bufferName);
			} else {
				var outTry = this._outBuffers[paramName];
				if(outTry !== undefined) {
					code += ArgBind(index, outTry._bufferName);
				} else {
					code += ArgBind(index, $arg.name);
				}
			}
		}
	}

	return '\n// Bind kernel arguments to kernel\n' + code;
};

KernelReplacer.prototype._makeBuffersCode = function() {
	var code = '\n// Create device memory buffers\n';

	for (inBuf of getValues(this._inBuffers)) {
		code += BufferDecl(inBuf._bufferName, inBuf._kind, inBuf._size);
	}

	for (outBuf of getValues(this._outBuffers)) {
		code += BufferDecl(outBuf._bufferName, outBuf._kind, outBuf._size);
	}

	for (inOutBuf of getValues(this._inOutBuffers)) {
		code += BufferDecl(inOutBuf._bufferName, inOutBuf._kind, inOutBuf._size);
	}

	code += '\n// Bind memory buffers\n';
	for (inBuf of getValues(this._inBuffers)) {
		code += BufferCopyIn(inBuf._bufferName, inBuf._size, inBuf._argName);
	}
	for (inOutBuf of getValues(this._inOutBuffers)) {
		code += BufferCopyIn(inOutBuf._bufferName, inOutBuf._size, inOutBuf._argName);
	}

	return code;
};

KernelReplacer.prototype._makeErrorHandlingCode = function() {

	switch (this._errorHandling) {

		case ErrorHandling.EXIT:
			return 'exit(EXIT_FAILURE);';
		default:
			return 'exit(EXIT_FAILURE);';
	}
};

KernelReplacer.prototype._makeInBuffers = function() {

	var buffers = {};
	
	// iterate over function parameters
	var params = this._function.params;
	for(var i in params) {
		var $param = params[i];
		// pick arrays/pointers
		if($param.type.isArray || $param.type.isPointer) {

			var bufferSize = this._getBufferSize($param.name);
			var $baseType = this._getBaseType($param.type);
			var argName = this._call.args[i].code;
			var info = new Buffer(BufferKind.INPUT, $param.name, $baseType, i, bufferSize, argName, $param.name + '_buffer');
			buffers[$param.name] = info;
		}
	}
	
	return buffers;
};

KernelReplacer.prototype._getBufferSize = function(paramName) {
	if(this._bufferSizes[paramName] !== undefined) {
		return this._bufferSizes[paramName];
	} else {
		return this._bufferSizes;
	}
};

KernelReplacer.prototype._getBaseType = function($type) {

	var $newType = $type;
	while(!$newType.isBuiltin) {
		$newType = $newType.unwrap;
	}
	
	return $newType;
}

/* ------------------------- PRIVATE CLASSES ------------------------ */

function Buffer(kind, paramName, baseType, index, size, argName, bufferName) {
	this._kind = kind;
	this._paramName = paramName;
	this._baseType = baseType;
	this._index = index;
	this._size = size;
	this._argName = argName;
	this._bufferName = bufferName;
}

/* ------------------------------ ENUMS ----------------------------- */

var BufferKind = {
	INPUT: "CL_MEM_READ_ONLY",
	OUTPUT: "CL_MEM_WRITE_ONLY",
	INPUT_OUTPUT: "CL_MEM_READ_WRITE"
};

var DeviceType = {
	CL_DEVICE_TYPE_ALL: "CL_DEVICE_TYPE_ALL",
	CL_DEVICE_TYPE_CPU: "CL_DEVICE_TYPE_CPU",
	CL_DEVICE_TYPE_GPU: "CL_DEVICE_TYPE_GPU",
	CL_DEVICE_TYPE_ACCELERATOR: "CL_DEVICE_TYPE_ACCELERATOR",
	CL_DEVICE_TYPE_DEFAULT: "CL_DEVICE_TYPE_DEFAULT"
};

var ErrorHandling = {
	EXIT: 0,
	RETURN: 1,
	USER: 2
};

/* ---------------------------- CODEDEFS ---------------------------- */

codedef SetupCode(deviceType, errorHandling) %{
// Query platforms
std::vector<cl::Platform> platforms;
cl::Platform::get(&platforms);
if (platforms.size() == 0) {
  std::cout << "Platform size 0\n";
  [[errorHandling]]
}

// Get list of devices on default platform and create context
cl_context_properties properties[] =
 { CL_CONTEXT_PLATFORM, (cl_context_properties)(platforms[0])(),
	   0};
cl::Context context([[deviceType]], properties);
devices = context.getInfo<CL_CONTEXT_DEVICES>();

// Create command queue for first device
cl::CommandQueue queue(context, devices[0], 0);

}%
end

codedef ExceptionCode () %{
} catch (cl::Error err) {
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
}%
end

codedef BufferCopyOut(bufferName, bufferSize, argName) %{
queue.enqueueReadBuffer([[bufferName]], CL_TRUE, 0, [[bufferSize]], [[argName]]);

}%
end

codedef EnqueueKernel () %{
// Enqueue kernel
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

}%
end

codedef SizesDecl(localsize, globalsizeCode) %{
// Number of work items in each local work group
cl::NDRange localSize([[localsize]]);
// Number of total work items - localSize must be devisor
[[globalsizeCode]]
        
}%
end

codedef ArgBind (index, arg) %{
kernel.setArg([[index]], [[arg]]);

}%
end

codedef KernelCreation (sourceString, kernelName) %{
//Build kernel from source string
cl::Program::Sources source(1,
  std::make_pair([[sourceString]],strlen([[sourceString]])));
program = cl::Program(context, source);
program.build(devices);

// Create kernel object
cl::Kernel kernel(program, "[[kernelName]]");

}%
end

codedef BufferDecl(bufferName, bufferKind, bufferSize) %{
cl::Buffer [[bufferName]] = cl::Buffer(context, [[bufferKind]], [[bufferSize]]);

}%
end

codedef BufferCopyIn(bufferName, bufferSize, argName) %{
queue.enqueueWriteBuffer([[bufferName]], CL_TRUE, 0, [[bufferSize]], [[argName]]);

}%
end
