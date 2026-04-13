#!/usr/bin/env python3
"""Compute KMP migration metrics for this repository.

Outputs two percentages:
1) Business-rule migration into commonMain (LOC-weighted in selected logic folders)
2) UI screen reusability via Compose Multiplatform (route-weighted, with hybrid expect/actual = 0.5)
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


REPO_ROOT = Path(__file__).resolve().parents[2]
APP_KT = REPO_ROOT / "composeApp/src/commonMain/kotlin/com/lemurs/lemurs_app/App.kt"
SRC_BASE = REPO_ROOT / "composeApp/src"
PKG_BASE = Path("kotlin/com/lemurs/lemurs_app")

BUSINESS_FOLDERS = [
    "data",
    "survey",
    "health",
    "ui/viewmodel",
]


@dataclass
class SourceSetStats:
    source_set: str
    files: int
    loc: int


def kotlin_files_under(base: Path) -> Iterable[Path]:
    if not base.exists():
        return []
    return [p for p in base.rglob("*.kt") if p.is_file()]


def line_count(path: Path) -> int:
    with path.open("r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def compute_business_rule_stats() -> dict:
    by_source_set: dict[str, SourceSetStats] = {}

    for source_set in ["commonMain", "androidMain", "iosMain"]:
        base = SRC_BASE / source_set / PKG_BASE
        files: list[Path] = []
        for folder in BUSINESS_FOLDERS:
            files.extend(kotlin_files_under(base / folder))
        files = sorted(set(files))
        loc = sum(line_count(p) for p in files)
        by_source_set[source_set] = SourceSetStats(source_set, len(files), loc)

    shared = by_source_set["commonMain"].loc
    platform = by_source_set["androidMain"].loc + by_source_set["iosMain"].loc
    denom = shared + platform
    shared_pct = (shared / denom * 100.0) if denom else 0.0

    return {
        "folders": BUSINESS_FOLDERS,
        "counts": {
            k: {"files": v.files, "loc": v.loc}
            for k, v in by_source_set.items()
        },
        "shared_loc": shared,
        "platform_loc": platform,
        "shared_percent": round(shared_pct, 1),
        "formula": "shared_loc / (shared_loc + platform_loc) * 100",
    }


def parse_routes_from_app() -> list[str]:
    text = APP_KT.read_text(encoding="utf-8")
    # Matches: LemurScreen.X.name -> SomeScreen(
    routes = re.findall(r"LemurScreen\.(\w+)\.name\s*->\s*[A-Za-z0-9_]+\(", text)
    return routes


def has_expect_screen(screen_name: str) -> bool:
    common_file = SRC_BASE / "commonMain" / PKG_BASE / "ui/screens" / f"{screen_name}.kt"
    if not common_file.exists():
        return False
    text = common_file.read_text(encoding="utf-8", errors="ignore")
    return bool(re.search(r"\bexpect\s+fun\s+" + re.escape(screen_name) + r"\s*\(", text))


def compute_ui_reuse_stats() -> dict:
    routes = parse_routes_from_app()
    total = len(routes)

    hybrid = 0
    shared = 0
    for route in routes:
        screen_fn = f"{route}Screen" if route not in {"History"} else "ProgressHistoryScreen"
        # Route-to-function mapping in App.kt is not always 1:1 by name.
        # We detect hybrid only when the actual function file is expect-based.
        # So we parse function directly from mapping again.

    text = APP_KT.read_text(encoding="utf-8")
    pairs = re.findall(r"LemurScreen\.(\w+)\.name\s*->\s*([A-Za-z0-9_]+)\(", text)

    for _route, function_name in pairs:
        if has_expect_screen(function_name):
            hybrid += 1
        else:
            shared += 1

    weighted_reusable = shared + 0.5 * hybrid
    reusable_pct = (weighted_reusable / total * 100.0) if total else 0.0
    strict_shared_pct = (shared / total * 100.0) if total else 0.0

    return {
        "total_routes": total,
        "shared_routes": shared,
        "hybrid_expect_actual_routes": hybrid,
        "weighted_reusable_routes": round(weighted_reusable, 1),
        "weighted_reuse_percent": round(reusable_pct, 1),
        "strict_shared_percent": round(strict_shared_pct, 1),
        "formula_weighted": "(shared_routes + 0.5 * hybrid_routes) / total_routes * 100",
    }


def main() -> None:
    business = compute_business_rule_stats()
    ui = compute_ui_reuse_stats()

    result = {
        "repo": str(REPO_ROOT),
        "business_rule_migration": business,
        "ui_reusability": ui,
    }

    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()

