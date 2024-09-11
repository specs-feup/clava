import Io from "lara-js/api/lara/Io.js";
import Strings from "lara-js/api/lara/Strings.js";
import Query from "lara-js/api/weaver/Query.js";
import KernelReplacer from "./KernelReplacer.js";
//	This aspect can be included in a library, imported and
// called by a user, since it needs no configuration/parameterization
export default function KernelReplacerAuto() {
    // look for pragma
    for (const $p of Query.search("pragma", "clava")) {
        const $pragma = $p;
        const $file = $pragma.target.getAncestor("file");
        if (!$pragma.content.startsWith("opencl_call")) {
            continue;
        }
        const configFilename = Strings.extractValue("opencl_call", $pragma.content)?.trim();
        const configFile = Io.getAbsolutePath($file.path, configFilename);
        if (!Io.isFile(configFile)) {
            console.log(`Expected to find the config file '${configFilename}' in the folder '${$file.path}'`);
            continue;
        }
        const config = Io.readJson(configFile);
        const $call = $pragma.target.getDescendants("call")[0];
        const kernel = new KernelReplacer($call, config.kernelName, config.kernelFile, config.bufferSizes, config.localSize, config.iterNumbers);
        for (const outBuf of config.outputBuffers) {
            kernel.setOutput(outBuf);
        }
        kernel.replaceCall();
    }
}
//# sourceMappingURL=KernelReplacerAuto.js.map