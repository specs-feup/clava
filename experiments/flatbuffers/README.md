# FlatBuffers release validation

Clava's production parser consumes the release-selected eager FlatBuffers
stream. The checked-in scripts here exercise that ordinary parser path; they do
not select a wire format or substitute a text parser. Text and Protobuf runs are
historical comparison controls and must use isolated, source-identified builds.

## Validation commands

Build Clava from its normal Gradle project, then run the production JavaScript
suite and the parser integration probes:

```sh
cd /home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers/clava
gradle -p ClavaWeaver --no-daemon installDist
python3 experiments/flatbuffers/suite/run_matrix.py \
  --runtime-root ClavaWeaver/build/install/ClavaWeaver --repeat-count 3
python3 experiments/flatbuffers/validation/run_java_suite.py --offline
python3 experiments/flatbuffers/validation/run_correctness.py
```

The Vitest workload contains 164 tests: 158 expected passes and six pending.
Four host-dependent OpenMP/CUDA failures are excluded by the same fixed test
filter in every cache state. The Java comparison workload is the established
116 parser tests; new wire, resource, dumper, and harness tests are excluded
from that matched workload and should also run in the ordinary full Gradle
test task.

The C/C++ corpus check reuses the fixed 5,000-file LLVM 18.1.8 candidate list
and its independently recorded clean-text classifications. It reruns the
1,062 clean cases through the selected native release, requires the matching
`verify_flatbuffers` executable to accept every stream, and emits a consumer
input manifest. The consumer comparison processes that manifest in one JVM
per runtime and compares generated-code hashes between eager and fresh Text
controls:

```sh
CORPUS=/home/lmsousa/.cache/ast-flatbuffers-release-validation/llvm-project/clang/test
BASELINE=/home/lmsousa/.cache/ast-flatbuffers-release-validation/text-corpus-5000/results.json
EAGER="$PWD/ClavaWeaver/build/install/ClavaWeaver"
TEXT=/home/lmsousa/.cache/ast-flatbuffers-release-validation/text-build/clava/ClavaWeaver/build/install/ClavaWeaver
PROTOBUF=/home/lmsousa/.cache/ast-flatbuffers-release-validation/protobuf-build/clava/ClavaWeaver/build/install/ClavaWeaver
python3 experiments/flatbuffers/validation/run_binary_corpus.py \
  --tool ../clang-dumper/build/tool --verifier ../clang-dumper/build/verify_flatbuffers \
  --baseline-results "$BASELINE" --corpus "$CORPUS" \
  --output-root experiments/flatbuffers/results/validation/eager-corpus
python3 experiments/flatbuffers/validation/run_corpus_consumer.py \
  --runtime "eager=$EAGER" --runtime "text=$TEXT" --runtime "protobuf=$PROTOBUF" \
  --manifest experiments/flatbuffers/results/validation/eager-corpus/consumer-inputs.json \
  --output-root experiments/flatbuffers/results/validation/consumer-corpus
```

`run_correctness.py` separately checks C and C++ parse-generate-reparse byte
stability and a two-translation-unit call linked to its provider definition.

Every run records its repository revisions and dirty state, runtime JAR hashes,
release tag, schema bundle and entrypoint hashes, native tool hash, host load
and memory, cache state, test counts, and timing boundary. Generated run data
stays under the ignored `results/` directories. Commit durable summaries only
when they are needed as release evidence.

## Memory gate

`validation/run_memory_matrix.py` launches one isolated JVM per observation,
uses 20 repeated parse/collect cycles by default, with the same fixed GC budget
for every runtime, and records kernel peak JVM
RSS (Linux VmHWM) and post-GC retained heap. GNU time also records the
process-tree maximum single-process RSS; that secondary number may include a native child. It records mapped files and temporary folders
for all controls; the eager run must collect every AST and leave no mappings or
temporary Clang directories. It accepts prebuilt runtime directories, so eager,
Text, and Protobuf observations run through the same probe without a wire-format
switch. Run it once for each of the NAS and templates workloads:

```sh
python3 experiments/flatbuffers/validation/run_memory_matrix.py \
  --runtime "eager=$EAGER" --runtime "text=$TEXT" --runtime "protobuf=$PROTOBUF" \
  --source /path/to/nas.c --repeat-count 3 --parse-repeats 20
python3 experiments/flatbuffers/validation/run_memory_matrix.py \
  --runtime "eager=$EAGER" --runtime "text=$TEXT" --runtime "protobuf=$PROTOBUF" \
  --source /path/to/templates.cpp --repeat-count 3 --parse-repeats 20
```

Build Text and Protobuf controls in isolated worktrees and record their source
revisions before including them. Each run captures host load, available memory,
and top CPU processes before and after the JVM. Keep the observations sequential
and do not overlap them with builds or benchmarks.

Historical prototype sources and their reports were kept on the
`lazy-flatbuffers-experiment` branch. Snapshots under `measurements/` are
historical results and are not current-build performance evidence. Current
release claims must cite runs made from the selected release and its exact
consumer build.
