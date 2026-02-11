# CEBO Bus Tracking Platform — Current Condition Review

> **Update:** For an execution-focused PRD and Android deployment roadmap with CRUD scope, see `PRD_LEVEL_IMPLEMENTATION_PLAN.md`.

## Executive assessment

Your repository currently looks like a **research prototype / architectural draft**, not yet a buildable Android application.

What is strong already:
- The domain decomposition is excellent (`core`, `gnss`, `sensors`, `route`, `storage`, `sync`, `service`, `ui`).
- The code tries to keep pure logic in non-Android modules (good for testability and paper rigor).
- You already include key fleet concepts: GNSS quality scoring, route snapping, offline storage, sync batching, and confidence modeling.

What blocks deployment:
- Build system and Android app metadata are missing (no Gradle files, no manifest, no resources).
- Multiple cross-module API mismatches make the current source set non-compilable.
- Several modules are placeholders/stubs (UI and upload pipeline in particular).

Bottom line:
- **Concept maturity: high**
- **Implementation completeness: medium-low**
- **Deployment readiness: low (blocked by compile/integration gaps)**

---

## Module-by-module condition ("Genesis modules")

### 1) Core engine (`core/*`) — **good conceptual base, moderate math risk**

Strengths:
- Strong typed value objects (`Accuracy`, `Speed`, `Heading`, `Confidence`, `GeoPoint`).
- Clean `TrackingEngine -> FusionEngine -> KalmanFilter` flow.

Risk areas:
- `FusionEngine` computes a motion prediction but does not feed that predicted position into filter state before correction, so prediction influence is weaker than intended.
- The Kalman model uses lat/lon directly as linear state and a shared scalar variance model; this is simple but can become unstable in high-latitude/variable-speed scenarios.

Needed improvements:
- Use ENU/local tangent plane for filtering state (`x/y` meters) then convert to WGS84.
- Expand to a constant-velocity state vector and tune process noise by motion mode.

### 2) GNSS module (`gnss/*`) — **important logic exists, but interface versioning is inconsistent**

Strengths:
- `GnssQualityMonitor` has a real scoring model with rolling windows and freshness penalty.
- `GnssManager` supports adaptive location intervals.

Critical issue:
- `GnssStatusSnapshot` fields used by `GnssQualityMonitor` are not the same as fields produced in `GnssCallback`.
- This indicates two versions of GNSS contracts coexist and will break compilation/integration.

Needed improvements:
- Consolidate to a single GNSS event contract and enforce via integration tests.
- Add explicit event timestamps and source quality tags (fused/network/gps/raw).

### 3) Sensor module (`sensors/*`) — **efficient runtime shape, low fusion integration**

Strengths:
- Adaptive sampling and movement detection are implemented in `SensorManagerWrapper`.

Risk area:
- IMU state is not yet integrated into tracking updates (callback placeholder comments indicate future work).

Needed improvements:
- Integrate zero-velocity update (ZUPT), heading stabilization, and dead-reckoning fallback logic into tracking pipeline.

### 4) Route module (`route/*`) — **good deterministic geometry, needs map quality controls**

Strengths:
- Route loading, snapping, validation, and ETA are all represented.

Risk areas:
- Map matching is nearest-segment only; lacks heading/speed/topology constraints.
- Deviation threshold is static.

Needed improvements:
- Add heading-consistency and temporal continuity penalties.
- Use confidence-weighted thresholds (wider during poor GNSS, tighter during strong GNSS).

### 5) Storage module (`storage/*`) — **solid offline-first skeleton, schema mismatch risk**

Strengths:
- Practical offline model with synced flag + batching indexes.

Critical issue:
- `RouteEntity`/`RouteDao` exist, but database declaration currently registers only `LocationEntity`.

Needed improvements:
- Add all entities/DAOs to `CeboDatabase` and define migrations from day 1.

### 6) Sync module (`sync/*`) — **pipeline shape is ready, transport is stubbed**

