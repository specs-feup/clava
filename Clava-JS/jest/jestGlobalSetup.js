import { Weaver, setupWeaver } from "lara-js/code/Weaver.js";
import { weaverConfig } from "../code/WeaverConfiguration.js";

export default async function () {
  const weaverMessageFromLauncher = {
    args: {
      _: [],
      $0: "",
    },
    config: weaverConfig,
  };

  setupWeaver(weaverMessageFromLauncher);
  await Weaver.awaitSetup();
}
