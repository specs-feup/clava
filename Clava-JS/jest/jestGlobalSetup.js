import { Weaver } from "lara-js/code/Weaver.js";
import { weaverConfig } from "../code/WeaverConfiguration.js";

async function oneTimeSetup() {
  const weaverMessageFromLauncher = {
    args: {
      _: [],
      $0: "",
    },
    config: weaverConfig,
  };

  await Weaver.setupWeaver(
    weaverMessageFromLauncher.args,
    weaverMessageFromLauncher.config
  );
}

let setupDone = false;

export default async function () {
  if (!setupDone) {
    await oneTimeSetup();
    setupDone = true;
  }
  Weaver.start();
}
