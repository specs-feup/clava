import laraGlobalTeardown from "@specs-feup/lara/jest/jestGlobalTeardown.ts";
import java from "java";

export default async function () {
  dumpJacocoCoverage();
  await laraGlobalTeardown();
}

function dumpJacocoCoverage(): void {
  if (process.env.CLAVA_JS_JAVA_OPTIONS === undefined) {
    return;
  }

  try {
    const JacocoRuntime = java.import("org.jacoco.agent.rt.RT");
    JacocoRuntime.getAgent().dump(false);
  } catch (error) {
    console.warn(`Could not dump Clava-JS JaCoCo coverage: ${error}`);
  }
}
