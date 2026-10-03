# Clava-JS eager FlatBuffers workload

`run_suite.py` stages a built ClavaWeaver distribution and runs the fixed
Vitest selection from `Clava-JS`. There is no wire-format switch. Each run
records the packaged release tag, local schema/tool identities when applicable,
runtime JAR hashes, repository state, cache counters, host load, and GNU-time
metrics.

The test-name filter keeps the historical workload at 164 tests: 158 passed,
zero failed, and six pending. It excludes the same four host-dependent
OpenMP/CUDA tests from every observation. A run fails when this population or
the eager protocol contract changes unexpectedly.

Run the three cache states sequentially, with independent cold caches paired
to their warm observations and cache bypass still exercising the production
file path:

```sh
python3 experiments/flatbuffers/suite/run_matrix.py \
  --runtime-root ClavaWeaver/build/install/ClavaWeaver --repeat-count 3
```

The matrix sets `CCACHE_DISABLE=true` for bypass and requires zero ccache calls
there; cold and warm runs require misses and restored hits, respectively. Do
not run build or benchmark jobs at the same time. Reports are written beneath
the ignored `experiments/flatbuffers/suite/results/` directory.
