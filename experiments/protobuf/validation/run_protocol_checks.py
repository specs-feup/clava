#!/usr/bin/env python3
"""Invoke production malformed/protocol validation tests and retain evidence.

The production command owns the actual protobuf parser checks.  This wrapper
records which required categories were requested and never substitutes a text
parser or a permissive fallback.  Command substitutions: ``{work}`` and
``{output}``.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
from pathlib import Path
import shlex
import subprocess
from typing import Any


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--command", required=True, help="Production test/helper command template")
    parser.add_argument("--checks", default="required,truncated,version,reference", help="Comma-separated categories delegated to production tests")
    parser.add_argument("--output-root", type=Path, required=True)
    args = parser.parse_args()
    args.output_root.mkdir(parents=True, exist_ok=False)
    values = {"work": str(args.output_root), "output": str(args.output_root / "production-checks.json")}
    command = [token.format(**values) for token in shlex.split(args.command)]
    log = args.output_root / "production-checks.log"
    started = dt.datetime.now(dt.timezone.utc)
    with log.open("w") as output:
        process = subprocess.run(command, stdout=output, stderr=subprocess.STDOUT, check=False, text=True)
    summary: dict[str, Any] = {"created_utc": started.isoformat(), "checks": [item for item in args.checks.split(",") if item], "return_code": process.returncode, "command": command, "log": str(log), "delegated_to_production": True}
    (args.output_root / "protocol-checks.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, indent=2))
    return process.returncode


if __name__ == "__main__":
    raise SystemExit(main())
