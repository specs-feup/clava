# Typed node storage performance investigation

The previous four-repeat check showed a Java warm-cache increase after introducing generated data bases.
The follow-up examined Java-worker JFR profiles, construction allocation, and the actual access paths.

## What caused overhead

The parent used `ListDataStore`, which resolves a key through cached indexes and accesses an array-backed list.
It did not allocate a hash map for every field. Typed getters therefore had a smaller advantage than a
comparison against a general map would suggest.

The initial compatibility layer added costs that direct getters could not remove:

- Parser construction called `computeIfAbsent` with a captured lambda on every node. The cache avoided rebuilding
  definitions, but it did not avoid allocating the function. JFR allocation samples exposed that site.
- Each generated base allocated a separate state and retained another storage reference.
- Generic presence checks, traversal and plain writes dispatched through field readers. Plain writes read the
  old value solely to construct the optional result of `setRaw`, although `set` never uses that result.
- Root ID, location and context remained in `ListDataStore`. Adding typed payload fields did not accelerate these
  common reads. Moving root fields into inherited storage also requires a direct root accessor; routing them
  through a concrete leaf's virtual reader reintroduces dispatch through the payload hierarchy.
- Factory creation and copying allocated full `ListDataStore` storage, then migrated values into typed state.
- A later profile exposed generic lookup on the typed getter's default-initialization path, especially for
  source locations. A declared field being typed did not remove that path. Normal construction also retained
  a state array, per-base class lookup and a virtual factory call despite having one physical state.

In the original matched Java warm-cache JFR workers, the new implementation sampled `NodeDataStore.read`
among the leading relevant CPU frames. Its callers included presence checks, traversal and plain setters.
The baseline and new worker GC pauses were both about 0.32 s. This supports investigating dispatch and
construction rather than attributing the loss to GC alone. JFR sample counts and allocation weights are
estimates, not exact elapsed-time attribution.

## Resulting implementation

A normal node now allocates one inherited typed state for root and payload fields, without a state array or
per-base class lookup. Generated getters read those fields directly. They initialize defaults directly in
the typed field, rather than falling back to generic lookup. Custom getter hooks are cached once because
DataKeys are immutable, so normal reads do not query key metadata. A presence bitmap handles generic presence checks without fetching field values;
fields beyond 64 use a lazily allocated extension. Construction metadata is cached as one immutable entry.

Generic reads and writes dispatch by the declaring layout through a generated dense switch to final
accessors. They do not dispatch through the concrete state's virtual methods or its inherited payload
switches. Root access uses final hand-written methods; ID, location, context and factory getters read the
same typed state directly. Plain writes avoid fetching an unused previous value. Factories and copies create
typed storage directly. Schema-bound scalar decoding writes by generated layout/index bindings and
root metadata initializes typed fields directly, without DataKey lookup. Deferred references retain their
existing generic fixup API.

`DataKey` names and the full public definition remain available. Defaults, custom getters, mutable-value
copy functions, null removal and virtual setter behavior are retained. Shared views reuse the original states;
a view that adds another branch allocates that branch without replacing existing references.

Validation covers 225 parser tests and coverage, three generator tests, the weaver build, and 162 JS tests
with two skipped. Regression tests check state sharing, presence updates and overflow, typed setters on
read-only views, direct defaults under strict mode, custom getter aliases, behavior-specific location overrides, and generic access against a state
whose virtual reader and writer throw.

## Diagnostic reproduction

Record Java-worker JFR profiles in isolated committed snapshots:

```sh
python clava/experiments/flatbuffers/data-bases-performance/run_benchmark.py \
  --baseline aa0946736 --current HEAD --suites java --modes warm --repeats 1 \
  --resource-seed /path/to/verified/cache/clang-dumper --profile --keep
```

Each Java run directory contains `worker.jfr`. These timings include profiling overhead and are kept separate
from the final performance comparison. Use `jfr print --json` with events `jdk.ExecutionSample`,
`jdk.ObjectAllocationSample` and `jdk.GarbageCollection` to inspect or aggregate the records.

