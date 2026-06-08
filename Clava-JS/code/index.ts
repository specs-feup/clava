#!/usr/bin/env node
import WeaverLauncher from "@specs-feup/lara/code/WeaverLauncher.ts";
import { weaverConfig } from "./WeaverConfiguration.ts";

const weaverLauncher = new WeaverLauncher(weaverConfig);

await weaverLauncher.execute();
