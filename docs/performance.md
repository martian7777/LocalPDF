# Performance & Efficiency

## Targets

Fill where relevant:

```text
Cold startup: [TARGET]
Warm startup: [TARGET]
Critical screen render: [TARGET]
Scroll/jank target: [TARGET]
Memory budget: [TARGET]
App size target: [TARGET]
```

## Compose

Watch for:

- unnecessary recomposition
- unstable parameters
- expensive work during composition
- repeated object creation
- excessive state reads
- large non-lazy lists

Use appropriate:

- `remember`
- `derivedStateOf`
- `LazyColumn` / `LazyGrid`
- stable data models where justified
- pagination

Do not cargo-cult optimization.

## Startup

Keep Application initialization minimal.

Defer non-critical work.

Use App Startup only when justified.

## Baseline Profiles

Use Baseline Profiles and Macrobenchmark for important production flows when valuable.

## Database

Measure:

- query latency
- large scans
- N+1 style access patterns
- migration cost

## Network

Reduce:

- unnecessary calls
- large payloads
- repeated downloads

Use caching only when product behavior supports it.

## Images

Use appropriate image size, caching, and decoding.

Do not load full-resolution images for thumbnails.

## Battery

Be conservative with:

- background work
- location
- Bluetooth scanning
- wakeups
- periodic sync
- sensors

Prefer event-driven behavior when possible.
