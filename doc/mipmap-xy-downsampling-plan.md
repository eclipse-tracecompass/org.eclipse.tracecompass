# Plan: Mipmap-Based XY Downsampling for Trace Compass

## Problem Statement

The current XY data provider pipeline uses trivial downsampling: the viewer requests N uniformly-spaced timestamps and each data provider returns one value per timestamp. This is equivalent to point-sampling/decimation — peaks, spikes, and signal features between sample points are silently lost.

## Goal

Replace trivial downsampling with max-per-bucket aggregation backed by pre-computed mipmap levels in the state system. This guarantees visual correctness (spikes are always visible at every zoom level) with O(log N) query performance per bucket.

## Key Design Decisions

### Max-Only Per Bucket

`fetchXY` returns the **max** value in each bucket:

- **No series model changes** — still one Y value per X point
- **No viewer/SWTChart changes** — same array shape, just better values
- **No API changes** — same `fetchXY` contract and response type
- **Spikes never lost** — the max in each bucket is always preserved at every zoom level
- **Zoom-in works naturally** — as bucket width shrinks, max converges to actual value; at full zoom you see raw data

### Power-of-10 Mipmap Levels

Mipmap levels use decimal time boundaries:

```
Level 0: raw data (1 ns resolution)
Level 1: max per 10 ns
Level 2: max per 100 ns
Level 3: max per 1 µs
Level 4: max per 10 µs
Level 5: max per 100 µs
Level 6: max per 1 ms
...
```

This matches the natural time units users think in and aligns cleanly with zoom levels.

**Note on existing `TmfMipmapFeature`**: The current implementation uses a **count-based resolution** — it promotes to the next level after N state intervals accumulate at the current level (not time-based). The resolution parameter means "aggregate every N intervals into one mipmap entry." For power-of-10 time-based levels, we need a new `TimeMipmapFeature` that promotes based on time window width (10ns, 100ns, ...) rather than interval count.

### Auto-Detection in Common Data Provider Base

The mipmap query logic lives in `AbstractTreeCommonXDataProvider.fetchXY()` (internal). It auto-detects mipmap sub-attributes using the existing `fIdToQuark` BiMap that already maps entry IDs → state system quarks:

```java
// AbstractTreeDataProvider already has:
private final BiMap<Long, Integer> fIdToQuark = HashBiMap.create();
```

For each `IYModel` returned by the child's `getYSeriesModels()`:
1. `model.getId()` → look up in `fIdToQuark` → get the quark
2. Check if `ss.optQuarkRelative(quark, "max")` exists
3. If yes: replace Y values with `queryRangeMax` per bucket
4. If no: keep as-is (backward compatible)

No per-data-provider mipmap code. No scatter plot concern — scatter plots don't use this base class path.

### Opt-In at the State Provider Level

State providers opt in by extending `AbstractXYStateProvider` and using its helpers. Only quarks written through the mipmap helpers get mipmap attributes.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│ Trace Ingestion (Build Time)                                        │
│                                                                     │
│  State Provider extends AbstractXYStateProvider (provisional API)    │
│       │                                                             │
│       ├─ modifyMipmapAttribute(ts, value, quark)                    │
│       │       │                                                     │
│       │       ▼                                                     │
│       │  TimeMipmapFeature (levels: 10ns, 100ns, 1µs, 10µs, ...)   │
│       │       │                                                     │
│       │       ▼                                                     │
│       │  State System: quark/max/1, quark/max/2, ...                │
│       │                                                             │
└───────┼─────────────────────────────────────────────────────────────┘
        │
┌───────┼─────────────────────────────────────────────────────────────┐
│ Query Time                                                          │
│       │                                                             │
│  AbstractTreeCommonXDataProvider.fetchXY()                           │
│       │                                                             │
│       ├─ Calls child's getYSeriesModels() → gets IYModel list       │
│       ├─ For each model:                                            │
│       │     quark = fIdToQuark.get(model.getId())                   │
│       │     if ss.optQuarkRelative(quark, "max") exists:            │
│       │       for each bucket [t_i, t_{i+1}]:                       │
│       │         y[i] = TmfStateSystemOperations.queryRangeMax(      │
│       │                    ss, t_i, t_{i+1}, quark)                 │
│       │                                                             │
│       ▼                                                             │
│  ISeriesModel (unchanged) → Viewer (unchanged)                      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Phases

