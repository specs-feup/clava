# Clava and Lara API Entry Points

Use this to locate the right TypeScript APIs and understand where functionality lives.

## Clava-JS APIs (this repo)

- Joinpoint wrappers (generated): `Clava-JS/api/Joinpoints.ts`
- Joinpoint factories/utilities: `Clava-JS/api/clava/ClavaJoinPoints.ts`
- Core Clava utilities and AST stack: `Clava-JS/api/clava/Clava.ts`
- Common passes/opts built on Query: `Clava-JS/api/clava/opt`, `Clava-JS/api/clava/pass`

Imports typically use:
- `@specs-feup/clava/api/Joinpoints.ts`
- `@specs-feup/clava/api/clava/ClavaJoinPoints.ts`
- `@specs-feup/clava/api/clava/Clava.ts`

## Lara-JS APIs (sibling repo)

- Query API: `../lara/Lara-JS/api/weaver/Query.ts`
- Selector behavior and filters: `../lara/Lara-JS/api/weaver/Selector.ts`
- Weaver utilities: `../lara/Lara-JS/api/weaver/Weaver.ts`

If the Lara-JS repo is not a sibling of Clava, search for `Lara-JS/api/weaver/Query.ts`.

Imports typically use:
- `@specs-feup/lara/api/weaver/Query.ts`
- `@specs-feup/lara/api/weaver/Weaver.ts`

## Notes

- Joinpoint wrappers expose attributes and methods specific to each type.
- `ClavaJoinPoints` provides factory helpers for types, statements, expressions, and declarations.
- Use `.code` on joinpoints (or `Query.root().code`) to inspect generated code quickly.
