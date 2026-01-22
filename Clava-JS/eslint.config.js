import { fileURLToPath } from "url";
import { dirname } from "path";
import typescriptEslint from "typescript-eslint";
import tsdoc from "eslint-plugin-tsdoc";
import jest from "eslint-plugin-jest";
import js from "@eslint/js";
import eslintConfigPrettier from "eslint-config-prettier";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default [
  js.configs.recommended,
  eslintConfigPrettier,
  ...typescriptEslint.configs.recommended,
  {
    ignores: ["**/*.d.ts", "**/*.config.js"],
  },
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
        project: ["./*/tsconfig.json", "./tsconfig.*.json"],
        tsconfigRootDir: __dirname,
      },
    },

    rules: {
      "tsdoc/syntax": "warn",
    },
  },
  {
    ...typescriptEslint.configs.disableTypeChecked,
    files: ["scripts/**/*.js"],
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
];
