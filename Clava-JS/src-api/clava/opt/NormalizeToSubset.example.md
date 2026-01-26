# NormalizeToSubset Usage Examples

## Basic Usage

```typescript
import NormalizeToSubset from "./NormalizeToSubset.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

// Apply normalization to the entire program
NormalizeToSubset(Query.root());
```

## With Options

### Using Global IDs (Recommended for Multiple Normalizations)

When you need to apply normalization multiple times over the same AST region, use the `useGlobalIds` option to prevent duplicate symbol generation:

```typescript
import NormalizeToSubset from "./NormalizeToSubset.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import { FunctionJp } from "../../Joinpoints.js";

const functionJp = Query.search(FunctionJp, { name: "myFunction" }).first();

// Apply normalization with global ID generation
NormalizeToSubset(functionJp, { useGlobalIds: true });

// Safe to apply again - will not create duplicate symbols
NormalizeToSubset(functionJp, { useGlobalIds: true });
```

### Custom Loop Simplification

```typescript
import NormalizeToSubset from "./NormalizeToSubset.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

// Customize loop simplification behavior
NormalizeToSubset(Query.root(), {
  simplifyLoops: { forToWhile: false },
  useGlobalIds: true
});
```

## Symbol Table Management

The StatementDecomposer automatically builds a symbol table of existing variable declarations and parameters before generating new variable names. This prevents conflicts with existing `decomp_N` variables in your code.

### Example

Given this code:
```c
int foo(int a) {
  int decomp_0 = 5;  // Existing variable
  int result = (a + 1) * (2 + 3);
  return result;
}
```

When normalized, the transformation will create `decomp_1`, `decomp_2`, etc., skipping `decomp_0` to avoid conflicts.

## Implementation Details

- **Symbol Table**: Collects all parameter and variable declaration names from the enclosing function
- **Local Counter**: By default, uses a local counter that checks against the symbol table
- **Global ID Generator**: When `useGlobalIds: true`, uses `IdGenerator.next("decomp_")` for globally unique names across the entire transformation session
