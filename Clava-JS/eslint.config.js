import typescriptEslint from "typescript-eslint";
import tsdoc from "eslint-plugin-tsdoc";
import js from "@eslint/js";
import eslintConfigPrettier from "eslint-config-prettier";

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
];
