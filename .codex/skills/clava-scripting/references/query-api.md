# Query and Selector API

Use this when writing or debugging joinpoint selection logic.

## Primary sources

- Query API: `../lara/Lara-JS/api/weaver/Query.ts` (sibling worktree)
- Selector behavior: `../lara/Lara-JS/api/weaver/Selector.ts` (sibling worktree)
- Query usage tests: `Clava-JS/api/Query.test.ts`

If the Lara-JS repo is not a sibling of Clava, search for `Lara-JS/api/weaver/Query.ts`.

## Core patterns

- `Query.root()` returns the root joinpoint.
- `Query.search(Type, filter?, traversal?)` starts from root.
- `Query.searchFrom($base, Type?, filter?, traversal?)` searches below a base node (exclusive).
- `Query.searchFromInclusive($base, Type?, filter?, traversal?)` includes the base node.
- `Query.childrenFrom($base, Type?, filter?)` searches direct children.
- `Selector.scope(Type?, filter?)` searches inside the scope of the previously selected nodes.

## Filters

Filters accept:
- A string or regex applied to the default attribute for that joinpoint type.
- A predicate function `(jp) => boolean`.
- An object with attribute names as keys and values of string/regex/predicate.

Default attributes are defined in the joinpoint wrappers and can be resolved via `Weaver.getDefaultAttribute()`.

## Selector consumption

`Selector` is iterable and is consumed by `for..of`, `.get()`, `.first()`, and `.chain()`.
Use `.chain()` when you need the full chain map (e.g., `loop`, `loop_0`, `loop_1`).

## Minimal examples

```ts
for (const $fn of Query.search(FunctionJp, { isImplementation: true })) {
  // $fn is a joinpoint instance
}

const chains = Query.search(FunctionJp, "query_loop")
  .search(Loop)
  .search(Loop)
  .chain();
```