### Phase 1: `AbstractXYStateProvider` (Provisional API)

**Location**: `tmf/org.eclipse.tracecompass.tmf.core` (provisional package)

New base class extending `AbstractTmfStateProvider` with ergonomic helpers:

| Helper Method | Replaces |
|---------------|----------|
| `modifyMipmapAttribute(ts, long, quark)` | `ss.modifyAttribute(ts, value, quark)` |
| `modifyMipmapAttribute(ts, double, quark)` | `ss.modifyAttribute(ts, value, quark)` |
| `incrementMipmapAttributeLong(ts, quark, inc)` | `StateSystemBuilderUtils.incrementAttributeLong()` |
| `incrementMipmapAttributeDouble(ts, quark, inc)` | `StateSystemBuilderUtils.incrementAttributeDouble()` |

Key details:
- Mipmap resolution: **power-of-10 time-based** (10ns, 100ns, 1µs, ...)
- Default feature: **MAX**
- `dispose()` flushes incomplete mipmap levels
- **Provisional API** — available for third-party plugins

### Phase 1b: `TimeMipmapFeature` (Internal)

**Location**: `tmf/org.eclipse.tracecompass.tmf.core` (internal mipmap package)

New mipmap feature implementation that promotes levels based on **time window width** rather than interval count:

```java
class TimeMipmapFeature extends TmfMipmapFeature {
    // Level 1 covers 10ns windows, level 2 covers 100ns, etc.
    // Promotes when the accumulated interval spans a full window at the current level
}
```

The existing `TmfMipmapFeature` promotes after N intervals; `TimeMipmapFeature` promotes when `endTime - startTime >= 10^level` ns.

### Phase 2: Auto-Detect Mipmap in Common Data Provider Base

**Location**: `tmf/org.eclipse.tracecompass.tmf.core` (internal)

Modify `AbstractTreeCommonXDataProvider.fetchXY()` to auto-enhance results:

```java
@Override
public final TmfModelResponse<ITmfXyModel> fetchXY(...) {
    // ... existing validation ...
    
    Collection<IYModel> yModels = getYSeriesModels(ss, fetchParameters, monitor);
    
    // Auto-enhance: replace point-sampled values with max-per-bucket
    // where mipmap attributes are available
    yModels = MipmapXYQueryHelper.enhanceWithMipmap(ss, fIdToQuark, yModels, filter.getTimesRequested());
    
    return TmfXyResponseFactory.create(getTitle(), filter.getTimesRequested(), yModels, complete);
}
```

`MipmapXYQueryHelper` is **internal** — called only by the abstract base class:

```java
class MipmapXYQueryHelper {
    /**
     * For each IYModel whose quark has mipmap sub-attributes,
     * replace Y values with queryRangeMax per bucket.
     * Models without mipmap are returned unchanged.
     */
    static Collection<IYModel> enhanceWithMipmap(
        ITmfStateSystem ss,
        BiMap<Long, Integer> idToQuark,
        Collection<IYModel> models,
        long[] bucketBoundaries);
}
```

Same integration in `AbstractTreeGenericXYCommonXDataProvider.fetchXY()`.

### Phase 3: Migrate CounterStateProvider (Proof of Concept)

**Location**: `analysis/org.eclipse.tracecompass.analysis.counters.core`

Changes:
1. `extends AbstractTmfStateProvider` → `extends AbstractXYStateProvider`
2. Replace `StateSystemBuilderUtils.incrementAttributeLong(ss, ts, quark, value)` with `incrementMipmapAttributeLong(ts, quark, value)`
3. Replace `ss.modifyAttribute(ts, value, quark)` with `modifyMipmapAttribute(ts, value, quark)`
4. Bump `getVersion()`

The counter data provider gets mipmap behavior **for free** — no data provider changes needed.

### Phase 4: Migrate Additional State Providers

