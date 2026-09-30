# Standalone per-source Text/Protobuf benchmark

This runner measures one `CodeParser.parse(List.of(source), compiler_options)`
call per captured event. It is a one-file-at-a-time benchmark, not a replay of the
complete 191-event Clava-JS or 247-event Java suite timings.

Inputs remain at the absolute paths recorded during capture. The runner preserves
the captured source path, compiler options, working directory, and generated
parse root. It does not recreate a private `rootfs`, copy or hardlink input
blobs, rewrite include paths, or create missing captured directories. The
content-addressed files in the corpus remain useful for auditing the capture,
but the runner never uses them as parser inputs.

Before writing a pilot schedule or starting Java, preflight checks every source
and dependency against its captured SHA-256. It rejects different snapshots for
the same original path and requires the captured working and generated parse
directories to still exist. The runner repeats the hash and directory checks
before and after each Java batch and after the benchmark. These checks run
outside the parser timer. A missing or changed input requires a fresh valid
capture with live paths retained; the runner will not restore one from a blob.
New run metadata marks this contract as `input_mode=original-paths` and reports
`original_inputs_unchanged`. The legacy report renderer assumes relocated input
snapshots, so it must become mode-aware before it consumes results from this
runner.

The `standalone-corpus-20260930` archive cannot be replayed in place under these
rules: its 438 events reference 11 missing paths and two paths with conflicting
captured hashes. Its recorded inputs and historical result files remain useful
as audit evidence, but this runner cannot benchmark that corpus as it stands.
The capture flow records dependencies before temporary cleanup, but does not
currently offer an option to retain all generated source roots or avoid reusing
one path for different contents. A fresh in-place run therefore needs a capture
that preserves those generated inputs at unique, stable paths.

Use `--pilot` to write the small representative schedule, then `--run` only
after the pilot has been validated. Both modes require the original captured
inputs and directories to pass preflight.
