from __future__ import annotations

import copy
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest import mock


SCRIPT = Path(__file__).with_name("refresh_benchmark.py")
SPEC = importlib.util.spec_from_file_location("refresh_benchmark_tests", SCRIPT)
REFRESH = importlib.util.module_from_spec(SPEC)
assert SPEC is not None and SPEC.loader is not None
sys.modules[SPEC.name] = REFRESH
SPEC.loader.exec_module(REFRESH)

PROTOBUF_ROOT = Path(__file__).resolve().parents[2]
CLAVA_ROOT = PROTOBUF_ROOT.parents[1]
FIXTURE_ROOT = PROTOBUF_ROOT / "validation/evidence/benchmark-20261008-rc8"


def write_json(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def completed_bundle(root: Path) -> Path:
    run_dir = root / "completed"
    runtime_dir = run_dir / "runtime"
    memory_dir = run_dir / "memory"
    runtime_dir.mkdir(parents=True)
    memory_dir.mkdir()
    runtime = json.loads((FIXTURE_ROOT / "updated-protobuf-results.json").read_text())
    runtime["driver_sha256"] = REFRESH.sha256_file(REFRESH.REFRESH_DIR / "collect_runtime.py")
    runtime["workload_contract"] = copy.deepcopy(REFRESH.EXPECTED_WORKLOAD)
    runtime["cohort_baseline_sha256"] = REFRESH.support.COHORT_BASELINE_SHA256
    runtime["cohort_baseline_results_sha256"] = REFRESH.COHORT_BASELINE["baseline_results_sha256"]
    runtime["sources"]["protobuf"]["selected_java_test_sources"] = copy.deepcopy(
        REFRESH.COHORT_BASELINE["java_selected_test_sources"])
    runtime["sources"]["protobuf"]["javascript_test_sources"] = copy.deepcopy(
        REFRESH.COHORT_BASELINE["javascript_test_sources"])
    js_overlay = REFRESH.support.expected_js_workload_overlay(
        runtime["sources"]["protobuf"]["repositories"]["clava"]["revision"]
    )
    runtime["sources"]["protobuf"]["javascript_workload_overlay"] = copy.deepcopy(js_overlay)
    lara_config = REFRESH.PROTOBUF_ROOT.parents[2] / "lara-framework/Lara-JS/vitest/weaverVitestConfig.ts"
    config_sha = REFRESH.sha256_file(lara_config)
    runtime["js_vitest_defaults"] = {
        "isolate": False, "fileParallelism": False, "maxWorkers": 1, "config_loader": "runner",
        "source_sha256": config_sha, "emitted_sha256": config_sha,
    }
    runtime["js_runtime_exports"] = {
        "vitest_node_url": "file:///snapshot/node_modules/vitest/dist/node.js",
    }
    runtime["preflight_complete"] = True
    runtime["preflight_completed_utc"] = runtime["created_utc"]
    for index, row in enumerate(runtime["observations"] + runtime["preflight"] + runtime["seeds"]):
        sidecar = run_dir / "sidecars" / f"row-{index}"
        sidecar.mkdir(parents=True)
        row["run_dir"] = str(sidecar)
        if row.get("mode") == "direct":
            marker = sidecar / "ccache-invocations.txt"
            row["cache_validation"]["direct_probe_marker"] = str(marker)
        if row.get("suite") == "clava-js":
            write_json(sidecar / "command.json", {
                "argv": ["npm", "exec", "--", "vitest", "run", "--configLoader", "runner"]
            })
    write_json(runtime_dir / "results.json", runtime)
    memory = json.loads((FIXTURE_ROOT / "memory.json").read_text())
    memory["driver_sha256"] = REFRESH.sha256_file(REFRESH.REFRESH_DIR / "collect_memory.py")
    checkout = Path(runtime["sources"]["protobuf"]["checkout"])
    for workload in memory["workloads"]:
        workload["source"] = str(checkout / "ClangAstParser/test-resources" / workload["relative_source"])
    for observation in memory["observations"]:
        work = memory_dir / "observations" / observation["workload"] / f"repeat-{observation['repeat']}"
        work.mkdir(parents=True)
        (work / "tmp").mkdir()
        (work / "resources").mkdir()
        observation["log"] = str(work / "probe.log")
        observation["resource_root"] = str(work / "resources")
        for phase in observation["heap_phases"]:
            phase["dumper_resource_root"] = str(work / "resources")
        runtime_path = Path(memory["runtime"]["path"])
        classes_path = Path(memory["runtime"]["classes"])
        java_binary = Path(memory["runtime"]["java_binary"])
        observation["command"] = [
            str(Path(observation["command"][0]).resolve()), "-v", "-o", str(work / "time.txt"), "--",
            str(java_binary.resolve()), *memory["jvm_options"],
            "-Djava.io.tmpdir=" + str(work / "tmp"), "-cp",
            str(classes_path.resolve()) + os.pathsep + str(runtime_path.resolve() / "lib" / "*"),
            "ValidationProbe", "memory", memory["workloads"][0]["source"] if observation["workload"] == "nas-lu"
            else memory["workloads"][1]["source"], str(work),
            next(item["standard"] for item in memory["workloads"] if item["key"] == observation["workload"]),
            "20", "true", str(work / "resources"),
        ]
        (work / "command.json").write_text(json.dumps({"argv": observation["command"], "cwd": str(checkout)}))
        old_raw = observation["gnu_time"]["raw"]
        observation["gnu_time"]["raw"] = (
            "\tCommand being timed: \"" + " ".join(observation["command"][5:]) + "\"\n"
            + old_raw.split("\n", 1)[1]
        )
        (work / "time.txt").write_text(observation["gnu_time"]["raw"])
        (work / "probe.log").write_text("\n".join(
            "CLAVA_HEAP " + json.dumps(phase) for phase in observation["heap_phases"]
        ) + "\n")
    write_json(memory_dir / "memory.json", memory)
    for key, name in (("nas-lu", "memory-nas-lu.json"), ("templates", "memory-templates.json")):
        write_json(memory_dir / name, REFRESH.support.normalize_memory_workload(memory, key))
    manifest = FIXTURE_ROOT / "release-manifest.json"
    (run_dir / "release-manifest.json").write_bytes(manifest.read_bytes())
    repositories = runtime["sources"]["protobuf"]["repositories"]
    snapshot_identity = {
        "sources": {
            name: {"revision": repositories[name]["revision"]}
            for name in ("clava", "specs-java-libs", "lara-framework")
        },
        "published_release": {
            "tag": runtime["selected_release"]["tag"],
            "native_repository_revision": repositories["clang-dumper"]["revision"],
        },
        "isolated_overlays": {"javascript_workload": copy.deepcopy(js_overlay)},
    }
    write_json(run_dir / "snapshot-identity.json", snapshot_identity)
    write_json(run_dir / "status.json", {
        "status": "complete",
        "created_utc": runtime["created_utc"],
        "completed_utc": runtime["completed_utc"],
        "frozen_controls_sha256": REFRESH.sha256_file(REFRESH.FROZEN_CONTROLS),
        "runtime_driver_sha256": runtime["driver_sha256"],
        "memory_driver_sha256": memory["driver_sha256"],
        "snapshot_identity_sha256": REFRESH.sha256_file(run_dir / "snapshot-identity.json"),
        "release_tag": runtime["selected_release"]["tag"],
        "release_manifest_sha256": hashlib.sha256(manifest.read_bytes()).hexdigest(),
    })
    return run_dir


def load_prepare_module():
    prepare_path = Path(__file__).with_name("prepare_snapshot.py")
    spec = importlib.util.spec_from_file_location("protobuf_prepare_snapshot_tests", prepare_path)
    module = importlib.util.module_from_spec(spec)
    assert spec is not None and spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


PREPARE = load_prepare_module()


def load_memory_module():
    path = Path(__file__).with_name("collect_memory.py")
    spec = importlib.util.spec_from_file_location("protobuf_collect_memory_tests", path)
    module = importlib.util.module_from_spec(spec)
    assert spec is not None and spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


MEMORY = load_memory_module()


class RefreshWorkflowTests(unittest.TestCase):
    def test_refresh_cli_disables_source_tree_bytecode_writes(self) -> None:
        self.assertTrue(sys.dont_write_bytecode)

    def _copy_current_js_workload(self, target: Path) -> dict[str, bytes]:
        baseline = REFRESH.COHORT_BASELINE["javascript_test_sources"]
        original: dict[str, bytes] = {}
        for relative in baseline:
            contents = (CLAVA_ROOT / "Clava-JS" / relative).read_bytes()
            destination = target / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(contents)
            original[relative] = contents
        return original

    def _create_isolated_snapshot_worktree(
        self, root: Path, *, symlink_relative: str | None = None
    ) -> tuple[Path, Path, dict[str, bytes]]:
        canonical_clava = root / "canonical" / "clava"
        source_tests = canonical_clava / "Clava-JS"
        original = self._copy_current_js_workload(source_tests)
        implementation = source_tests / "api/Joinpoints.ts"
        implementation.parent.mkdir(parents=True, exist_ok=True)
        implementation.write_text("current production implementation\n")
        if symlink_relative is not None:
            external = root / "external-test-target.ts"
            external.write_bytes(original[symlink_relative])
            linked = source_tests / symlink_relative
            linked.unlink()
            linked.symlink_to(external)

        subprocess.run(["git", "init", "--quiet", str(canonical_clava)], check=True)
        subprocess.run(["git", "-C", str(canonical_clava), "config", "user.name", "Benchmark Test"], check=True)
        subprocess.run(["git", "-C", str(canonical_clava), "config", "user.email", "benchmark@example.invalid"], check=True)
        subprocess.run(["git", "-C", str(canonical_clava), "add", "Clava-JS"], check=True)
        subprocess.run(["git", "-C", str(canonical_clava), "commit", "--quiet", "-m", "fixture"], check=True)
        snapshot_clava = root / "snapshot" / "clava"
        snapshot_clava.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(["git", "-C", str(canonical_clava), "worktree", "add", "--quiet",
                        "--detach", str(snapshot_clava), "HEAD"], check=True)
        return canonical_clava, snapshot_clava / "Clava-JS", original

    def test_pinned_js_workload_stages_only_owned_test_fixtures(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            canonical_clava, snapshot_tests, current = self._create_isolated_snapshot_worktree(root)
            implementation = snapshot_tests / "api/Joinpoints.ts"
            canonical_before = {
                relative: (canonical_clava / "Clava-JS" / relative).read_bytes()
                for relative in current
            }
            fixture_root = REFRESH.REFRESH_DIR / "workload-fixtures/clava-js"
            fixture_before = {
                relative: (fixture_root / relative).read_bytes()
                for relative in current
            }
            revision = REFRESH.support.git(snapshot_tests.parent, "rev-parse", "HEAD")
            overlay = REFRESH.support.stage_pinned_js_workload(
                snapshot_tests, revision, canonical_clava,
            )

            self.assertEqual(overlay, REFRESH.support.expected_js_workload_overlay(revision))
            staged = {relative: REFRESH.support.sha256_file(snapshot_tests / relative)
                      for relative in current}
            self.assertEqual(staged, REFRESH.COHORT_BASELINE["javascript_test_sources"])
            self.assertEqual(implementation.read_text(), "current production implementation\n")
            self.assertEqual({relative: (canonical_clava / "Clava-JS" / relative).read_bytes()
                              for relative in current}, canonical_before)
            self.assertEqual({relative: (fixture_root / relative).read_bytes()
                              for relative in current}, fixture_before)

    def test_pinned_js_workload_rejects_unknown_current_and_fixture_bytes_before_writing(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            canonical_clava, snapshot_tests, initial = self._create_isolated_snapshot_worktree(root)
            revision = REFRESH.support.git(snapshot_tests.parent, "rev-parse", "HEAD")
            changed = "code/Sandbox.test.ts"
            (snapshot_tests / changed).write_bytes(initial[changed] + b"// unknown drift\n")
            before = {relative: (snapshot_tests / relative).read_bytes() for relative in initial}
            with self.assertRaisesRegex(RuntimeError, "current JavaScript test source hash is unknown"):
                REFRESH.support.stage_pinned_js_workload(snapshot_tests, revision, canonical_clava)
            self.assertEqual({relative: (snapshot_tests / relative).read_bytes()
                              for relative in initial}, before)

            canonical_clava, snapshot_tests, initial = self._create_isolated_snapshot_worktree(
                root / "bad-fixture",
            )
            revision = REFRESH.support.git(snapshot_tests.parent, "rev-parse", "HEAD")
            fixture_copy = root / "fixtures"
            shutil.copytree(REFRESH.REFRESH_DIR / "workload-fixtures/clava-js", fixture_copy)
            (fixture_copy / changed).write_bytes(b"unknown frozen fixture\n")
            before = {relative: (snapshot_tests / relative).read_bytes() for relative in initial}
            with self.assertRaisesRegex(RuntimeError, "fixture is missing or hash-mismatched"):
                REFRESH.support.stage_pinned_js_workload(
                    snapshot_tests, revision, canonical_clava, fixture_copy,
                )
            self.assertEqual({relative: (snapshot_tests / relative).read_bytes()
                              for relative in initial}, before)

    def test_pinned_js_workload_rejects_symlink_targets_and_canonical_checkout(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            relative = "api/LegacyIntegrationTests - CXX.test.ts"
            canonical_clava, snapshot_tests, original = self._create_isolated_snapshot_worktree(
                root, symlink_relative=relative,
            )
            revision = REFRESH.support.git(snapshot_tests.parent, "rev-parse", "HEAD")
            external = root / "external-test-target.ts"
            before = external.read_bytes()
            with self.assertRaisesRegex(RuntimeError, "symlink"):
                REFRESH.support.stage_pinned_js_workload(snapshot_tests, revision, canonical_clava)
            self.assertEqual(external.read_bytes(), before)
            self.assertTrue((snapshot_tests / relative).is_symlink())

            with self.assertRaisesRegex(RuntimeError, "canonical Clava checkout"):
                REFRESH.support.stage_pinned_js_workload(
                    canonical_clava / "Clava-JS",
                    REFRESH.support.git(canonical_clava, "rev-parse", "HEAD"),
                    canonical_clava,
                )

    def test_pinned_js_workload_validation_rejects_post_stage_corruption(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            canonical_clava, snapshot_tests, _ = self._create_isolated_snapshot_worktree(root)
            revision = REFRESH.support.git(snapshot_tests.parent, "rev-parse", "HEAD")
            overlay = REFRESH.support.stage_pinned_js_workload(snapshot_tests, revision, canonical_clava)
            changed = "code/Sandbox.test.ts"
            (snapshot_tests / changed).write_bytes(b"post-stage corruption\n")
            with self.assertRaisesRegex(RuntimeError, "do not match the frozen cohort"):
                REFRESH.support.validate_js_workload_snapshot(
                    snapshot_tests, overlay, revision, canonical_clava,
                )

    def test_memory_java_executable_is_resolved_through_symlinks(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            target = root / "java-real"
            target.write_text("#!/bin/sh\nexit 0\n")
            target.chmod(0o755)
            alias = root / "java"
            alias.symlink_to(target)
            with mock.patch.object(MEMORY.shutil, "which", return_value=str(alias)):
                self.assertEqual(MEMORY.resolve_executable("java"), str(target.resolve()))

    def test_command_selection_passes_explicit_snapshot_output_and_seed(self) -> None:
        snapshot = Path("/tmp/snapshot").resolve()
        output = Path("/tmp/output").resolve()
        seed = Path("/tmp/seed").resolve()
        runtime = REFRESH.runtime_command(snapshot, output, seed)
        memory = REFRESH.memory_command(snapshot, output, seed)
        for command in (runtime, memory):
            self.assertEqual(command[0], sys.executable)
            self.assertIn("--snapshot", command)
            self.assertIn("--output", command)
            self.assertIn("--resource-cache", command)
            self.assertIn(str(snapshot), command)
            self.assertIn(str(output), command)
            self.assertIn(str(seed), command)
        self.assertEqual(Path(runtime[2]).name, "collect_runtime.py")
        self.assertEqual(Path(memory[2]).name, "collect_memory.py")

    def test_snapshot_dependency_links_use_snapshot_workspace_packages(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            workspace = root / "workspace"
            snapshot = root / "snapshot"
            dependencies = workspace / "node_modules"
            specs = dependencies / "@specs-feup"
            specs.mkdir(parents=True)
            original_package = workspace / "clava/Clava-JS"
            snapshot_package = snapshot / "clava/Clava-JS"
            original_package.mkdir(parents=True)
            snapshot_package.mkdir(parents=True)
            (specs / "clava").symlink_to(original_package)
            PREPARE._link_node_modules(dependencies, snapshot / "node_modules", workspace, snapshot)
            mapped = snapshot / "node_modules/@specs-feup/clava"
            self.assertEqual(mapped.resolve(), snapshot_package.resolve())

    def test_snapshot_stages_hashed_workspace_manifest(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            workspace = root / "workspace"
            snapshot = root / "snapshot"
            workspace.mkdir()
            snapshot.mkdir()
            manifest = {
                "name": "test workspace", "workspaces": [
                    "lara-framework/Lara-JS", "clava/Clava-JS",
                ],
            }
            (workspace / "package.json").write_text(json.dumps(manifest))
            digest = PREPARE._stage_workspace_manifest(workspace, snapshot)
            self.assertEqual((snapshot / "package.json").read_bytes(),
                             (workspace / "package.json").read_bytes())
            self.assertEqual(digest, REFRESH.sha256_file(snapshot / "package.json"))

    def test_pinned_js_file_orders_accept_code_files_and_reject_drift(self) -> None:
        orders = REFRESH.support.load_js_file_orders(REFRESH.REFRESH_DIR / "js-file-orders.json")
        self.assertTrue(any(item.startswith("code/") for item in orders["app"]))
        with tempfile.TemporaryDirectory() as temp:
            changed = Path(temp) / "orders.json"
            payload = json.loads((REFRESH.REFRESH_DIR / "js-file-orders.json").read_text())
            payload["app"][0], payload["app"][1] = payload["app"][1], payload["app"][0]
            write_json(changed, payload)
            with self.assertRaisesRegex(RuntimeError, "fixture hash"):
                REFRESH.support.load_js_file_orders(changed)

    def test_runtime_hash_and_workload_cohort_are_required(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            REFRESH.validate_run_bundle(run_dir)
            results_path = run_dir / "runtime/results.json"
            runtime = json.loads(results_path.read_text())
            runtime["workload_contract"]["stages"] = ["protobuf", "text"]
            write_json(results_path, runtime)
            with self.assertRaisesRegex(ValueError, "workload contract"):
                REFRESH.validate_run_bundle(run_dir)

            runtime["workload_contract"] = copy.deepcopy(REFRESH.EXPECTED_WORKLOAD)
            runtime["driver_sha256"] = "0" * 64
            write_json(results_path, runtime)
            with self.assertRaisesRegex(ValueError, "driver hash"):
                REFRESH.validate_run_bundle(run_dir)

            runtime["driver_sha256"] = REFRESH.sha256_file(REFRESH.REFRESH_DIR / "collect_runtime.py")
            runtime["native_sha256"]["protobuf"] = "0" * 64
            write_json(results_path, runtime)
            with self.assertRaisesRegex(ValueError, "measured native tool"):
                REFRESH.validate_run_bundle(run_dir)

    def test_runtime_selected_test_source_hashes_are_required(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            results_path = run_dir / "runtime/results.json"
            runtime = json.loads(results_path.read_text())
            source_hashes = runtime["sources"]["protobuf"]["selected_java_test_sources"]
            source_hashes[next(iter(source_hashes))] = "0" * 64
            write_json(results_path, runtime)
            with self.assertRaisesRegex(ValueError, "Java test source hashes"):
                REFRESH.validate_run_bundle(run_dir)

    def test_runtime_javascript_workload_overlay_provenance_is_pinned(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            results_path = run_dir / "runtime/results.json"
            runtime = json.loads(results_path.read_text())
            file_record = runtime["sources"]["protobuf"]["javascript_workload_overlay"]["files"]
            file_record["api/LegacyIntegrationTests - CXX.test.ts"]["original_current_sha256"] = "0" * 64
            write_json(results_path, runtime)
            with self.assertRaisesRegex(ValueError, "overlay provenance"):
                REFRESH.validate_run_bundle(run_dir)

    def test_snapshot_and_runtime_javascript_workload_provenance_must_match(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            identity_path = run_dir / "snapshot-identity.json"
            identity = json.loads(identity_path.read_text())
            identity["isolated_overlays"]["javascript_workload"]["files"][
                "api/LegacyIntegrationTests - CXX.test.ts"]["original_current_sha256"] = "0" * 64
            write_json(identity_path, identity)
            status_path = run_dir / "status.json"
            status = json.loads(status_path.read_text())
            status["snapshot_identity_sha256"] = REFRESH.sha256_file(identity_path)
            write_json(status_path, status)
            with self.assertRaisesRegex(ValueError, "snapshot and runtime"):
                REFRESH.validate_run_bundle(run_dir)

    def test_runtime_observations_are_checked_before_render_and_publish(self) -> None:
        mutations = (
            ("App call count", lambda row: row.update(app_calls=1)),
            ("cache validation", lambda row: row["cache_validation"].update(passed=False)),
            ("test identities", lambda row: row.update(test_identity_sha256="0" * 64)),
            ("return_code", lambda row: row.update(return_code=99)),
        )
        for label, mutate in mutations:
            with self.subTest(label=label), tempfile.TemporaryDirectory() as temp:
                run_dir = completed_bundle(Path(temp))
                results_path = run_dir / "runtime/results.json"
                runtime = json.loads(results_path.read_text())
                row = next(item for item in runtime["observations"]
                           if item.get("phase") == "app")
                mutate(row)
                write_json(results_path, runtime)
                with self.assertRaises(ValueError):
                    REFRESH.validate_run_bundle(run_dir)

    def test_failed_collection_is_saved_and_propagated(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            snapshot = root / "snapshot"
            snapshot.mkdir()
            write_json(snapshot / "snapshot-identity.json", {
                "published_release": {"tag": "v18.1.8_6-rc8"},
            })
            output = root / "failed-run"

            def fail(command: list[str], log_path: Path) -> int:
                self.assertIn("collect_runtime.py", command[2])
                self.assertIn("--resource-cache", command)
                log_path.write_text("preflight stopped\n", encoding="utf-8")
                return 17

            with self.assertRaisesRegex(RuntimeError, "exit code 17"):
                REFRESH.collect(snapshot, root / "seed", output,
                                lock_path=root / "lock", command_runner=fail)
            status = json.loads((output / "status.json").read_text())
            self.assertEqual(status["status"], "failed")
            self.assertIn("exit code 17", status["error"])
            self.assertEqual((output / "runtime.stdout.log").read_text(), "preflight stopped\n")
            self.assertFalse((output / "report.html").exists())

    def test_concurrent_collection_lock_fails_before_starting_work(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            snapshot = root / "snapshot"
            snapshot.mkdir()
            write_json(snapshot / "snapshot-identity.json", {
                "published_release": {"tag": "v18.1.8_6-rc8"},
            })
            lock_path = root / "lock"
            with lock_path.open("a") as lock:
                import fcntl
                fcntl.flock(lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
                runner = mock.Mock()
                with self.assertRaisesRegex(RuntimeError, "another Protobuf benchmark"):
                    REFRESH.collect(snapshot, root / "seed", root / "run",
                                    lock_path=lock_path, command_runner=runner)
                runner.assert_not_called()
                self.assertFalse((root / "run").exists())

    def test_incomplete_capture_cannot_reach_draftlink(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            status_path = run_dir / "status.json"
            status = json.loads(status_path.read_text())
            status["status"] = "failed"
            write_json(status_path, status)
            runner = mock.Mock()
            with self.assertRaisesRegex(ValueError, "not complete"):
                REFRESH.publish_private(run_dir, "draft-id", private=True, runner=runner)
            runner.assert_not_called()

    def test_publish_requires_private_flag(self) -> None:
        with self.assertRaisesRegex(ValueError, "explicit --private"):
            REFRESH.publish_private(Path("/unused"), "draft-id", private=False)
        with mock.patch("sys.stderr", new=io.StringIO()):
            with self.assertRaises(SystemExit):
                REFRESH.main(["publish", "--run-dir", "/unused", "--draft-id", "draft-id"])

    def test_publish_uses_private_update_and_exact_readback(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            report_path = REFRESH.render_run(run_dir)
            html = report_path.read_bytes()
            calls: list[tuple[list[str], dict[str, object]]] = []

            def runner(command: list[str], **kwargs: object):
                calls.append((command, kwargs))
                if command[1] == "update":
                    return type("Result", (), {"returncode": 0, "stdout": b"updated", "stderr": b""})()
                return type("Result", (), {"returncode": 0, "stdout": html, "stderr": b""})()

            with mock.patch.object(REFRESH.shutil, "which", return_value="/mock/draftlink"):
                result = REFRESH.publish_private(run_dir, "draft-id", private=True, runner=runner)
            self.assertTrue(result["published"])
            self.assertEqual(len(calls), 2)
            self.assertEqual(calls[0][0][0:3], ["/mock/draftlink", "update", "draft-id"])
            self.assertIn("--private", calls[0][0])
            self.assertEqual(calls[1][0], ["/mock/draftlink", "read", "draft-id"])
            self.assertEqual(result["sha256"], hashlib.sha256(html).hexdigest())

    def test_readback_mismatch_is_retained_and_reported(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            def runner(command: list[str], **kwargs: object):
                stdout = b"updated" if command[1] == "update" else b"different report"
                return type("Result", (), {"returncode": 0, "stdout": stdout, "stderr": b""})()

            with mock.patch.object(REFRESH.shutil, "which", return_value="/mock/draftlink"):
                with self.assertRaisesRegex(RuntimeError, "byte-for-byte"):
                    REFRESH.publish_private(run_dir, "draft-id", private=True, runner=runner)
            mismatch = next((run_dir / "publications").glob("*/readback-mismatch.html"))
            self.assertEqual(mismatch.read_bytes(), b"different report")

    def test_failed_publication_logs_are_kept_across_retry(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            def fail_update(command: list[str], **kwargs: object):
                return type("Result", (), {"returncode": 7, "stdout": b"update failed", "stderr": b"failure"})()

            with mock.patch.object(REFRESH.shutil, "which", return_value="/mock/draftlink"):
                with self.assertRaisesRegex(RuntimeError, "update failed with exit code 7"):
                    REFRESH.publish_private(run_dir, "draft-id", private=True, runner=fail_update)
                old_attempt = next((run_dir / "publications").glob("*"))
                self.assertEqual((old_attempt / "update.stdout.log").read_bytes(), b"update failed")
                self.assertEqual(json.loads((old_attempt / "publication.json").read_text())["status"],
                                 "update_failed")

                html = (run_dir / "report.html").read_bytes()
                def retry(command: list[str], **kwargs: object):
                    stdout = b"updated" if command[1] == "update" else html
                    return type("Result", (), {"returncode": 0, "stdout": stdout, "stderr": b""})()

                result = REFRESH.publish_private(run_dir, "draft-id", private=True, runner=retry)
            self.assertTrue(result["published"])
            self.assertNotEqual(Path(result["attempt_dir"]), old_attempt)
            self.assertEqual((old_attempt / "update.stdout.log").read_bytes(), b"update failed")
            self.assertEqual(len(list((run_dir / "publications").glob("*"))), 2)

    def test_memory_sidecars_must_match_capture_record(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            sidecar_path = run_dir / "memory/memory-nas-lu.json"
            sidecar = json.loads(sidecar_path.read_text())
            sidecar["source_sha256"] = "0" * 64
            write_json(sidecar_path, sidecar)
            with self.assertRaisesRegex(ValueError, "does not match"):
                REFRESH.validate_run_bundle(run_dir)

    def test_memory_provenance_and_commands_are_checked(self) -> None:
        mutations = (
            ("verified_assets", lambda memory: memory["published_manifest"]["verified_assets"].update(
                {"clang-dumper-linux-x64": "0" * 64})),
            ("observation assets", lambda memory: memory["observations"][0]["release_asset_sha256"].update(
                {"clang-dumper-ast-wire.pb": "0" * 64})),
            ("JVM options", lambda memory: memory["jvm_options"].__setitem__(1, "-Xmx8g")),
            ("command arguments", lambda memory: memory["observations"][0]["command"].__setitem__(9, "-Xmx8g")),
        )
        for label, mutate in mutations:
            with self.subTest(label=label), tempfile.TemporaryDirectory() as temp:
                run_dir = completed_bundle(Path(temp))
                memory_path = run_dir / "memory/memory.json"
                memory = json.loads(memory_path.read_text())
                mutate(memory)
                write_json(memory_path, memory)
                with self.assertRaises(ValueError):
                    REFRESH.validate_run_bundle(run_dir)

    def test_runtime_vitest_sequencer_must_use_resolved_file_url(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            results_path = run_dir / "runtime/results.json"
            runtime = json.loads(results_path.read_text())
            runtime["js_runtime_exports"]["vitest_node_url"] = "vitest/node"
            write_json(results_path, runtime)
            with self.assertRaisesRegex(ValueError, "file URL"):
                REFRESH.validate_run_bundle(run_dir)

    def test_vitest_runner_config_loader_is_pinned_in_runner_source(self) -> None:
        source = 'command = ["--config", str(config), "--reporter=json"]'
        patched = REFRESH.support.pin_vitest_runner_config_loader(source)
        self.assertIn('"--configLoader", "runner"', patched)
        with self.assertRaisesRegex(RuntimeError, "shared App runner"):
            REFRESH.support.pin_vitest_runner_config_loader(source.replace("--config", "--configuration"))

    def test_runtime_rejects_vitest_commands_without_runner_loader(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            results_path = run_dir / "runtime/results.json"
            runtime = json.loads(results_path.read_text())
            row = next(row for row in runtime["observations"]
                       if row["phase"] == "app" and row["suite"] == "clava-js")
            write_json(Path(row["run_dir"]) / "command.json", {"argv": ["vitest", "run"]})
            write_json(results_path, runtime)
            with self.assertRaisesRegex(ValueError, "runner config loader"):
                REFRESH.validate_run_bundle(run_dir)

    def test_non_protobuf_control_rows_are_identical(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            bundle = REFRESH.validate_run_bundle(run_dir)
            app_rows, wall_rows = REFRESH.REPORT.combine_rows(bundle["prior"], bundle["report_rows"])
            hashes = REFRESH.verify_control_preservation(bundle["prior"], app_rows, wall_rows)
            self.assertEqual(hashes, bundle["controls_sha256"])
            changed = copy.deepcopy(app_rows)
            text_row = next(row for row in changed if row["stage"] == "ccache-text")
            text_row["app_elapsed_ms"] += 1
            with self.assertRaisesRegex(ValueError, "frozen non-Protobuf"):
                REFRESH.verify_control_preservation(bundle["prior"], changed, wall_rows)

    def test_report_rejects_paths_private_urls_and_secret_values(self) -> None:
        for value in (
            "<!doctype html><p>/home/private/results.json</p>",
            "<!doctype html><p>https://draftlink.lmsousa.workers.dev/d/private</p>",
            "<!doctype html><p>Bearer abcdefghijklmnopqrstuvwxyz</p>",
        ):
            with self.assertRaises(ValueError):
                REFRESH.validate_report_html(value)

    def test_render_does_not_overwrite_existing_output(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            run_dir = completed_bundle(Path(temp))
            report = REFRESH.render_run(run_dir)
            original = report.read_bytes()
            self.assertEqual(REFRESH.render_run(run_dir), report)
            report.write_bytes(b"keep prior evidence")
            with self.assertRaisesRegex(ValueError, "refusing to overwrite"):
                REFRESH.render_run(run_dir)
            self.assertEqual(report.read_bytes(), b"keep prior evidence")
            report.write_bytes(original)


if __name__ == "__main__":
    unittest.main()
