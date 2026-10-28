#!/usr/bin/env python3
"""Verify the language/linker assumptions behind cross-TU regression tests."""
import argparse
import json
from pathlib import Path
import subprocess
import tempfile


def verify(compiler):
    def run(arguments, expected_success):
        process = subprocess.run([compiler, *arguments], text=True, capture_output=True)
        if (process.returncode == 0) != expected_success:
            raise AssertionError(f"Unexpected compiler result: {arguments}\n{process.stdout}\n{process.stderr}")
        return {"arguments": arguments, "exit_code": process.returncode, "diagnostics": process.stderr.strip()}

    with tempfile.TemporaryDirectory(prefix="clava-ctu06-") as temporary:
        root = Path(temporary)
        def source(name, code):
            path = root / name
            path.write_text(code)
            return str(path)
        declaration = source("declaration.cpp", "int choose(int); int use(){return choose(1);}\n")
        variadic = source("variadic.cpp", "int choose(int,...){return 2;}\n")
        main = source("main.cpp", "int use(); int main(){return use()==2 ? 0 : 1;}\n")
        objects = [str(root / (name + ".o")) for name in ("declaration", "variadic", "main")]
        compilation = [run(["-std=c++20", "-c", path, "-o", obj], True)
                       for path, obj in zip((declaration, variadic, main), objects)]
        link = run([*objects, "-o", str(root / "negative")], False)
        if "choose" not in link["diagnostics"]:
            raise AssertionError("The negative link failed for a reason other than the missing choose function")
        fixed = source("matching.cpp", "int choose(int){return 2;}\n")
        positive = run(["-std=c++20", declaration, fixed, main, "-o", str(root / "positive")], True)
        subprocess.run([str(root / "positive")], check=True)
        ambiguous = source("ambiguous.cpp", "int choose(int); int choose(int,...); int use(){return choose(1);}\n")
        overload = run(["-std=c++20", "-fsyntax-only", ambiguous], False)
        if "ambiguous" not in overload["diagnostics"]:
            raise AssertionError("The overload compile failed for a reason other than ambiguity")
        result = {"compiler": subprocess.run([compiler, "--version"], capture_output=True, text=True, check=True).stdout.splitlines()[0],
                  "individual_translation_units": compilation, "missing_fixed_arity_definition": link,
                  "matching_definition_control": positive, "both_overloads_visible": overload}
        # Keep the report reusable without retaining random temporary directory names.
        return json.loads(json.dumps(result).replace(str(root), "<fixture>"))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--compiler", action="append", required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    report = {"CTU06": [verify(compiler) for compiler in args.compiler]}
    text = json.dumps(report, indent=2) + "\n"
    if args.output:
        args.output.write_text(text)
    else:
        print(text, end="")
