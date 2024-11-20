#!/usr/bin/env node
import WeaverLauncher from "@specs-feup/lara/code/WeaverLauncher.js";
import { weaverConfig } from "./WeaverConfiguration.js";

const weaverLauncher = new WeaverLauncher(weaverConfig);

await weaverLauncher.execute();
