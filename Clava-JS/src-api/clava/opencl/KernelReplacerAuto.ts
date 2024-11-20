import Io from "@specs-feup/lara/api/lara/Io.js";
import Strings from "@specs-feup/lara/api/lara/Strings.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import { Call, FileJp, Pragma } from "../../Joinpoints.js";
import KernelReplacer, {
  OpenClKernelReplacerConfiguration,
} from "./KernelReplacer.js";

//	This aspect can be included in a library, imported and
// called by a user, since it needs no configuration/parameterization
export default function KernelReplacerAuto() {
  // look for pragma
  for (const $pragma of Query.search(Pragma, "clava")) {
    const $file = $pragma.target.getAncestor("file") as FileJp;

    if (!$pragma.content.startsWith("opencl_call")) {
      continue;
    }

    const configFilename = Strings.extractValue(
      "opencl_call",
      $pragma.content
    )?.trim();
    const configFile = Io.getAbsolutePath($file.path, configFilename);

    if (!Io.isFile(configFile)) {
      console.log(
        `Expected to find the config file '${configFilename}' in the folder '${$file.path}'`
      );
      continue;
    }

    const config = Io.readJson(configFile) as OpenClKernelReplacerConfiguration;

    const $call = $pragma.target.getDescendants("call")[0] as Call;
    const kernel = new KernelReplacer(
      $call,
      config.kernelName,
      config.kernelFile,
      config.bufferSizes,
      config.localSize,
      config.iterNumbers
    );

    for (const outBuf of config.outputBuffers) {
      kernel.setOutput(outBuf);
    }

    kernel.replaceCall();
  }
}
