# CEBO Bus Tracking Platform — PRD-Level Implementation Plan (Android Deployment Ready)

> **Companion guide:** For field constraints, runtime risks, and real-time readiness gates, see `REALTIME_READINESS_IMPLEMENTATION_PLAYBOOK.md`.

## 1) Objective and implementation target

This plan converts the current prototype tree into a production-ready Android bus-tracking platform with:
- end-to-end CRUD operations,
- robust offline-first behavior,
- measurable localization quality,
- deployment-ready Android packaging and release controls.

Primary outcomes:
1. Compile + run on supported Android devices.
2. Track buses with bounded, measurable error.
3. Manage fleet entities (bus/route/device/trip/driver) through CRUD workflows.
4. Sync safely to backend with retries and idempotency.
5. Provide driver UI and operations/admin flows.

---

## 2) Current project-tree issue review (what must be fixed first)

### 2.1 Build and packaging blockers
- Missing Gradle project files and Android app metadata (manifest/resources).
- No visible CI path for static checks, unit tests, instrumentation tests, or release signing.

### 2.2 API/contract mismatch blockers
- Service-layer constructors and method signatures are inconsistent across modules.
- GNSS contract appears split across incompatible snapshot shapes.
- Route storage entities exist but are not fully represented in DB config.

### 2.3 Feature-completeness blockers
- Sync upload path is placeholder.
- UI status screen is placeholder.
- Sensor fusion and fallback modes are partially wired.

### 2.4 Research-grade evidence blockers
- No evaluation protocol for reporting P50/P95 error, continuity, or alert precision.
- No experiment-replay framework for repeatable benchmarking.

---

## 3) Product Requirements Document (PRD) structure

## 3.1 Product scope

### In scope (v1)
- Driver app for on-bus tracking.
- Admin/fleet management API integration.
- Route-aware localization and ETA engine.
- Offline persistence + guaranteed eventual sync.
- Core analytics and health telemetry.

### Out of scope (v1)
- Passenger-facing public app.
- Full demand prediction / ML dispatch optimization.
- Multi-modal planner.

## 3.2 Users and personas
- **Driver**: must keep tracking ON with minimal interaction.
- **Fleet operator**: must monitor active trips and incidents.
- **System admin**: configures routes/devices and validates operations.

## 3.3 Non-functional requirements (NFRs)
- App cold start to tracking active: < 10 seconds.
- Tracking sample pipeline uptime: > 99% while service running.
- Sync delivery SLO: > 99.5% within 10 minutes of network recovery.
- Battery budget: configurable by profile (urban/intercity).
- Data integrity: no duplicate track points after server dedupe.

---

## 4) Android deployment-ready architecture target

## 4.1 Module responsibilities (clean + enforceable)

- `app`: DI wiring, startup, permissions, foreground service, navigation.
- `domain-core`: tracking engine, motion model, confidence model (pure Kotlin).
- `data-gnss`: GNSS adapters and quality snapshots.
- `data-sensors`: IMU stream and motion states.
- `data-route`: route load/cache/match/validate/eta.
- `data-local`: Room DB, DAOs, migrations, repositories.
- `data-sync`: API client, DTO mapping, sync workers.
- `feature-driver-ui`: status and diagnostics UI.
- `feature-admin` (optional mobile admin mode): CRUD screens for fleet config.

## 4.2 Android runtime strategy
- Foreground service + persistent notification.
- WorkManager for guaranteed background sync.
- Coroutine-based structured concurrency; no raw `Thread {}`.
- Hilt or Koin for dependency graph.
- Separate debug/staging/prod flavors.

## 4.3 Environment setup
- `debug`: mock API + verbose telemetry.
- `staging`: real API sandbox + synthetic routes.
- `prod`: hardened endpoints + strict logging policy.

---

## 5) CRUD design (entities, operations, and owner modules)

## 5.1 Core entities and CRUD matrix

1. **Bus**
   - Create: onboard/register bus profile.
   - Read: list and detail.
   - Update: metadata, plate, capacity, status.
   - Delete: soft-delete/archive.

2. **Device**
   - Create: bind Android device to bus.
   - Read: health, firmware/app version.
   - Update: assignment changes, calibration profile.
   - Delete: unbind/revoke token.

3. **Route**
   - Create: upload geometry/stops/schedule profile.
   - Read: route versions and active revision.
   - Update: publish new revision, rollback.
   - Delete: archive inactive route.

4. **Trip / Shift**
   - Create: start run with route + driver.
   - Read: active and historical trips.
   - Update: trip state transitions (paused/resumed/ended).
   - Delete: admin-only purge/invalid mark.

5. **Location sample**
   - Create: local ingestion and remote sync insert.
   - Read: latest + replay windows.
   - Update: sync status, server ack metadata.
   - Delete: TTL retention / compliance purge.

