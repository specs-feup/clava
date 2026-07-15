import js from "@eslint/js";
import eslintConfigPrettier from "eslint-config-prettier";
import jest from "eslint-plugin-jest";
import tsdoc from "eslint-plugin-tsdoc";
import { defineConfig } from "eslint/config";
import { dirname } from "path";
import typescriptEslint from "typescript-eslint";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default defineConfig([
  js.configs.recommended,
  eslintConfigPrettier,
  ...typescriptEslint.configs.recommended,
  {
    plugins: {
      "@typescript-eslint": typescriptEslint.plugin,
      tsdoc,
    },

    languageOptions: {
      parser: typescriptEslint.parser,
      ecmaVersion: 5,
      sourceType: "script",

      parserOptions: {
        project: ["./tsconfig.json", "./tsconfig.*.json"],
        tsconfigRootDir: __dirname,
      },
    },

    rules: {
      "tsdoc/syntax": "warn",
    },
  },
  {
    ...jest.configs["flat/recommended"],
    files: ["**/*.spec.ts", "**/*.test.ts"],

    plugins: {
      jest,
    },

    languageOptions: {
      globals: {
        ...jest.environments.globals.globals,
      },
    },
  },
]);
