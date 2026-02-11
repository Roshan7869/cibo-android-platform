# CEBO Bus Platform — Real-Time Readiness & Practical Implementation Playbook

## Purpose

This playbook translates the current CEBO architecture into practical, field-ready execution for real-time bus tracking.

It focuses on:
- real-world operational challenges,
- current readiness gaps,
- practical mitigations,
- implementation tasks you can start immediately.

---

## 1) Real-time challenge landscape (what similar platforms face)

Real-time transit/AVL platforms commonly face these challenge classes:

1. **Positioning instability**
   - Urban canyon multipath.
   - Tunnel/underpass GNSS outages.
   - Device-specific sensor quality variance.

2. **Connectivity instability**
   - Dead zones and intermittent network.
   - Carrier handoff interruptions.
   - Burst reconnect causing duplicate uploads.

3. **Power and thermal pressure**
   - Continuous GPS + sensor use drains battery.
   - Thermal throttling in hot environments (dashboard mounting).

4. **Background execution constraints (Android)**
   - Foreground service policy constraints.
   - OEM power managers killing background tasks.
   - Permission revocation edge cases.

5. **Data quality and trust issues**
   - Noisy speed/heading.
   - Delayed timestamps and out-of-order records.
   - False route deviation alerts due to poor map matching.

6. **Operations and maintainability**
   - Hard to diagnose field issues without observability.
   - App/server contract drift over releases.
   - Migration failures in offline databases.

---

## 2) CEBO real-time readiness scorecard (current vs required)

Scoring model:
- 0 = missing
- 1 = partial/prototype
- 2 = usable with risk
- 3 = production-ready

### A) Localization and tracking fidelity — **1/3**
Current state:
- Fusion/tracking modules exist but integration contracts are inconsistent.
- IMU fallback and degraded-mode behavior are not fully operational.

Required for production:
- Unified sensor/GNSS contracts.
- Explicit mode machine: `LOCKED`, `DEGRADED`, `DEAD_RECKONING`, `RECOVERING`.
- Replay-driven tuning against route-specific traces.

### B) Offline sync reliability — **1/3**
Current state:
- Offline storage and batching concepts exist.
- Upload path remains placeholder behavior.

Required for production:
- Outbox with retry/backoff/jitter.
- Idempotent server write protocol.
- Sync checkpointing and duplicate suppression.

### C) Android deployment readiness — **0/3**
Current state:
- Missing full build/deployment scaffold and release controls.

Required for production:
- Complete Gradle + manifest + flavors + signing pipeline.
- WorkManager policy for resilient background tasks.
- Device policy matrix and OEM hardening.

### D) CRUD and fleet operations — **1/3**
Current state:
- Route/location data structures exist.
- Full lifecycle CRUD workflows (bus/device/driver/trip/alerts) not implemented end-to-end.

Required for production:
- API + Room + UI flows for each core entity.
- Role-aware access and auditability.

### E) Observability and incident response — **0/3**
Current state:
- No complete observability contract visible.

Required for production:
- Structured logs, metrics, traces.
- Alert pipelines and runbooks.
- Failure classification and escalation workflows.

**Overall readiness: ~20–30% (prototype, not operational production yet).**

---

## 3) Practical risk matrix and mitigations

## 3.1 Tracking risks

### Risk: false off-route alerts during GNSS degradation
Mitigation:
- Use confidence-aware deviation thresholds.
- Add heading continuity + segment progression checks.
- Suppress hard alerts during sustained degraded mode unless persistent.

### Risk: jumpy position updates at low speed/stops
Mitigation:
- Stationary filter mode with stricter smoothing.
- Speed floor and hysteresis for movement state transitions.

### Risk: tunnel blackout
Mitigation:
- Dead-reckoning mode with bounded time window.
- Confidence decay curve + UI state change.
- Rapid re-lock strategy when GNSS quality returns.

## 3.2 Sync and data integrity risks

### Risk: duplicate points after reconnect
Mitigation:
- Generate deterministic sample IDs (tripId + deviceTime + sequence).
- Server-side idempotent upsert.

### Risk: large backlog causing delayed freshness
Mitigation:
- Priority lanes: latest points first for live map freshness.
- Background catch-up for historical backlog.

### Risk: schema migration breaking offline data
Mitigation:
- Migration test suite for every DB version.
- Roll-forward-only migration policy and backup before destructive operations.

## 3.3 Android runtime risks

