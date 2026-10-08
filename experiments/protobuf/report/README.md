# Updated Protobuf benchmark report

`render_updated_benchmark.py` writes a standalone HTML page from the frozen
comparison evidence and the later Protobuf-only measurement session. It keeps
the existing App and wall-time chart implementations, removes only the older
Protobuf rows, and leaves the Text, FlatBuffers, and before-cache rows intact.

Render the archived October 8 comparison from the frozen controls, completed
Protobuf results, and matching release manifest:

```sh
python3 experiments/protobuf/report/render_updated_benchmark.py \
  --prior-evidence experiments/protobuf/validation/evidence/benchmark-20261008/frozen-controls-20261007.json \
  --updated-results experiments/protobuf/validation/evidence/benchmark-20261008/updated-protobuf-results.json \
  --release-manifest experiments/protobuf/validation/evidence/benchmark-20261008/release-manifest.json \
  --output /tmp/protobuf-updated-report-20261008.html
```

Pass two `--memory LABEL=PATH` options to add the separate NAS+ and C++ template
memory probes. The captured diagnostic inputs are also archived in the
validation evidence folder:

```sh
python3 experiments/protobuf/report/render_updated_benchmark.py \
  --prior-evidence experiments/protobuf/validation/evidence/benchmark-20261008/frozen-controls-20261007.json \
  --updated-results experiments/protobuf/validation/evidence/benchmark-20261008/updated-protobuf-results.json \
  --release-manifest experiments/protobuf/validation/evidence/benchmark-20261008/release-manifest.json \
  --memory 'NAS+=experiments/protobuf/validation/evidence/memory-nas-20261007.json' \
  --memory 'C++ templates=experiments/protobuf/validation/evidence/memory-templates-20261007.json' \
  --output /tmp/protobuf-updated-report-20261008.html
```

The report validates the 48 updated observations and the four-repeat chart
cells before rendering. It checks all 120 memory phases for App collection and
parser resource cleanup, then reports each JVM repeat's first and last retained
heap readings, range, fitted slope, and separate JVM and GNU time RSS values.
The optional release manifest is matched by SHA-256 to the selected measured
release and its schema, descriptor, and tool hashes are cross-checked against
the protocol assets and measured native executable. The report displays source
revisions and each measurement session's dates independently. It does not
calculate paired changes between measurements collected in different sessions.

The HTML and CSV download contain measurement summaries, timestamps, and
counts. They do not include input filenames, local paths, commands, raw logs, or
private URLs.

The stylesheet uses light colors by default and dark colors when the host adds
`dark` to the root `<html>` element. It adds no theme control and does not read
the system color preference.

Run the renderer tests with:

```sh
python3 -m unittest discover -s experiments/protobuf/report -p 'test_*.py' -v
```