Strengths:
- Batching, compression, and re-entrancy guard are present.

Critical issue:
- Upload always returns success (`TODO` placeholder), so reliability claims are not yet measurable.

Needed improvements:
- Implement authenticated transport, retries with exponential backoff, idempotency keys, and server ACK contract.

### 7) Service/orchestration (`service/*`) — **design intention is strong, constructor contracts are broken**

Critical issues:
- `TrackingForegroundService` initializes `TrackingEngine()` without required constructor arguments.
- `TrackingCoordinator` constructor and usage are inconsistent; method calls use signatures not available in current core classes.

Needed improvements:
- Freeze service-layer interfaces and align coordinator/core contracts before any new features.
- Replace raw `Thread {}` persistence with structured coroutines/supervision.

### 8) UI module (`ui/*`) — **prototype-only state**

Critical issues:
- `StatusFragment` returns `View(context)` placeholder and comments out real rendering.
- Main activity uses no layout resource.

Needed improvements:
- Implement real status layout and bind to live `UiState` stream from service/viewmodel.
- Add driver-safe status semantics and localized text.

---

## Why your platform cannot currently be "100% accurate"

For bus tracking, 100% physical accuracy is impossible due to GNSS physics (multipath, canopy/tunnel loss, clock bias, atmospheric delay). The practical goal should be:
- **Bounded error** (e.g., P50, P95 horizontal error)
- **Deterministic failover behavior** (degraded mode in GNSS outage)
- **Operational consistency** (predictable confidence outputs)

Your paper can become stronger by reframing claims to:
- "high-integrity tracking with quantified uncertainty"
- "route-constrained localization with confidence bounds"
- "offline-resilient telemetry with eventual consistency"

---

## Top root causes behind current gaps

1. **Version drift between modules**
   - Multiple classes imply different API generations at once.
2. **Architecture ahead of integration**
   - Good design docs, but runtime wiring lags.
3. **No build/test harness in repo**
   - Missing Gradle + manifest prevents immediate compile verification.
4. **Placeholder production paths**
   - UI and sync still contain temporary implementations.

---

## Priority improvement roadmap

### Phase 0 (immediate, must do)
1. Add Android project essentials (`settings.gradle`, module `build.gradle`, manifest, resources).
2. Fix all compile-time interface mismatches across `service`, `core`, and `gnss`.
3. Replace placeholder upload return path with real API contract.
4. Implement a minimal functional status screen.

### Phase 1 (accuracy hardening)
1. Move filter math to local metric frame (ENU).
2. Add dead-reckoning branch using IMU when GNSS quality drops.
3. Introduce route-aware confidence adjustment with explicit equations.
4. Log per-sample quality features for post-hoc calibration.

### Phase 2 (validation + paper quality)
1. Define evaluation protocol:
   - Urban canyon, open sky, tunnel, stop-go traffic.
2. Report metrics:
   - P50/P95 error, continuity loss rate, false deviation rate, ETA MAE.
3. Add reproducible calibration artifacts:
   - Config profile by city/route/device class.

---

## Suggested "accuracy governance" model for Genesis modules

For each module, enforce:
- **Owner**: single responsible maintainer.
- **Contract**: stable DTO/interface file.
- **SLO/KPI**: measurable acceptance target.
- **Tests**: unit + integration + replay tests.

Example KPI set:
- Core fusion: P95 filtered error improvement vs raw GNSS ≥ X%.
- GNSS quality: outage detection latency < 2 s.
- Route matcher: false off-route alert rate < 1% on labeled runs.
- Sync: >99.5% eventual delivery within 10 min connectivity window.

---

## Final recommendation

Your project has a strong architecture for a publishable bus-tracking platform, but currently behaves more like a **technical blueprint** than a production artifact.

If you first close integration/compilation gaps, then quantify uncertainty rigorously instead of promising absolute accuracy, your platform and paper will become significantly stronger and more defensible.