| Provider | Package | Complexity |
|----------|---------|-----------|
| `KernelMemoryStateProvider` | `analysis.os.linux.core` | Low |
| `KernelCpuUsageStateProvider` | `analysis.os.linux.core` | Low |
| `KernelContextSwitchStateProvider` | `analysis.os.linux.core` | Low |
| `InputOutputStateProvider` | `analysis.os.linux.core` | Medium |

Each migration: change `extends`, swap write calls, bump version. No data provider changes.

### Phase 5 (Future): Min-Max Bands and LTTB

Optional enhancements once max-only is proven:
- Return both min and max for full envelope rendering
- Apply LTTB for perceptually optimal point selection
- Additive — don't affect the max-only baseline

## File Changes Summary

| File | Change |
|------|--------|
| `tmf.core/.../provisional/.../AbstractXYStateProvider.java` | **NEW** — provisional API base class |
| `tmf.core/.../mipmap/TimeMipmapFeature.java` | **NEW** — time-based level promotion |
| `tmf.core/.../mipmap/MipmapXYQueryHelper.java` | **NEW** — internal auto-detect + query |
| `tmf.core/.../xy/AbstractTreeCommonXDataProvider.java` | Integrate mipmap auto-enhancement in `fetchXY()` |
| `tmf.core/.../genericxy/AbstractTreeGenericXYCommonXDataProvider.java` | Same integration |
| `analysis.counters.core/.../CounterStateProvider.java` | Modify extends + helper calls + version bump |
| `analysis.os.linux.core/.../KernelMemoryStateProvider.java` | Modify extends + helper calls + version bump |
| `analysis.os.linux.core/.../KernelCpuUsageStateProvider.java` | Modify extends + helper calls + version bump |
| `analysis.os.linux.core/.../KernelContextSwitchStateProvider.java` | Modify extends + helper calls + version bump |

## Mipmap Level Structure (Power-of-10, Time-Based)

```
quark (base attribute — raw data)
├── max
│   ├── 1   (max per 10 ns window)
│   ├── 2   (max per 100 ns window)
│   ├── 3   (max per 1 µs window)
│   ├── 4   (max per 10 µs window)
│   ├── 5   (max per 100 µs window)
│   ├── 6   (max per 1 ms window)
│   ├── 7   (max per 10 ms window)
│   ├── 8   (max per 100 ms window)
│   ├── 9   (max per 1 s window)
│   └── ... (as needed by trace duration)
```

Number of levels = ceil(log₁₀(trace_duration_ns)). A 100-second trace needs 11 levels.

## ID-to-Quark Mapping

The mapping from `IYModel.getId()` to state system quark is already solved:

```
AbstractTreeDataProvider.fIdToQuark : BiMap<Long, Integer>
```

- Built during `fetchTree()` / `getTree()` when the provider calls `getId(quark)`
- Available in `fetchXY()` since tree is always fetched first
- `MipmapXYQueryHelper` receives this map and uses it to look up quarks for each model

No new mapping infrastructure needed.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Storage overhead | ~15% extra (max-only, power-of-10 levels) | Only quarks written through mipmap helpers get levels |
| Build time regression | ~10-15% slower ingestion | Benchmark; max-only is lightest option |
| Forced state system rebuild | One-time cost for users | Document in release notes; automatic on version mismatch |
| Max-only is optimistic | Line hugs peaks, not representative of average | Acceptable for spike detection; avg/min available as future enhancement |
| Existing `TmfMipmapFeature` is count-based | Doesn't match time-based scheme | New `TimeMipmapFeature`; existing feature unchanged for backward compat |
| `fIdToQuark` may not have entry for all model IDs | Some models created without `getId(quark)` | Graceful fallback — if quark not found, skip mipmap enhancement |

## Success Criteria

1. Counter XY view shows no missed spikes when zoomed out on a large trace
2. Zooming in on a spike smoothly converges to the actual spike value
3. Query time for 1000-bucket max on a 10M-event trace is < 100ms
4. No regression in existing XY view behavior for non-migrated providers
5. Storage overhead < 20% of baseline state history size
6. Zero mipmap-related code in individual data provider implementations
