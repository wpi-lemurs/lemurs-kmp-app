# KMP Metrics

This folder contains a lightweight script to quantify migration progress in this repo.

## Metrics

- **Business-rule migration into commonMain**
  - LOC-weighted across:
    - `data`
    - `survey`
    - `health`
    - `ui/viewmodel`
  - Formula: `shared_loc / (shared_loc + platform_loc) * 100`

- **UI screen reusability via Compose Multiplatform**
  - Reads route mappings from `composeApp/src/commonMain/kotlin/com/lemurs/lemurs_app/App.kt`
  - Counts routes backed by `expect/actual` as hybrid (`0.5`)
  - Formula: `(shared_routes + 0.5 * hybrid_routes) / total_routes * 100`

## Run

```bash
python3 scripts/metrics/compute_kmp_metrics.py
```

The script prints JSON so it can be consumed by CI, docs, or dashboards.

