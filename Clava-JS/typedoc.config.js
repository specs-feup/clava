import { fileURLToPath } from "url";

export default {
  extends: [
    fileURLToPath(import.meta.resolve("@specs-feup/lara/typedoc.base.json")),
  ],
  entryPoints: ["src-api/"],
  tsconfig: "src-api/tsconfig.json",
};
