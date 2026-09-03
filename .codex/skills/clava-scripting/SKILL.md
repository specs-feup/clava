---
name: clava-scripting
description: Create, update, and explain Clava/LARA scripts in TypeScript using Clava-JS and Lara-JS APIs, including Query/Selector usage, joinpoint selection and filters, and AST transformations. Use for Clava script authoring, joinpoint queries, or refactoring code via Clava/Lara weaver APIs.
---

# Clava Scripting

## Overview

Write and modify Clava scripts in TypeScript using Clava/Lara APIs for joinpoint selection and AST transformations.

## Quick Start

Use ESM imports with `.js` extensions, select joinpoints with `Query`, and transform with Clava APIs.

```ts
import Query from "@specs-feup/lara/api/weaver/Query.js";
import { FunctionJp } from "@specs-feup/clava/api/Joinpoints.js";

const $fn = Query.search(FunctionJp, { isImplementation: true }).first();
if ($fn) $fn.clone(`${$fn.name}_clone`);
```

## Workflow

1. Identify joinpoints and attributes.
Use the generated joinpoint wrappers in `@specs-feup/clava/api/Joinpoints.js` and check `Joinpoints.ts` for default attributes and available fields.

2. Select joinpoints with Query/Selector.
Use `Query.search`, `Query.searchFrom`, `Query.searchFromInclusive`, `Query.childrenFrom`, and `Selector.scope`. Filters accept strings, regex, predicate functions, or objects keyed by attributes. `Selector` is iterable and methods like `.get()`, `.first()`, and `.chain()` consume the current selection.

3. Transform and emit code.
Use joinpoint methods like `.clone()`, `.replaceWith()`, `.addParam()`, `.setReturnType()`, and factories in `ClavaJoinPoints` for new nodes. Use `Query.root().code` or `Clava.writeCode()` to inspect or emit output.

## References

- `references/query-api.md` for Query/Selector behavior and filters.
- `references/clava-apis.md` for key Clava/Lara API entry points and file locations.
- `references/examples.md` for real scripts and test patterns.
