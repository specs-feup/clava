from __future__ import annotations

import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

import group_workload as workload


CLAVA_ROOT = Path(__file__).resolve().parents[2]
REQUIRED_ENV = (
    "GROUPED_TEST_CORPUS",
    "GROUPED_TEST_CLASSES",
    "GROUPED_TEST_RUNTIME_LIB",
    "CLANG_DUMPER_TOOL",
)


@unittest.skipUnless(all(os.environ.get(name) for name in REQUIRED_ENV),
                     "set grouped replay environment variables to run the integration regression")
class GraphDigestReferenceClosureIntegrationTests(unittest.TestCase):
    def test_legacy_compatibility_edges_preserve_graph_and_outputs(self) -> None:
        corpus = Path(os.environ["GROUPED_TEST_CORPUS"])
        _, groups, _ = workload.verify_workload(corpus)
        group = next(group for group in groups
                     if group["suite"] == "java"
                     and any(Path(path).name == "for.cpp"
                             for path in group.get("source_paths", [])))
        pseudo_group = next(group for group in groups
                            if group["suite"] == "java"
                            and any(Path(path).name == "pseudo_destructor.cpp"
                                    for path in group.get("source_paths", [])))

        classes = Path(os.environ["GROUPED_TEST_CLASSES"])
        runtime_lib = Path(os.environ["GROUPED_TEST_RUNTIME_LIB"])
        classpath = f"{classes}:{runtime_lib}/*"
        java = os.environ.get("JAVA", "java")
        with tempfile.TemporaryDirectory(prefix="grouped-legacy-compatibility-") as temporary:
            root = Path(temporary)
            temp_dir = root / "tmp"
            temp_dir.mkdir()
            for selected_group in (group, pseudo_group):
                input_id = f"java-group-{int(selected_group['ordinal']):04d}"
                results = {}
                for protocol in ("text", "protobuf"):
                    schedule = root / f"{input_id}-{protocol}.jsonl"
                    output = root / f"{input_id}-{protocol}-result.jsonl"
                    operation = workload.build_operation(
                        selected_group, root / f"{input_id}-{protocol}",
                        protocol, "direct", "fidelity", 0,
                    )
                    schedule.write_text(json.dumps(operation, separators=(",", ":")) + "\n",
                                        encoding="utf-8")
                    environment = os.environ.copy()
                    environment.pop("JAVA_TOOL_OPTIONS", None)
                    environment["CCACHE_DISABLE"] = "true"
                    environment["TMPDIR"] = str(temp_dir)
                    subprocess.run(
                        [java, f"-Djava.io.tmpdir={temp_dir}", "-cp", classpath,
                         "StandaloneParseRunner", "--schedule", str(schedule), "--output", str(output)],
                        cwd=CLAVA_ROOT,
                        env=environment,
                        check=True,
                        capture_output=True,
                        text=True,
                    )
                    results[protocol] = json.loads(output.read_text(encoding="utf-8").strip())
                    self.assertTrue(results[protocol]["valid"], results[protocol].get("error"))

                for field in ("complete_graph_sha256", "generated_code_sha256", "generated_code_bytes",
                              "ast_node_count", "ast_node_kind_counts_sha256"):
                    self.assertEqual(results["text"][field], results["protobuf"][field],
                                     f"{input_id} differs in {field}")

    def test_multitu_reference_closure_and_record_edge_sensitivity(self) -> None:
        corpus = Path(os.environ["GROUPED_TEST_CORPUS"])
        _, groups, _ = workload.verify_workload(corpus)
        group = next(group for group in groups
                     if group["suite"] == "clava-js"
                     and len(group.get("source_paths", [])) > 1
                     and any(Path(path).name == "array_access.cpp"
                             for path in group.get("source_paths", [])))

        classes = Path(os.environ["GROUPED_TEST_CLASSES"])
        runtime_lib = Path(os.environ["GROUPED_TEST_RUNTIME_LIB"])
        classpath = f"{classes}:{runtime_lib}/*"
        java = os.environ.get("JAVA", "java")
        results = {}
        with tempfile.TemporaryDirectory(prefix="graph-digest-reference-closure-") as temporary:
            root = Path(temporary)
            temp_dir = root / "tmp"
            temp_dir.mkdir()
            for protocol in ("text", "protobuf"):
                schedule = root / f"{protocol}.jsonl"
                output = root / f"{protocol}-result.jsonl"
                operation = workload.build_operation(
                    group, root / protocol, protocol, "direct", "fidelity", 0,
                )
                operation["verify_record_reference_sensitivity"] = True
                schedule.write_text(json.dumps(operation, separators=(",", ":")) + "\n",
                                    encoding="utf-8")

                environment = os.environ.copy()
                environment.pop("JAVA_TOOL_OPTIONS", None)
                environment["CCACHE_DISABLE"] = "true"
                environment["TMPDIR"] = str(temp_dir)
                subprocess.run(
                    [java, f"-Djava.io.tmpdir={temp_dir}", "-cp", classpath,
                     "StandaloneParseRunner", "--schedule", str(schedule), "--output", str(output)],
                    cwd=CLAVA_ROOT,
                    env=environment,
                    check=True,
                    capture_output=True,
                    text=True,
                )
                result = json.loads(output.read_text(encoding="utf-8").strip())
                self.assertTrue(result["valid"], result.get("error"))
                self.assertTrue(result["record_reference_sensitivity_passed"])
                results[protocol] = result

        self.assertEqual(results["text"]["complete_graph_sha256"],
                         results["protobuf"]["complete_graph_sha256"])

    def test_omp_clause_kind_and_code_are_hashed(self) -> None:
        corpus = Path(os.environ["GROUPED_TEST_CORPUS"])
        _, groups, _ = workload.verify_workload(corpus)
        group = next(group for group in groups
                     if group["suite"] == "clava-js"
                     and any(Path(path).name == "omp.cpp"
                             for path in group.get("source_paths", [])))

        classes = Path(os.environ["GROUPED_TEST_CLASSES"])
        runtime_lib = Path(os.environ["GROUPED_TEST_RUNTIME_LIB"])
        classpath = f"{classes}:{runtime_lib}/*"
        java = os.environ.get("JAVA", "java")
        results = {}
        with tempfile.TemporaryDirectory(prefix="graph-digest-omp-clause-") as temporary:
            root = Path(temporary)
            temp_dir = root / "tmp"
            temp_dir.mkdir()
            for protocol in ("text", "protobuf"):
                schedule = root / f"{protocol}.jsonl"
                output = root / f"{protocol}-result.jsonl"
                operation = workload.build_operation(
                    group, root / protocol, protocol, "direct", "fidelity", 0,
                )
                operation["verify_omp_clause_sensitivity"] = True
                schedule.write_text(json.dumps(operation, separators=(",", ":")) + "\n",
                                    encoding="utf-8")

                environment = os.environ.copy()
                environment.pop("JAVA_TOOL_OPTIONS", None)
                environment["CCACHE_DISABLE"] = "true"
                environment["TMPDIR"] = str(temp_dir)
                subprocess.run(
                    [java, f"-Djava.io.tmpdir={temp_dir}", "-cp", classpath,
                     "StandaloneParseRunner", "--schedule", str(schedule), "--output", str(output)],
                    cwd=CLAVA_ROOT,
                    env=environment,
                    check=True,
                    capture_output=True,
                    text=True,
                )
                result = json.loads(output.read_text(encoding="utf-8").strip())
                self.assertTrue(result["valid"], result.get("error"))
                self.assertTrue(result["omp_clause_sensitivity_passed"])
                results[protocol] = result

        self.assertEqual(results["text"]["complete_graph_sha256"],
                         results["protobuf"]["complete_graph_sha256"])


if __name__ == "__main__":
    unittest.main()
