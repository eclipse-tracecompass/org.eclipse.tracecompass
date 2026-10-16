# Plan: Mipmap-Based XY Downsampling for Trace Compass

## Status

**Infrastructure landed, enhancement disabled by default (opt-in).** The query
helper, the base-class integration points, and the mipmap feature code exist,
but XY mipmap enhancement is **off by default** and no shipped data provider
opts in yet. This is deliberate: the first attempt at auto-enabling the feature
was reverted because it broke the Counters view and produced invisible lines.
The sections below document the corrected design and, importantly, the
**correctness constraints** that any consumer must respect before opting in.

## Problem Statement

The current XY data provider pipeline uses trivial downsampling: the viewer
requests N uniformly-spaced timestamps and each data provider returns one value
per timestamp. This is equivalent to point-sampling/decimation — peaks, spikes,
and signal features between sample points are silently lost.

## Goal

Offer max-per-bucket aggregation backed by pre-computed mipmap levels in the
state system, so that spikes remain visible at every zoom level with O(log N)
query performance per bucket — **for the data providers where that is
semantically valid**, and only when they explicitly opt in.

## Correctness Constraints (read this first)

Max-per-bucket downsampling replaces the value at requested timestamp `i` with
the maximum of the underlying state attribute over the bucket
`[t[i], t[i+1])`. This is only correct when **the Y value the provider displays
is the raw instantaneous value of a single state-system quark**. It is
**incorrect** whenever the provider transforms the raw state before displaying
it:

- **Differential counters** — `CounterDataProvider` defaults to *differential*
  mode: it stores a cumulative (monotonic) total in the quark and plots the
  *delta* between consecutive samples. The maximum of a monotonic counter over
  a bucket is just its value at the end of the bucket, which has nothing to do
  with the per-interval delta. Enhancing differential counters corrupts the
  chart.
- **Rate-based views** — `CpuUsageDataProvider` plots a *rate* (`% CPU`)
  computed from `getCpuUsageInRange(prevTime, time)` over each interval, not a
  raw quark value. The maximum of the raw cumulative on-CPU time is meaningless
  as a rate. CPU usage must never be enhanced.

Consequence: enhancement cannot be a blanket, auto-detected behavior. It must be
**opt-in per data provider**, and a provider may only opt in if every Y value it
returns is the raw instantaneous value of the quark mapped through
`getIdToQuark()`.

## Attribute-Tree Constraint

Several data providers identify their data series as the **leaf** attributes of
the state system (`ss.getSubAttributes(quark, false).isEmpty()`) and build their
tree by walking `getSubAttributes`. `CounterDataProvider` does both. Therefore,
storing mipmap data as child attributes *directly under the data quark* (e.g.
`quark/max/1`) is harmful:

- The data quark stops being a leaf, so leaf-based series selection filters it
  out — the line disappears.
- The `max` attribute (and its per-level children) show up as bogus tree
  entries.

Any mipmap storage scheme must therefore keep the data quark a leaf from the
point of view of these providers — for example by storing mipmap levels in a
**separate sibling subtree** keyed by the base quark, rather than as children of
the base quark. This is a prerequisite before re-enabling the write side for a
leaf-based provider such as Counters.

## Current Implementation

### Opt-in enhancement in the common data provider base

Enhancement is gated behind a hook on `AbstractTreeDataProvider`:

```java
/**
 * Opt-in (default false). Only correct when every returned Y value is the
 * raw instantaneous state value of the quark from getIdToQuark().
 */
protected boolean isMipmapEnhanced() {
    return false;
}
```

Both `AbstractTreeCommonXDataProvider.fetchXY()` and
`AbstractTreeGenericXYCommonXDataProvider.fetchXY()` only call the enhancement
when the provider opts in:

```java
if (isMipmapEnhanced()) {
    yModels = MipmapXYQueryHelper.enhanceWithMipmap(ss, getIdToQuark(), yModels, times);
}
```

Because the default is `false`, every shipped provider — including
`CpuUsageDataProvider` and `CounterDataProvider` — is unchanged: the enhancement
code is never invoked for them.

### `MipmapXYQueryHelper` (internal)

For each `IYModel`:

1. `model.getId()` → look up in `getIdToQuark()` → get the quark (skip if
   missing or negative).
2. If the quark has mipmap data available, replace the Y values with
   max-per-bucket values.
3. Otherwise return the model unchanged.