`StorageProbe.java` measures construction allocations on the executing thread. Compile it against a snapshot's
`ClavaWeaver/build/install/ClavaWeaver/lib/*`, then run with that same classpath and `-Xmx512m`. The probe performs
40,000 warmup constructions and four rounds of 200,000 escaping constructions per class. All observations are
saved in `profiling-summary.json`. Construction-only timings are not predictions of whole App time.

The final timing comparison runs without JFR and retains the original fixed workload and cache gates. A
short Java-only warm-cache check is diagnostic; the complete direct/warm Java/JS comparison supplies the
reported end-to-end result. Interrupted or superseded candidate runs are not used in that comparison.

## Workload consistency failure

The first full comparison of direct parser writes stopped after ten valid observations. In the next Java
cell, the includes test's two calls reported `SHOW_CODE=false`, although preflight reported true. Every
other normalized setting and all source bytes matched. The failed record is retained separately and
supplies no final timing estimates.

This exposed a pre-existing parser configuration race. `CodeParser` uses `SimpleDataStore`, whose default
reads write into an unsynchronized `HashMap`. Parallel native workers share this parser. Concurrent
default insertion can lose an explicitly stored option during map resizing. A standalone eight-worker
probe reproduced 13 losses of an explicit `SHOW_CODE=true` setting in 2,000 fresh configurations.
Initializing defaults first reproduced zero losses with the same probe.

`ParallelCodeParser` now initializes declared defaults before starting its executor. The benchmark
overlay applies the same initialization at the same boundary to the older parent control. It retains
`SHOW_CODE` in the workload fingerprint and all original workload gates. This additional control is
recorded explicitly in the final raw results. It changes the comparison from unmodified parent parsing
to parent parsing with this shared correctness fix. `SHOW_CODE` printing is outside the App timer,
but losing configuration makes a run invalid regardless of where the affected work is timed.

## Final matched outcome

The complete comparison of `aa0946736` with `9182af4ca` passed all 32 measured cells.
All normalized workload fingerprints matched across revisions, modes and rounds, and no parsed source
contents differed. Both revisions use the same parser-default initialization correction.

| App time, median of four runs | Parent with parser fix | Typed fields with parser fix | Change |
| --- | ---: | ---: | ---: |
| JS direct | 7.803 s | 7.967 s | +2.10% |
| JS warm | 4.483 s | 4.581 s | +2.19% |
| Java direct | 21.649 s | 21.626 s | -0.10% |
| Java warm | 12.008 s | 12.005 s | -0.03% |

Java is essentially unchanged in this workload. JS remains slower in every paired observation,
with median increases of 2.10% direct and 2.19% warm. The no-degradation performance gate is still unmet.
Four runs are descriptive and do not establish precise statistical confidence or an end-to-end gain.

Steady construction allocation fell from 864 to 688 bytes for `BuiltinType`, and from 1160 to 784 bytes
for `VarDecl`, reductions of 20.4% and 32.4%. These are empty-node construction measurements, not total
App allocation or process RSS. The typed model has a measured storage benefit. That does not imply
a matching throughput gain for a workload that retains generic access and includes native execution,
wire decoding and AST transformations.

`stable-defaults-results.json` preserves the final raw record. `comparison.json` links it by SHA-256
and retains every sample; `comparison.html` shows all samples alongside the construction allocation
measurements. Earlier candidate results are retained separately and supply no final estimates.

## Remaining JS cost

A separate matched warm-cache JS JFR diagnostic reused the final snapshots and the same parser correction.
The current JVM recorded 86 generated base/state class-load events; the control recorded none. Their
inclusive duration sum was about 29 ms. These events can nest, so that sum is not exclusive elapsed
time and cannot be subtracted directly from the App difference. The generated hierarchy adds startup
work even when steady field reads and construction allocation improve.

Generic dispatch and copying still occur through the compatibility API. The profile retained only
31 control and 40 current CPU samples with a `ParallelCodeParser.parse` frame. It supports identifying
remaining costs, but cannot precisely apportion the roughly 100 ms warm-cache App difference. CPU
samples across the whole JS worker also include code generation, test work and copying outside the
App timer. The remaining JS regression is measured; it is not fully attributed by this diagnostic.
The final performance figures come solely from the separate unprofiled four-round matrix.
