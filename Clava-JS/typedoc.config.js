import { fileURLToPath } from "url";

export default {
  extends: [ fileURLToPath(import.meta.resolve("lara-js/typedoc.base.json")) ],
  entryPoints: ["src-api/"],
  tsconfig: "src-api/tsconfig.json",
}
