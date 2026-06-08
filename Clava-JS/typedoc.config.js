import { fileURLToPath } from "url";

export default {
  extends: [
    fileURLToPath(import.meta.resolve("@specs-feup/lara/typedoc.base.json")),
  ],
  entryPoints: ["api/"],
  tsconfig: "api/tsconfig.json",
};