Key contract (this was the source of the "invisible lines" bug and is now
fixed): the enhanced series has **exactly one value per requested timestamp**,
the same length as the original point-sampled series. Value `i` is the maximum
over `[requestedTimes[i], requestedTimes[i+1])`; the final value is the single
sample at the last timestamp. When a bucket falls outside the state system range
or has no data, the **original point-sampled value is preserved**, so the result
is never worse than trivial sampling.

### `queryRangeMax`

Range-max queries reuse the existing, tested
`TmfStateSystemOperations.queryRangeMax`, which walks the mipmap levels stored
under a `max` feature attribute (level `1`, `2`, … holding the per-level max, and
the feature attribute itself holding the number of levels as its value). This is
the same query path used by the existing count-based mipmap feature, so it is
already covered by tests.

### Mipmap feature (write side)

The count-based `MaxMipmapFeature`/`TmfMipmapFeature` pair (feature aggregates
every N intervals into one entry at the next level) is the proven, tested write
implementation and is matched to `queryRangeMax`. Visual correctness of
max-per-bucket does **not** require power-of-10 time-aligned levels, so the
experimental `TimeMipmapFeature` is not required for correctness and is not on
the active write path.

## Phases

### Phase 0 (done): Safe, opt-in scaffolding

- `isMipmapEnhanced()` hook (default `false`) on `AbstractTreeDataProvider`.
- Gated enhancement in the two common XY base classes.
- `MipmapXYQueryHelper` returns one value per requested timestamp with
  edge/out-of-range fallback to the original value.

### Phase 1 (prerequisite): Non-polluting mipmap storage

Before any leaf-based provider can opt in, implement mipmap storage that keeps
the data quark a leaf (sibling subtree keyed by base quark), plus a
`queryRangeMax` overload that takes the base quark and the (separately located)
mipmap feature quark. Only quarks written through the mipmap helpers get levels.

### Phase 2 (proof of concept): a valid consumer

Migrate a provider whose displayed Y value **is** the raw instantaneous quark
value (not a delta or a rate) and have it override `isMipmapEnhanced()` to return
`true`. Candidates must be validated against the correctness constraints above.

> **Not candidates as-is:** `CounterStateProvider`/`CounterDataProvider`
> (differential by default) and `KernelCpuUsageStateProvider`/
> `CpuUsageDataProvider` (rate-based). These require either a separate raw series
> or a different aggregation (e.g. per-bucket sum/last for deltas) before they
> can benefit.

### Phase 3 (future): Min-Max bands and LTTB

Optional enhancements once max-only is proven on a valid consumer:

- Return both min and max for full envelope rendering.
- Apply LTTB for perceptually optimal point selection.

## File Changes Summary

| File | Change |
|------|--------|
| `tmf.core/.../tree/AbstractTreeDataProvider.java` | Add opt-in `isMipmapEnhanced()` hook (default `false`) + `getIdToQuark()` |
| `tmf.core/.../xy/AbstractTreeCommonXDataProvider.java` | Call enhancement only when opted in |
| `tmf.core/.../genericxy/AbstractTreeGenericXYCommonXDataProvider.java` | Call enhancement only when opted in (timestamp sampling) |
| `tmf.core/.../mipmap/MipmapXYQueryHelper.java` | One value per requested timestamp, edge fallback, guards |
| `tmf.core/.../provisional/.../AbstractXYStateProvider.java` | Provisional base state provider with mipmap write helpers (write side currently base-attribute only) |
| `tmf.core/.../mipmap/TimeMipmapFeature.java` | Experimental time-based feature; not on the active path |

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Enhancing a transformed series (delta/rate) | Wrong chart | Opt-in only; documented correctness constraints; default off |
| Mipmap attributes pollute leaf-based providers | Invisible lines, bogus tree entries | Store mipmaps in a sibling subtree, not under the data quark |
| Enhanced series length ≠ requested times | Misaligned/invisible lines | Helper now returns exactly one value per requested timestamp |
| Storage overhead | Extra history size | Only quarks written through mipmap helpers get levels |
| Forced state system rebuild on migration | One-time cost | Bump provider version; document in release notes |

## Success Criteria

1. No regression in existing XY views (all default to `isMipmapEnhanced() == false`).
2. CPU Usage view and Counters view render exactly as before (verified: neither
   opts in, and the CPU usage state provider creates no mipmap attributes).
3. When a *valid* provider opts in, spikes are preserved when zoomed out and the
   line converges to the raw value when zoomed in.
4. Enhanced series always have the same number of points as the requested
   timestamps.
