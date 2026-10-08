# Protobuf benchmark refresh

`refresh_benchmark.py` prepares an isolated source snapshot, collects the
Protobuf runtime and memory workloads, validates the captured cohort, and
renders the existing report. Collection never publishes. `publish` is a
separate command, requires `--private`, and reads the saved DraftLink back to
verify the exact HTML bytes.

## Prepare and collect

Prepare a new snapshot from the four current sibling worktrees. The command
checks the permitted local selector and Lara App-capture overlays, records
their hashes, sets the release selector only in the new Clava worktree, and
uses the verified published Linux x64 tool and protocol assets. It also stages
the checked-in workspace `package.json` so npm can resolve the Clava and Lara
workspaces outside the original checkout; the staged manifest hash is recorded.
The selected JavaScript tests come from checked-in workload fixtures whose
hashes match the frozen cohort. Preparation checks every current test file
against the pinned source policy, stages only those test files in the isolated
Clava worktree, and records each current and staged hash with both source
revisions. The production implementation and generated packages are built
from the current Clava snapshot.

```sh
python3 experiments/protobuf/suite/refresh/refresh_benchmark.py prepare \
  --workspace-root /home/lmsousa/Documents/Projects/SPeCS/ast-protobuf \
  --release-assets-root /home/lmsousa/.cache/protobuf-production-validation/build-final-20261008/clang-dumper/build \
  --output-root /home/lmsousa/.cache/protobuf-production-validation/snapshots/protobuf-20261008
```

Pass the resulting snapshot and the verified consumer `DUMPER_FOLDER` seed.
The default output is a new timestamped directory under
`~/.cache/protobuf-production-validation/benchmark-refreshes`; `--output-root`
can pin a path, but an existing directory is always rejected.

```sh
python3 experiments/protobuf/suite/refresh/refresh_benchmark.py collect \
  --snapshot /home/lmsousa/.cache/protobuf-production-validation/snapshots/protobuf-20261008 \
  --resource-cache /home/lmsousa/.cache/protobuf-production-validation/release-rc8/consumer-validation/tmp/clang_ast_exe_lmsousa/clang-dumper
```

The collector holds one nonblocking lock for the run. It saves complete
stdout logs, partial results, commands, cache seeds, runtime sidecars, memory
probe records, and failure status in the new run directory. A failed
preflight or timed cell stays there and prevents report rendering or DraftLink
updates. The preflight checks pinned Java and JavaScript test identity hashes,
JavaScript file order, App call counts, source counts, and context/workload
fingerprints against the accepted cohort before entering the timed matrix.

## Render and publish

Render saved, completed evidence again without running any workloads:

```sh
python3 experiments/protobuf/suite/refresh/refresh_benchmark.py render \
  --run-dir /path/to/completed-run
```

This validates the frozen control hash, all 48 Protobuf observations, source
and release hashes, the six memory JVMs and 120 cleanup phases, and unchanged
non-Protobuf control rows. It also checks cache results, exact suite counts,
the uninstrumented wall rows, Java's 512 MiB worker heap, JavaScript's default
heap and Vitest isolation settings, and the memory sidecars against their
capture record. It writes `report.html` in the run directory. Existing report
files are kept; repeated renders are idempotent only when the bytes match.
Pass `--output` to render to a separate path. The HTML keeps the current
boxplots, theme, summary, and download view.

To update the existing private DraftLink after reviewing the saved report:

```sh
python3 experiments/protobuf/suite/refresh/refresh_benchmark.py publish \
  --run-dir /path/to/completed-run \
  --draft-id DRAFT_ID \
  --private
```

The command renders from saved results, rejects local paths, private URLs, and
secret-like values in the HTML, runs `draftlink update` with `--private`, then
compares `draftlink read` byte-for-byte with the rendered page. Use
`publish --dry-run` to run all local validation without contacting DraftLink.
Each attempt has its own directory, so retries retain earlier publication
logs and readback failures. No command makes the report public.

`collect` captures fresh memory data with each runtime refresh: three new JVMs
per workload, 20 NAS+ cycles and 20 template cycles per JVM. This keeps every
published runtime result accompanied by separately dated, freshly captured
memory evidence from the same isolated snapshot.

## Fixed measurement contract

- Protobuf only. The frozen Text and FlatBuffers rows are never rerun.
- Java uses 116 pinned test identities and 216 App calls. The added
  `ClangFrameworkSearchTest` class is excluded by a run-local Gradle init file.
- JavaScript uses the frozen 164-test suite, 158 passes, 6 skips, 170 App
  calls, and 130 syntax-only calls. App and wall runs use the checked-in
  original file orders, `isolate: false`, no file parallelism, and one Vitest
  worker. Its nine selected test files are staged from pinned fixture bytes;
  unknown current or fixture hashes fail before collection.
- Each suite has direct, cold, and warm cache modes with four repetitions for
  App construction and separate uninstrumented suite wall time. The matrix
  contains 48 timed rows after identity and cache preflight.
- Java workers use a 512 MiB heap. JavaScript keeps its default JVM heap.
  Runtime App timing has no forced GC. The memory diagnostic alone explicitly
  requests GC and records cleanup. Wall commands do not load the App timing
  overlay or emit App metrics.
- Cold cache roots start empty. Warm measurements restore a verified seed.
  Direct mode disables ccache and checks that its wrapper is not invoked.
  Enabled modes set `CCACHE_NOCOMPRESS=true`.
- The selected release manifest, schema, descriptor, executable, consumer
  resolved manifest, runtime JARs, JDK, Node, npm, source revisions, overlays,
  and resource seed hashes are recorded with the capture.

Run the focused workflow and report tests with:

```sh
python3 -m unittest discover -s experiments/protobuf/suite/refresh -p 'test_*.py' -v
python3 -m unittest discover -s experiments/protobuf/report -p 'test_*.py' -v
```