6. **Alert / Incident**
   - Create: deviation, outage, overspeed, stalled bus alerts.
   - Read: active/closed alerts.
   - Update: acknowledge/resolve/escalate.
   - Delete: policy-based archive only.

7. **Driver account**
   - Create: invite/provision.
   - Read: profile and assignment.
   - Update: role, language, contact, active state.
   - Delete: disable account.

## 5.2 API contract conventions
- REST + JSON for management CRUD.
- Batch ingest endpoint for telemetry (`POST /telemetry/batch`).
- Idempotency key header on writes.
- Cursor-based pagination for list operations.
- Soft-delete semantics with `isArchived` + `deletedAt`.

## 5.3 Local DB model requirements
- Room entities for: routes, trips, devices, buses, drivers, locations, alerts, outbox.
- Indexed columns: timestamps, sync flags, foreign keys, route/trip IDs.
- Migration policy: never destructive in production; full migration test per version.

---

## 6) Implementation plan by phases

## Phase A — Foundation hardening (Weeks 1–2)

Deliverables:
1. Add full Android project skeleton (Gradle, manifest, resources, flavors).
2. Resolve compile-time contract mismatches across service/core/gnss.
3. Unify tracking APIs into one versioned contract package.
4. Add baseline lint/detekt/ktlint + unit test task in CI.

Exit criteria:
- `./gradlew assembleDebug testDebugUnitTest` passes in CI.
- App installs and foreground service starts on device.

## Phase B — Data and CRUD backbone (Weeks 3–5)

Deliverables:
1. Finalize Room schema with migrations.
2. Implement repository layer for all CRUD entities.
3. Implement API clients and DTO mappers.
4. Build outbox pattern for offline writes and conflict-safe sync.

Exit criteria:
- CRUD flows validated for Bus/Device/Route/Trip/Alert in staging.
- Offline create/update/delete replay succeeds after reconnect.

## Phase C — Tracking fidelity and route intelligence (Weeks 6–8)

Deliverables:
1. Replace simplistic filter with local metric-frame EKF/UKF-lite (x/y + velocity).
2. Integrate IMU fallback and confidence degradation policy.
3. Route matching with heading + temporal continuity penalties.
4. ETA model calibration and confidence intervals.

Exit criteria:
- Controlled replay shows filtered P95 error better than raw GNSS baseline.
- False deviation alerts below agreed threshold.

## Phase D — Driver UX + operations readiness (Weeks 9–10)

Deliverables:
1. Production status screen (tracking, GNSS health, sync health, battery state).
2. Error-state UX for permissions/network/gps/sensor faults.
3. Optional admin screens for core CRUD edits.
4. Localization and accessibility pass.

Exit criteria:
- Driver can operate with ≤ 2 taps for standard shift workflow.
- All critical faults visible and actionable.

## Phase E — Deployment and release controls (Weeks 11–12)

Deliverables:
1. Secure auth/token refresh and certificate pinning where required.
2. Crash/performance/ANR monitoring setup.
3. Play signing/release pipeline with staged rollout.
4. Incident response runbook and operational dashboards.

Exit criteria:
- Staging soak test (7 days) passes SLO thresholds.
- Production launch checklist signed off.

---

## 7) Similar platforms and research implications to include

## 7.1 Similar platforms to benchmark

1. **OneBusAway**
   - Practical GTFS/GTFS-RT ecosystem integration.
   - Implication: adopt standards first to reduce custom protocol complexity.

2. **OpenTripPlanner + GTFS-Realtime stacks**
   - Strong route/schedule model alignment.
   - Implication: treat route and schedule as versioned artifacts with explicit revisions.

3. **Traccar-style telematics pipelines**
   - Mature device telemetry ingestion patterns.
   - Implication: adopt outbox + ack + idempotency and robust replay semantics.

4. **Commercial transit AVL systems (generic)**
   - Emphasis on operational dashboards and alert workflows.
   - Implication: build alert lifecycle (open/ack/resolve) early, not as afterthought.

## 7.2 Research-paper themes worth adopting

1. **Map-matching under noisy GNSS**
   - Use topology + heading + speed constraints, not nearest segment alone.
   - Example literature to review: Hidden Markov map matching methods for GPS traces (e.g., Newson & Krumm, 2009).

2. **Sensor fusion under GNSS outage**
   - Blend GNSS with inertial dead reckoning and confidence decay.
   - Example literature to review: INS/GNSS integration and urban canyon robustness studies in intelligent transportation research.

3. **Uncertainty-aware ETA**
   - Publish ETA interval, not just point estimate.
   - Example literature to review: probabilistic travel-time prediction models and confidence interval estimation for transit arrival.

4. **Energy-aware mobile sensing**
   - Adaptive sampling and duty cycling to preserve battery.
   - Example literature to review: mobile sensing energy optimization and adaptive duty-cycling strategies on Android.

