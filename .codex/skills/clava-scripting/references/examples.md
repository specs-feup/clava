# Script Examples and Patterns

Use these files for concrete patterns and idioms.

## Weaver tests (JS, but patterns apply to TS)

- `ClavaWeaver/resources/clava/test/weaver/Function2.js`
  - Select function, clone it, change return type, replace body, add param.

- `ClavaWeaver/resources/clava/test/weaver/Clone.js`
  - Clone all functions with definitions and print file code.

- `ClavaWeaver/resources/clava/test/weaver/Field.js`
  - Navigate record fields and read attributes like `isPublic`.

- `ClavaWeaver/resources/clava/test/issues/Issue168.js`
  - Normalize loops and decompose statements using `NormalizeToSubset` and `StatementDecomposer`.

- `ClavaWeaver/resources/clava/test/issues/Issue_aiq_1.js`
  - Filter loops by kind, inspect condition relation.

## API tests

- `ClavaWeaver/resources/clava/test/api/ClavaJoinPointsTest.js`
  - Large catalog of `ClavaJoinPoints` factory helpers.

- `Clava-JS/src-api/Query.test.ts`
  - Query chaining, `.scope()`, `.chain()`, and regex selection.
