#!/usr/bin/env python3
"""
Same-session Text + ccache, Protobuf and FlatBuffers benchmark.

Each stage is a committed Clava revision with its own clang-dumper release. All stages share one
specs-java-libs and lara-framework revision and run the same pinned Java and JavaScript workloads, so
only the AST transport differs. Rounds rotate stage, cache-mode and suite order as in the original
protocol comparison.

    three_way.py run [--quick] [--draft-id ID]

`--quick` measures one App-time repeat in direct and warm modes; it is never published.
"""

from __future__ import annotations

import argparse
import collections
import dataclasses
import datetime as dt
import getpass
import importlib.util
import inspect
import json
import os
from pathlib import Path
import re
import shutil
import statistics
import subprocess
import sys
import time
from typing import Any, Callable
from urllib.parse import unquote, urlparse

sys.dont_write_bytecode = True

import refresh_benchmark as refresh
import support
from prepare_snapshot import DIAGNOSTICS_FALSE, DIAGNOSTICS_PATH, DIAGNOSTICS_TRUE, _link_node_modules

REFRESH_DIR = Path(__file__).resolve().parent
WORKSPACE = refresh.DEFAULT_WORKSPACE
FLATBUFFERS_WORKSPACE = WORKSPACE.parent / "ast-flatbuffers"
PROTOCOL_COMPARISON = WORKSPACE / "clava/experiments/protocol-comparison"
VALIDATION = Path.home() / ".cache/protobuf-production-validation"
OUTPUT_BASE = VALIDATION / "three-way"
SUITES = ("clava-js", "java")
MODES = ("direct", "cold", "warm")
JAVA_GENERATED = "ClangAstParser/build/generated"


def _constants(path: Path) -> dict[str, str]:
    """Reads `public static final` literals from a generated Java source."""
    return dict(re.findall(r'static final \w+ (\w+) = "?([^";]+)"?;', path.read_text()))


def _protobuf_namespace(clava: Path) -> str:
    toolchain = _constants(clava / JAVA_GENERATED / "source/proto-toolchain/main/pt/up/fe/specs/clang/wire/ProtoToolchain.java")
    schema = _constants(clava / JAVA_GENERATED / "source/proto-schema/main/pt/up/fe/specs/clang/wire/ProtoSchemaHash.java")
    identity = "-".join(toolchain[name] for name in (
        "NATIVE_PROTOC_VERSION", "NATIVE_PROTOBUF_VERSION", "JAVA_PROTOC_VERSION", "JAVA_PROTOBUF_VERSION",
        "DESCRIPTOR_SHA256"))
    return f"clang-dumper-protobuf-ccache-v1/protobuf-v1-{identity}-{schema['VALUE']}"


def _flatbuffers_namespace(clava: Path) -> str:
    protocol = _constants(clava / "ClangAstParser/src/pt/up/fe/specs/clang/wire/WireProtocol.java")
    nodes = _constants(clava / JAVA_GENERATED / "sources/wire/pt/up/fe/specs/clang/wire/GeneratedNodes.java")
    return (f"clang-dumper-ccache/flatbuffers-v{protocol['SCHEMA_VERSION']}-{protocol['FLATBUFFERS_VERSION']}-"
            f"{protocol['FLATBUFFERS_COMMIT']}-{nodes['SCHEMA_HASH']}")


@dataclasses.dataclass(frozen=True)
class StageSpec:
    """A measured transport: Clava revision, wire mode, dumper release and its ccache folder."""
    key: str
    repository: Path
    ref: str
    wire: str
    release_tag: str | None  # None keeps the revision's committed (local build) selector
    resource_seed: Path | None  # DUMPER_FOLDER/clang-dumper contents holding releases/<tag>
    namespace: Callable[[Path], str]