5. **Data-quality governance in mobile sensing**
   - Track provenance/quality metadata per sample for auditability.
   - Example literature to review: data quality and trust frameworks for IoT/mobile telemetry pipelines.

## 7.3 Suggested paper-review worklist for your team (first pass)

- HMM-based map matching for noisy GPS trajectories (baseline algorithm study).
- Urban bus ETA prediction using historical + real-time telemetry (compare deterministic vs probabilistic ETA).
- Smartphone sensor fusion for vehicle localization (GNSS + IMU drift and correction methods).
- Fleet telematics architecture papers focusing on offline-first ingestion and delayed sync.
- Transit operations papers on alert precision/recall and operational decision support.

For your report/paper, extract from each paper:
- problem setting,
- input signals,
- algorithm class,
- evaluation metrics,
- deployment constraints,
- what can be reused in CEBO v1 vs v2.

## 7.4 Practical implications for CEBO
- Add `quality_features` payload (satUsed, snr, healthScore, sourceType).
- Add `state_mode` enum (GNSS_LOCKED, DEGRADED, DEAD_RECKONING, RECOVERING).
- Add confidence-aware business rules (alerts, ETA, route deviation thresholds).

---

## 8) Detailed engineering backlog (priority order)

## P0 (must before beta)
1. Build system + manifest + resources.
2. Fix all module contract mismatches.
3. Implement real sync transport and server ACK.
4. Replace placeholder UI with functional screen.
5. Full DB entity registration + migration tests.

## P1 (beta quality)
1. WorkManager-based resilient sync.
2. Structured logging and telemetry event schema.
3. Route matcher improvements (heading/continuity).
4. Driver shift lifecycle (start/pause/resume/end).
5. Alerts CRUD and status lifecycle.

## P2 (production optimization)
1. Advanced filter tuning by route profile.
2. Auto-calibration tools per device model.
3. Fleet analytics dashboards.
4. Security hardening and privacy retention controls.

---

## 9) Testing and validation strategy

## 9.1 Unit tests
- domain models (range checks, normalization).
- fusion math (predict/update invariants).
- route matching and validation rules.
- sync serializer/compressor/outbox ordering.

## 9.2 Integration tests
- Room migrations and repository workflows.
- API sync with mocked backend failures/retries.
- service lifecycle (start/stop/restart).

## 9.3 Field/replay tests
- replay recorded GNSS+IMU traces through engine versions.
- compare baseline vs candidate for P50/P95 and continuity.
- evaluate urban canyon/tunnel/open-sky scenarios.

## 9.4 Acceptance KPIs (ship gates)
- Tracking continuity > 99% during active trips.
- P95 horizontal error under target corridor.
- False off-route alert rate below threshold.
- Crash-free sessions > 99.5%.

---

## 10) Android deployment checklist

1. Manifest permissions finalized (location/background/activity/notifications).
2. Foreground service notification compliant with current Android policy.
3. Runtime permission UX for denied/permanently denied states.
4. Battery optimization handling + OEM-specific guidance.
5. Network security config and TLS policy.
6. Proguard/R8 rules for Room/serialization layers.
7. Signing config and Play Console staged rollout plan.
8. Privacy policy, data retention, and consent flow.

---

## 11) Governance model for “100% as accurate as possible”

Absolute 100% is not physically realistic for GNSS-based systems. Use a governance model:
- Define measurable quality bands (Excellent/Good/Degraded).
- Tie each band to explicit app behavior (sampling, ETA confidence, alerts).
- Report uncertainty transparently instead of claiming certainty.

Governance cadence:
- Weekly metric review for engineering.
- Monthly calibration review with field data.
- Versioned tuning configs per city/route/device family.

---

## 12) Actionable next 14-day sprint plan

### Sprint goals
- Make repo compile and deployable to at least one Android test device.
- Enable first complete CRUD workflow (Route + Trip + Alert + telemetry sync).

### Day-by-day execution
1. Day 1–2: bootstrap Gradle/manifest/resources and CI skeleton.
2. Day 3–4: unify contracts and refactor coordinator/service wiring.
3. Day 5–6: Room schema finalization + migrations + DAO tests.
4. Day 7–8: sync API client, outbox, retries, idempotency.
5. Day 9–10: driver status UI and fault states.
6. Day 11–12: route CRUD + trip state transitions.
7. Day 13: instrumentation and smoke tests.
8. Day 14: release candidate review and risk register update.

### Sprint DoD
- App installable from CI artifact.
- Tracking service runs 30+ minutes in field test.
- CRUD endpoints exercised via staging with audit logs.
- No critical blocker in release checklist.

---

## 13) Final recommendation

The fastest path to completion is:
1. **stabilize architecture contracts**,
2. **implement CRUD + sync backbone**,
3. **upgrade localization quality in measured steps**,
4. **ship with explicit uncertainty and operational guardrails**.

This delivers a practical PRD-level platform that is both deployment-ready and academically defensible.
