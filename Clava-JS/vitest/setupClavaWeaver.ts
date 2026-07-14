import { weaverConfig } from "../code/WeaverConfiguration.ts";
import { setupWeaver } from "@specs-feup/lara/vitest/setupWeaver.ts";
import java from "java";

applyJavaOptions();
setupWeaver(weaverConfig);

function applyJavaOptions(): void {
  const rawJavaOptions = process.env.CLAVA_JS_JAVA_OPTIONS;

  if (rawJavaOptions === undefined || rawJavaOptions.trim() === "") {
    return;
  }

  for (const javaOption of parseJavaOptions(rawJavaOptions)) {
    if (!java.options.includes(javaOption)) {
      java.options.push(javaOption);
    }
  }
}

function parseJavaOptions(rawJavaOptions: string): string[] {
  try {
    const parsed: unknown = JSON.parse(rawJavaOptions);

    if (
      Array.isArray(parsed) &&
      parsed.every((javaOption) => typeof javaOption === "string")
    ) {
      return parsed;
    }
  } catch {
    // Fall back to whitespace splitting for ad-hoc local use.
  }

  return rawJavaOptions.split(/\s+/).filter((javaOption) => javaOption !== "");
}
