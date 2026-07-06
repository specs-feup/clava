import laraGlobalSetup from "@specs-feup/lara/jest/jestGlobalSetup.ts";
import java from "java";

export default async function (...args: Parameters<typeof laraGlobalSetup>) {
  applyJavaOptions();
  await laraGlobalSetup(...args);
}

function applyJavaOptions(): void {
  const rawJavaOptions = process.env.CLAVA_JS_JAVA_OPTIONS;

  if (rawJavaOptions === undefined || rawJavaOptions.trim() === "") {
    return;
  }

  const javaOptions = parseJavaOptions(rawJavaOptions);

  for (const javaOption of javaOptions) {
    if (!java.options.includes(javaOption)) {
      java.options.push(javaOption);
    }
  }
}

function parseJavaOptions(rawJavaOptions: string): string[] {
  try {
    const parsed = JSON.parse(rawJavaOptions);

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