STAGES = (
    StageSpec("ccache-text", WORKSPACE / "clava", "text-ccache-baseline", "text", None, None,
              lambda clava: "clang-dumper-ccache"),
    StageSpec("protobuf", WORKSPACE / "clava", "ast-protobuf", "protobuf", "v18.1.8_6-rc8",
              refresh.DEFAULT_RESOURCE_CACHE, _protobuf_namespace),
    StageSpec("flatbuffers", FLATBUFFERS_WORKSPACE / "clava", "ast-flatbuffers", "flat-eager", "v18.1.8_5-rc3",
              VALIDATION / "seeds/fb-rc3/clang-dumper", _flatbuffers_namespace),
)


def git(*args: str, cwd: Path) -> str:
    return subprocess.run(["git", *args], cwd=cwd, check=True, capture_output=True, text=True).stdout.strip()


# ---------------------------------------------------------------------------------------------------------------
# Snapshot and build


def prepare(output: Path) -> dict[str, Any]:
    """Creates shared library worktrees and one Clava worktree per stage under `output`."""
    output.mkdir(parents=True)
    shared = output / "shared"
    identity: dict[str, Any] = {"created_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "shared": {}, "stages": {}}
    for name in ("specs-java-libs", "lara-framework"):
        revision = git("rev-parse", "HEAD", cwd=WORKSPACE / name)
        git("worktree", "add", "--detach", str(shared / name), revision, cwd=WORKSPACE / name)
        identity["shared"][name] = revision
    # Lara-JS builds from the shared tree, so it needs the workspace dependencies above it too.
    shutil.copy2(WORKSPACE / "package.json", shared / "package.json")
    _link_node_modules(WORKSPACE / "node_modules", shared / "node_modules", WORKSPACE, shared)
    _link_node_modules(WORKSPACE / "lara-framework/Lara-JS/node_modules",
                       shared / "lara-framework/Lara-JS/node_modules", WORKSPACE, shared)

    fixtures = REFRESH_DIR / "workload-fixtures/clava-js"
    for spec in STAGES:
        root = output / "stages" / spec.key
        root.mkdir(parents=True)
        revision = git("rev-parse", spec.ref, cwd=spec.repository)
        git("worktree", "add", "--detach", str(root / "clava"), revision, cwd=spec.repository)
        for name in ("specs-java-libs", "lara-framework"):
            (root / name).symlink_to(shared / name, target_is_directory=True)
        shutil.copy2(WORKSPACE / "package.json", root / "package.json")
        _link_node_modules(WORKSPACE / "node_modules", root / "node_modules", WORKSPACE, root)
        _link_node_modules(WORKSPACE / "clava/Clava-JS/node_modules", root / "clava/Clava-JS/node_modules",
                           WORKSPACE, root)

        clava = root / "clava"
        tag_file = clava / "ClangAstParser/clang-dumper-release.tag"
        if spec.release_tag is not None:
            tag_file.write_text(spec.release_tag + "\n")
        diagnostics = clava / DIAGNOSTICS_PATH
        diagnostics.write_text(diagnostics.read_text().replace(DIAGNOSTICS_TRUE, DIAGNOSTICS_FALSE, 1))
        # Every stage runs the same pinned JavaScript test bytes.
        for fixture in fixtures.rglob("*.ts"):
            shutil.copy2(fixture, clava / "Clava-JS" / fixture.relative_to(fixtures))
        identity["stages"][spec.key] = {"repository": str(spec.repository), "ref": spec.ref, "revision": revision,
                                        "selector": tag_file.read_text().strip(), "wire": spec.wire}
    (output / "snapshot-identity.json").write_text(json.dumps(identity, indent=2) + "\n")
    return identity


def build(output: Path, java_init: Path) -> None:
    """Emits the JavaScript packages and Java runtimes before any timed command."""
    logs = output / "build-logs"
    logs.mkdir(exist_ok=True)

    def run(name: str, command: list[str], cwd: Path, env: dict[str, str]) -> None:
        with (logs / f"{name}.log").open("w") as log:
            subprocess.run(command, cwd=cwd, env=env, stdout=log, stderr=subprocess.STDOUT, check=True)

    for index, spec in enumerate(STAGES):
        root = output / "stages" / spec.key
        # Resolved paths, as the measured commands use: Gradle rejects one included build seen through two paths.
        env = {**os.environ, "SPECS_JAVA_LIBS_HOME": str((root / "specs-java-libs").resolve()),
               "LARA_FRAMEWORK_HOME": str((root / "lara-framework").resolve())}
        env.pop("JAVA_TOOL_OPTIONS", None)
        if index == 0:
            run("npm-lara", ["npm", "run", "build"], root / "lara-framework/Lara-JS", env)
        run(f"npm-clava-{spec.key}", ["npm", "run", "build"], root / "clava/Clava-JS", env)
        run(f"installdist-{spec.key}", ["gradle", "--no-daemon", "--offline", "-p", "ClavaWeaver", "installDist"],
            root / "clava", env)
        run(f"testclasses-{spec.key}", ["gradle", "--no-daemon", "--offline", "--init-script", str(java_init),
                                        "-p", "ClangAstParser", "testClasses"], root / "clava", env)


# ---------------------------------------------------------------------------------------------------------------
# Collection


def _load(name: str, path: Path) -> Any:
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


class Collector:
    """Runs the rotated App and wall matrices for every stage and records validated observations."""

    def __init__(self, output: Path, quick: bool):
        self.output = output
        self.quick = quick
        self.results_path = output / "results.json"
        self.app = _load("three_way_app", PROTOCOL_COMPARISON / "app-build/run_app_build_matrix.py")
        self.overlay = _load("three_way_overlay", PROTOCOL_COMPARISON / "app-build/build_overlay.py")
        self.comparison = self.app.comparison
        self.cohort = support.load_cohort_baseline(REFRESH_DIR / "cohort-baseline.json")
        self.expected_java = support.load_pinned_test_ids(REFRESH_DIR / "java-test-identities.json")
        self.js_orders = support.load_js_file_orders(REFRESH_DIR / "js-file-orders.json")
        self.sequencer = output / "original-file-order.ts"
        self.java_init = output / "java-suite.init.gradle"
        self.record: dict[str, Any] = {
            "created_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "status": "preparing", "quick": quick,
            "methodology": "Text + ccache, Protobuf and FlatBuffers in one session; rotated serial rounds of App "
                           "and separate uninstrumented wall commands",
            "snapshot": json.loads((output / "snapshot-identity.json").read_text()),
            "stages": {}, "preflight": [], "seeds": [], "observations": [],
        }
        self.stages: dict[str, dict[str, Any]] = {}
        self.workloads: dict[tuple[str, str], Any] = {}
        self.content: dict[str, tuple[list[tuple[str, ...]], collections.Counter]] = {}
        self._patch_shared_runner()

    def save(self) -> None:
        self.results_path.write_text(json.dumps(self.record, indent=2, default=str) + "\n")

    def _patch_shared_runner(self) -> None:
        a = self.app
        # Stages select published releases through their selector; the local native-root override is not used.
        source = inspect.getsource(a.execute_cell)
        override = '        if stage["key"] == "protobuf":\n            command += [f"-PclangDumperRoot={stage[\'native_root\']}"]\n'
        if source.count(override) != 1:
            raise RuntimeError("shared App runner native-root selector changed")
        source = support.pin_vitest_runner_config_loader(source.replace(override, ""))
        exec(compile(source, str(PROTOCOL_COMPARISON / "app-build/run_app_build_matrix.py"), "exec"), a.__dict__)

        base_init = (PROTOCOL_COMPARISON / "java-suite.init.gradle").read_text()
        anchor = "                excludeTestsMatching 'pt.up.fe.specs.clang.utils.ClassesServiceConcurrencyTest'\n"
        self.java_init.write_text(base_init.replace(anchor, anchor + "                excludeTestsMatching "
                                                    "'pt.up.fe.specs.clang.dumper.ClangFrameworkSearchTest'\n", 1))
        a.SCRIPT_ROOT = self.output

        self.comparison.cache_namespace = lambda stage: stage["namespace"]
        base_environment = a.base_environment

        def environment(stage, *args, **kwargs):
            env = base_environment(stage, *args, **kwargs)
            env["JAVA_TOOL_OPTIONS"] = re.sub(r" -Dclava\.astWire=\S+", "", env["JAVA_TOOL_OPTIONS"])
            env.pop("FLAT_NATIVE", None)
            return env

        a.base_environment = environment
        write_js_config = a.write_js_config

        def ordered_js_config(run_dir, stage, runtime):
            config = self._emitted_runtime(write_js_config(run_dir, stage, runtime), stage)
            text = config.read_text().replace(
                "export default { ...baseConfig,",
                f"import OriginalOrder from {json.dumps(self.sequencer.as_uri())};\nexport default {{ ...baseConfig,").replace(
                "test: { ...baseConfig.test, environment:",
                "test: { ...baseConfig.test, fileParallelism: false, maxWorkers: 1, minWorkers: 1, "
                "sequence: {sequencer:OriginalOrder}, environment:")
            config.write_text(text)
            return config

        a.write_js_config = ordered_js_config

    def _emitted_runtime(self, config: Path, stage: dict[str, Any]) -> Path:
        """Points generated configs at the stage's emitted package modules, which the tests also import."""
        clava = stage["clava"]
        replacements = {
            clava.parent / "node_modules/@specs-feup/lara/vitest/weaverVitestConfig.ts": "@specs-feup/lara/vitest/weaverVitestConfig.ts",
            clava / "Clava-JS/code/WeaverConfiguration.ts": "@specs-feup/clava/code/WeaverConfiguration.ts",
            clava / "Clava-JS/api/Joinpoints.ts": "@specs-feup/clava/api/Joinpoints.ts",
            clava / "Clava-JS/code/sideEffects.ts": "@specs-feup/clava/code/sideEffects.ts",
        }
        resolved = stage["resolved_runtime"]
        text = config.read_text()
        for source, specifier in replacements.items():
            text = text.replace(json.dumps(source.as_uri()), json.dumps(resolved[specifier]))
        config.write_text(text)
        shim = config.parent / "appBuildWeaverEnvironment.ts"
        environment = (clava.parent / "node_modules/@specs-feup/lara/vitest/weaverEnvironment.ts").as_uri()
        shim.write_text(shim.read_text().replace(json.dumps(environment),
                                                 json.dumps(resolved["@specs-feup/lara/vitest/weaverEnvironment.ts"])))
        return config

    def _resolve_runtime(self, clava: Path) -> dict[str, str]:
        specifiers = ["@specs-feup/lara/vitest/weaverVitestConfig.ts", "@specs-feup/lara/vitest/weaverEnvironment.ts",
                      "@specs-feup/clava/code/WeaverConfiguration.ts", "@specs-feup/clava/api/Joinpoints.ts",
                      "@specs-feup/clava/code/sideEffects.ts", "vitest/node"]
        script = (f"const specs={json.dumps(specifiers)};"
                  "console.log(JSON.stringify(Object.fromEntries(specs.map(s=>[s,import.meta.resolve(s)]))))")
        resolved = json.loads(subprocess.run(["node", "--input-type=module", "-e", script], cwd=clava / "Clava-JS",
                                             check=True, capture_output=True, text=True).stdout)
        for specifier, url in resolved.items():
            if not url.startswith("file:") or not Path(unquote(urlparse(url).path)).is_file():
                raise RuntimeError(f"{specifier} did not resolve to a file from {clava}: {url}")
        return resolved

    def setup_stages(self) -> None:
        for spec in STAGES:
            root = self.output / "stages" / spec.key
            clava = root / "clava"
            runtime = clava / "ClavaWeaver/build/install/ClavaWeaver"
            stage = {
                "key": spec.key, "root": root, "clava": clava, "frontend_clava": clava, "runtime": runtime,
                "runtime_lib": runtime / "lib", "native_root": None, "wire": spec.wire, "cache": True,
                "cache_enabled": True, "namespace": spec.namespace(clava), "seed": spec.resource_seed,
                "resolved_runtime": self._resolve_runtime(clava),
            }
            stage["overlay"] = self.overlay.build_overlay(spec.key, clava, runtime, self.output / "overlays" / spec.key)
            self.stages[spec.key] = stage
            self.record["stages"][spec.key] = {
                "namespace": stage["namespace"],
                "runtime_jars": {jar.name: self.app.sha256_file(jar) for jar in (runtime / "lib").glob("*.jar")},
                **self.record["snapshot"]["stages"][spec.key],
            }
        self.save()

    def _resources(self, stage: dict[str, Any], phase: str, suite: str) -> None:
        """Gives each phase/suite/stage its own DUMPER_FOLDER copy of the stage's verified release cache."""
        phase_root = self.output / phase
        parent = (phase_root / "temp" / suite / stage["key"] / f"clang_ast_exe_{getpass.getuser()}" if suite == "java"
                  else phase_root / "cache" / suite / stage["key"] / "@specs-feup/clava")
        parent.mkdir(parents=True, exist_ok=True)
        destination = parent / "clang-dumper"
        if stage["seed"] is not None and not destination.exists():
            shutil.copytree(stage["seed"], destination, symlinks=True)

    def _validate(self, row: dict[str, Any], stage: dict[str, Any], phase: str) -> dict[str, Any]:
        suite = row["suite"]
        run = Path(row["run_dir"])
        expected = self.cohort["suites"][suite]
        if suite == "java":
            ids = support.parse_junit_results(run / "junit-results")["identities"]
            if ids != self.expected_java:
                raise RuntimeError(f"{stage['key']} Java test identities differ from the pinned 116-test cohort")
        else:
            report = json.loads((run / "vitest.json").read_text())
            ids = self.app.js_test_identities(report)
            order = [item["name"].split("/Clava-JS/")[1]
                     for item in sorted(report["testResults"], key=lambda item: item["startTime"])]
            if order != self.js_orders[phase]:
                raise RuntimeError(f"{stage['key']} JavaScript file order changed: {order}")
        if self.app.digest(ids) != expected["test_identity_sha256"]:
            raise RuntimeError(f"{stage['key']} {suite} test identities differ from the pinned cohort")
        counts = row["test_counts"]
        for field in ("total_tests", "passed_tests", "failed_tests", "skipped_tests"):
            if counts.get(field) != expected["test_counts"][field]:
                raise RuntimeError(f"{stage['key']} {suite} {field} differs from the pinned cohort: {counts}")
        if phase == "app":
            if (row["app_calls"], row["syntax_only_calls"]) != (expected["app_calls"], expected["syntax_only_calls"]):
                raise RuntimeError(f"{stage['key']} {suite} App call counts differ from the pinned cohort")
            captures = [json.loads(line) for line in Path(row["metrics"]).read_text().splitlines() if line.strip()]
            # Harnesses stage inputs in different folders, so stages are compared by file names. Content may
            # differ only where a test re-parses the code its own Clava revision generated; that count is kept.
            calls = [tuple(sorted((Path(source["path"]).name, source["sha256"]) for source in capture["resolved_sources"]))
                     for capture in captures if capture.get("elapsed_ms") is not None]
            names = sorted(tuple(name for name, _ in call) for call in calls)
            reference = self.content.setdefault(suite, (names, collections.Counter(calls)))
            if reference[0] != names:
                raise RuntimeError(f"{stage['key']} {suite} App calls parse different files than other stages")
            row["calls_with_different_content"] = sum((collections.Counter(calls) - reference[1]).values())
            heap = {capture["jvm_max_memory_bytes"] for capture in captures}
            if suite == "java" and heap != {512 * 1024 * 1024}:
                raise RuntimeError(f"Java App worker heap differs from 512 MiB: {heap}")
            workload = self.app.workload_counter(captures)
            if self.workloads.setdefault((stage["key"], suite), workload) != workload:
                raise RuntimeError(f"{stage['key']} {suite} App workload changed between runs")
        row["phase"] = phase
        return row

    def app_run(self, stage, suite, mode, repeat, ordinal, measured):
        self._resources(stage, "app", suite)
        row = self.app.execute_cell(stage, suite, mode, repeat, self.output / "app", ordinal, measured)
        return self._validate(row, stage, "app")

    def wall_run(self, stage, suite, mode, repeat, ordinal, measured):
        """Runs the same suite command without the App overlay and times the whole process."""
        self._resources(stage, "wall", suite)
        a, comparison = self.app, self.comparison
        phase_root = self.output / "wall"
        run = phase_root / ("measured" if measured else "preflight") / suite / f"{ordinal:03d}-{stage['key']}-{mode}-r{repeat}"
        run.mkdir(parents=True)
        temp = phase_root / "temp" / suite / stage["key"]
        temp.mkdir(parents=True, exist_ok=True)
        env = a.base_environment(stage, suite, mode, repeat, run, run / "unused-metrics", temp,
                                 Path(stage["overlay"]["overlay_jar"]), phase_root)
        for name in [name for name in env if name.startswith("APP_BUILD_")]:
            env.pop(name)
        cache = a.cache_dir_for(stage, suite, phase_root)
        probe = comparison.install_direct_ccache_probe(run, env) if mode == "direct" else None
        comparison.configure_cache_mode(stage, mode, measured, env, cache)
        if suite == "java":
            init = a.write_gradle_overlay_init(run)
            init.write_text(init.read_text().replace(
                "      classpath = files(System.getenv('APP_BUILD_OVERLAY_JAR')) + classpath\n", ""))
            command = ["gradle", "--no-daemon", "--offline", "--init-script", str(self.java_init),
                       "--init-script", str(init), "-p", "ClangAstParser", "test"]
            cwd = stage["clava"]
            results = cwd / "ClangAstParser/build/test-results/test"
            shutil.rmtree(results, ignore_errors=True)
        else:
            resolved = stage["resolved_runtime"]
            config = run / "vitest.wall.config.ts"
            config.write_text(
                f"import {{createWeaverVitestConfig}} from {json.dumps(resolved['@specs-feup/lara/vitest/weaverVitestConfig.ts'])};\n"
                f"import {{weaverConfig}} from {json.dumps(resolved['@specs-feup/clava/code/WeaverConfiguration.ts'])};\n"
                f"import OriginalOrder from {json.dumps(self.sequencer.as_uri())};\n"
                f"const base=createWeaverVitestConfig({{...weaverConfig,jarPath:{json.dumps(str(stage['runtime']))},"
                f"importForSideEffects:{json.dumps([resolved['@specs-feup/clava/api/Joinpoints.ts'], resolved['@specs-feup/clava/code/sideEffects.ts']])}}});\n"
                f"export default {{...base,test:{{...base.test,fileParallelism:false,maxWorkers:1,minWorkers:1,"
                f"sequence:{{sequencer:OriginalOrder}}}},root:{json.dumps(str(stage['clava'] / 'Clava-JS'))}}};\n")
            command = ["npm", "exec", "--workspace", "@specs-feup/clava", "--", "vitest", "run", "--config", str(config),
                       "--configLoader", "runner", "--reporter=json", "--outputFile", str(run / "vitest.json"),
                       "-t", comparison.JS_TEST_FILTER]
            cwd = stage["clava"] / "Clava-JS"
        (run / "command.json").write_text(json.dumps({"argv": command, "cwd": str(cwd)}, indent=2) + "\n")
        started = time.perf_counter()
        with (run / "run.log").open("w") as log:
            process = subprocess.run(command, cwd=cwd, env=env, stdout=log, stderr=subprocess.STDOUT)
        elapsed = time.perf_counter() - started
        if suite == "java" and results.exists():
            shutil.copytree(results, run / "junit-results")
        counts = a.check_test_counts(suite, run, process.returncode)
        cache_state = comparison.cache_validation(stage, mode, measured, cache, probe)
        if not cache_state["passed"]:
            raise RuntimeError(f"cache-state gate failed for {stage['key']}/{suite}/{mode}: {cache_state}")
        row = {"suite": suite, "stage": stage["key"], "mode": mode, "repeat": repeat, "measured": measured,
               "valid": True, "return_code": process.returncode, "wall_s": elapsed, "test_counts": counts,
               "cache_validation": cache_state, "run_dir": str(run)}
        return self._validate(row, stage, "wall")

    def _set_order(self, phase: str) -> None:
        order = self.js_orders[phase]
        vitest_node = next(iter(self.stages.values()))["resolved_runtime"]["vitest/node"]
        self.sequencer.write_text(
            f"import {{BaseSequencer}} from {json.dumps(vitest_node)};\nconst order={json.dumps(order)};\n"
            "export default class OriginalOrder extends BaseSequencer {async sort(files) {"
            "const key=f=>f.moduleId.split('/Clava-JS/')[1];"
            "for(const f of files) if(!order.includes(key(f)))throw new Error('Unexpected test file '+key(f));"
            "return [...files].sort((a,b)=>order.indexOf(key(a))-order.indexOf(key(b)));}}\n")

    def collect(self) -> dict[str, Any]:
        phases = [("app", self.app_run)] if self.quick else [("app", self.app_run), ("wall", self.wall_run)]
        modes = ("direct", "warm") if self.quick else MODES
        rounds = 1 if self.quick else 4
        keys = list(self.stages)
        ordinal = 0
        try:
            seeds: dict[tuple[str, str, str], Path] = {}
            for phase, run in phases:
                self._set_order(phase)
                for key in keys:
                    stage = self.stages[key]
                    for suite in SUITES:
                        ordinal += 1
                        self.record["preflight"].append(run(stage, suite, "direct", 0, ordinal, False))
                        ordinal += 1
                        self.record["seeds"].append(run(stage, suite, "warm", 0, ordinal, False))
                        cache = self.app.cache_dir_for(stage, suite, self.output / phase)
                        seed = self.output / phase / "seeds" / key / suite
                        shutil.copytree(cache, seed)
                        seeds[phase, key, suite] = seed
                        self.save()
                        print(json.dumps({"preflight": phase, "stage": key, "suite": suite, "valid": True}), flush=True)
            self.record["status"] = "running"
            for phase, run in phases:
                self._set_order(phase)
                for repeat in range(1, rounds + 1):
                    offset = (repeat - 1) % len(keys)
                    for position, key in enumerate(keys[offset:] + keys[:offset]):
                        stage = self.stages[key]
                        shift = (repeat - 1 + position) % len(modes)
                        for index, mode in enumerate(modes[shift:] + modes[:shift]):
                            suites = SUITES if (repeat + position + index) % 2 == 0 else SUITES[::-1]
                            for suite in suites:
                                if mode == "warm":
                                    cache = self.app.cache_dir_for(stage, suite, self.output / phase)
                                    shutil.rmtree(cache, ignore_errors=True)
                                    shutil.copytree(seeds[phase, key, suite], cache)
                                ordinal += 1
                                row = run(stage, suite, mode, repeat, ordinal, True)
                                self.record["observations"].append(row)
                                self.save()
                                print(json.dumps({k: row.get(k) for k in (
                                    "phase", "stage", "suite", "mode", "repeat", "app_elapsed_ms", "wall_s")}), flush=True)
            self.record["status"] = "complete"
            self.record["completed_utc"] = dt.datetime.now(dt.timezone.utc).isoformat()
        except Exception as error:
            self.record["status"] = "failed"
            self.record["error"] = repr(error)
            raise
        finally:
            self.save()
        return self.record


def summary(record: dict[str, Any]) -> str:
    """Median App time per suite, mode and stage, in seconds."""
    stages = list(record["stages"])
    lines = [f"{'suite':10} {'mode':7} " + " ".join(f"{stage:>12}" for stage in stages)]
    for suite in SUITES:
        for mode in MODES:
            cells = []
            for stage in stages:
                values = [row["app_elapsed_ms"] / 1000 for row in record["observations"] if row["phase"] == "app"
                          and (row["suite"], row["mode"], row["stage"]) == (suite, mode, stage)]
                cells.append(f"{statistics.median(values):12.2f}" if values else f"{'-':>12}")
            if any(cell.strip() != "-" for cell in cells):
                lines.append(f"{suite:10} {mode:7} " + " ".join(cells))
    return "\n".join(lines)


# ---------------------------------------------------------------------------------------------------------------
# Command line


def run(quick: bool, draft_id: str | None, keep: bool) -> Path:
    with refresh._exclusive_lock(refresh.DEFAULT_LOCK):
        stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        output = OUTPUT_BASE / f"{'quick' if quick else 'full'}-{stamp}"
        prepare(output)
        try:
            collector = Collector(output, quick)
            build(output, collector.java_init)
            collector.setup_stages()
            record = collector.collect()
            (output / "summary.txt").write_text(summary(record) + "\n")
            print(summary(record))
            if not quick:
                html = render(output)
                if draft_id is not None:
                    publish(html, draft_id)
        finally:
            if not keep:
                # Keep results, logs and the report; drop worktrees, ccache seeds and parser temp trees.
                for phase in ("app", "wall"):
                    for name in ("cache", "temp", "seeds"):
                        shutil.rmtree(output / phase / name, ignore_errors=True)
                remove_worktrees(output)
        return output


def remove_worktrees(output: Path) -> None:
    """Detaches every worktree created by `prepare` and deletes the stage trees."""
    worktrees = [(WORKSPACE / name, output / "shared" / name) for name in ("specs-java-libs", "lara-framework")]
    worktrees += [(spec.repository, output / "stages" / spec.key / "clava") for spec in STAGES]
    for repository, worktree in worktrees:
        if worktree.exists():
            subprocess.run(["git", "worktree", "remove", "--force", str(worktree)], cwd=repository,
                           capture_output=True, check=False)
    shutil.rmtree(output / "stages", ignore_errors=True)
    shutil.rmtree(output / "shared", ignore_errors=True)


def render(output: Path) -> Path:
    report = _load("three_way_report", refresh.REPORT_SCRIPT)
    html = report.render_comparison(json.loads((output / "results.json").read_text()),
                                    refresh.load_json(refresh.FROZEN_CONTROLS))
    refresh.validate_report_html(html)
    path = output / "report.html"
    path.write_text(html)
    return path


def publish(html: Path, draft_id: str) -> None:
    subprocess.run(["draftlink", "update", draft_id, "--file", str(html), "--private"], check=True)
    readback = subprocess.run(["draftlink", "read", draft_id], check=True, capture_output=True).stdout
    if readback != html.read_bytes():
        raise RuntimeError("DraftLink readback differs from the rendered report")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    run_parser = commands.add_parser("run", help="snapshot, build, collect and render (and optionally publish)")
    run_parser.add_argument("--quick", action="store_true", help="one App repeat in direct and warm modes")
    run_parser.add_argument("--draft-id", help="privately update this DraftLink after a full run")
    run_parser.add_argument("--keep", action="store_true", help="keep worktrees and cache trees")
    render_parser = commands.add_parser("render", help="re-render a completed run directory")
    render_parser.add_argument("run_dir", type=Path)
    args = parser.parse_args(argv)
    if args.command == "run":
        if args.quick and args.draft_id:
            parser.error("quick runs are never published")
        print(run(args.quick, args.draft_id, args.keep))
    else:
        print(render(args.run_dir))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
