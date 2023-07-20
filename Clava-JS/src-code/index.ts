#!/usr/bin/env node
import WeaverLauncher from "lara-js/code/WeaverLauncher.js";
import { weaverConfig } from "./WeaverConfiguration.js";

const weaverLauncher = new WeaverLauncher(weaverConfig);

await weaverLauncher.execute();
