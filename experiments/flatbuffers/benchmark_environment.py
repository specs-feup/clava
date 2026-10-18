"""Small helpers for benchmark processes that must not resolve ccache."""

from __future__ import annotations

from pathlib import Path
import os
import shutil


DEFAULT_REQUIRED_EXECUTABLES = ("npm", "node", "java", "git", "df")


def make_path_without_ccache(
    output_root: Path,
    required_executables: tuple[str, ...] = DEFAULT_REQUIRED_EXECUTABLES,
) -> tuple[str, Path]:
    """Return a PATH link farm that excludes ccache and all aliases to it.

    PATH may contain a directory such as ``/usr/bin`` where ccache lives next
    to tools a benchmark still needs. A link farm keeps those tools available
    while omitting both a file named ``ccache`` and symlink aliases whose
    resolved target is the ccache binary.
    """

    bin_root = output_root / "path-without-ccache"
    bin_root.mkdir(parents=True)
    source_dirs: list[Path] = []
    for raw_path in os.environ.get("PATH", "").split(os.pathsep):
        if not raw_path:
            continue
        candidate = Path(raw_path)
        if candidate.is_dir() and candidate not in source_dirs:
            source_dirs.append(candidate)

    for source_dir in source_dirs:
        try:
            entries = sorted(source_dir.iterdir(), key=lambda item: item.name)
        except OSError:
            continue
        for source in entries:
            if source.name == "ccache" or (bin_root / source.name).exists():
                continue
            try:
                resolved = source.resolve(strict=False)
                if resolved.name == "ccache":
                    continue
                (bin_root / source.name).symlink_to(resolved)
            except OSError:
                continue

    path = str(bin_root)
    if shutil.which("ccache", path=path) is not None:
        raise RuntimeError(f"ccache unexpectedly resolves from the filtered PATH: {path}")
    for executable in required_executables:
        if shutil.which(executable, path=path) is None:
            raise RuntimeError(f"required executable {executable!r} is missing from the filtered PATH")
    return path, bin_root