### Risk: service killed by OEM battery manager
Mitigation:
- Foreground service compliance + startup recovery broadcast.
- In-app OEM-specific battery optimization guidance.
- Watchdog heartbeat with controlled restart policy.

### Risk: permission denial or revocation
Mitigation:
- Permission-state machine with clear UX and recovery path.
- Degraded operation mode if background permission unavailable.

---

## 4) Practical architecture upgrades required now

1. **Contract stabilization layer**
   - Create a versioned contract package for GNSS/sensor/tracking DTOs.
   - Prevent cross-module drift by enforcing shared contract tests.

2. **Real sync implementation**
   - Replace stub transport with authenticated API client.
   - Add retry policy (exponential backoff + jitter).
   - Add outbox status fields: `PENDING`, `SENDING`, `ACKED`, `FAILED`.

3. **Operational state machine**
   - Implement explicit tracking state machine:
     - `IDLE`
     - `TRACKING_LOCKED`
     - `TRACKING_DEGRADED`
     - `TRACKING_DR`
     - `SYNC_BACKLOG`
     - `ERROR_BLOCKING`

4. **Observability backbone**
   - Define event schema (`tracking_mode_changed`, `sync_retry`, `route_deviation_opened`, etc.).
   - Add metric counters and latency histograms.

5. **Release safety controls**
   - Feature flags for route matching thresholds and filter parameters.
   - Remote config for emergency tuning in production.

---

## 5) Minimum practical implementation backlog (next 6 weeks)

## Week 1–2: runtime foundation
- Build/manifest/flavors/signing setup.
- Dependency injection and coroutine scope governance.
- Foreground service lifecycle hardening.
- CI pipeline with lint + unit tests.

## Week 2–3: data and sync core
- Implement outbox table and repository.
- Implement telemetry ingest API client.
- Add idempotency keys and ACK handling.
- Add retry/backoff and dead-letter handling.

## Week 3–4: tracking stabilization
- Unify GNSS/sensor contracts.
- Implement mode machine and confidence-aware behavior.
- Integrate route matching guardrails.

## Week 4–5: CRUD operational scope
- End-to-end CRUD for Route, Trip, Alert, Device assignment.
- Validation rules and optimistic UI updates.

## Week 5–6: field readiness and validation
- Replay tests (urban/tunnel/open sky).
- Battery stress tests and long-run service tests.
- Staging soak test and release checklist closure.

---

## 6) Practical KPIs for “real-time ready” definition

Your platform is “real-time ready” only when these are true in staging/field tests:

1. **Freshness KPI**
   - P95 end-to-end latency (capture → server ingest) under target.

2. **Continuity KPI**
   - Tracking continuity above threshold during active trips.

3. **Accuracy KPI**
   - P95 route-projected error under corridor target.

4. **Alert quality KPI**
   - False positive off-route alert rate below target.

5. **Reliability KPI**
   - Eventual delivery success above threshold after network recovery.

6. **Runtime KPI**
   - Crash-free + ANR-free sessions above release threshold.

---

## 7) Field testing protocol (practical)

### Scenario suite
1. Open sky highway route.
2. Dense urban canyons.
3. Tunnel and underpass sections.
4. Stop-and-go city traffic.
5. Poor network corridor.

### Instrumentation to capture
- raw GNSS samples,
- fused positions,
- sensor mode transitions,
- sync queue depth,
- upload retry logs,
- battery/thermal telemetry.

### Decision gates
- If alert false positives exceed threshold, tune route-matching constraints first.
- If continuity drops in tunnels, tune DR fallback and re-lock criteria.
- If freshness latency rises during reconnect bursts, apply queue prioritization.

---

## 8) Is CEBO ready today for real-time challenges?

**Short answer: not yet.**

Current status indicates CEBO is a strong architecture prototype but not yet deployment-ready for real-time operations because:
- build/runtime release scaffolding is incomplete,
- sync transport is not fully implemented,
- integration contracts are inconsistent,
- observability and operational safeguards are not fully established.

**Good news:** with the backlog in this playbook (especially foundation + sync + state machine + field testing), CEBO can reach practical staging readiness quickly.

---

## 9) What to implement first (priority order)

1. Compile/deploy pipeline and Android runtime hardening.
2. Real sync transport + outbox reliability.
3. Contract unification across service/core/gnss/sensors.
4. Tracking mode machine with confidence-aware behavior.
5. Route/trip/alert CRUD end-to-end and field validation.

This order minimizes project risk and gets you to practical, testable real-time performance fastest.
