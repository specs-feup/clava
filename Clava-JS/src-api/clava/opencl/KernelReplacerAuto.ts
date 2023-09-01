import clava.opencl.KernelReplacer;

import lara.Io;
import lara.Strings;


//	This aspect can be included in a library, imported and
// called by a user, since it needs no configuration/parameterization
aspectdef KernelReplacerAuto

	// look for pragma
	select file.pragma{"clava"} end
	apply
	
		if(!$pragma.content.startsWith("opencl_call")) {
			continue;
		}

		var configFilename = Strings.extractValue("opencl_call", $pragma.content).trim(); 		
		var configFile = Io.getPath($file.path, configFilename);
		
		if(!Io.isFile(configFile)) {
			println("Expected to find the config file '"+configFilename+"' in the folder '"+$file.path+"'");
			continue;
		}
		
		var config = Io.readJson(configFile);
	
		var $call = $pragma.target.getDescendants('call')[0];
		var kernel = new KernelReplacer($call,
			config.kernelName, config.kernelFile,
			config.bufferSizes,
			config.localSize, config.iterNumbers);

		for(var outBuf of config.outputBuffers) {
			kernel.setOutput(outBuf);
		}
		
		kernel.replaceCall();
	end
end
