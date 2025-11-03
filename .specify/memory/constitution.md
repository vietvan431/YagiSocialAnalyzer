# YagiSocialAnalyzer Constitution

## Core Principles

### I. Object-Oriented Design Discipline (NON-NEGOTIABLE)

MUST apply SOLID principles, encapsulation, and clear boundaries. Prefer
composition over inheritance. Public APIs are minimal and intention‑revealing.
State is localized; avoid global/static mutable state. Define interfaces for
collaborations; use dependency injection to decouple construction from usage.
Layering is explicit (e.g., UI → application/services → domain → infrastructure)
with no upward or lateral leaks.

Rationale: Strict OOP discipline maximizes readability, testability,
substitutability, and long‑term maintainability.

### II. Code Quality & Maintainability

MUST provide unit tests for business logic with minimum 80% line coverage in
core domain modules; critical algorithms require explicit property‑based and
boundary tests. Enforce static analysis, formatting, and style rules suitable
for Java (e.g., Checkstyle/SpotBugs/PMD or equivalents) in CI. Keep classes and
methods small and cohesive; cyclomatic complexity targets are set and enforced
in CI. All changes require at least one peer review with rationale documented.

Rationale: High code quality reduces defects and accelerates future change.

### III. User Experience & Accessibility

MUST deliver a responsive desktop UI that never blocks the UI thread for long‑
running work; operations use background workers with progress and cancellation.
Keyboard access, clear focus order, and readable contrast are required. Error
messages are actionable and non‑destructive; user preferences persist reliably.
Localization‑ready strings are used where practical.

Rationale: Superior UX increases adoption and reduces support burden.

### IV. Data Accuracy & Determinism

MUST validate inputs and explicitly define numeric precision, rounding, and time
handling (time zone, locale). Results are reproducible for the same inputs;
randomness is seeded in tests. Data transformations are versioned; any
user‑facing calculations document assumptions and error bounds.

Rationale: Accurate, deterministic behavior builds trust and enables debugging.

### V. Performance, Stability & Observability

MUST keep the UI responsive (<100 ms for interactive feedback where feasible).
Long operations expose progress and are cancellable. Structured logs and
diagnostics are available in development builds; crashes capture minimal safe
context for troubleshooting. Backward compatibility of user data formats is
maintained unless a major version is released with a migration path.

Rationale: A fast, stable app with good diagnostics minimizes downtime and
support costs.

## Additional Constraints & Technology Standards

- Project Type: Java desktop application; adhere to Java LTS (confirm exact
  version in planning). UI framework selection (e.g., JavaFX) MUST be documented
  per feature when affected.
- Build Tool: Maven or Gradle with reproducible builds and lockfiles.
- Packaging: Follow standard desktop distribution for the target OS; versioned
  artifacts with semantic versioning.
- Structure: Clear package structure by layer and/or feature modules; prohibit
  cyclic dependencies between modules.
- Security & Privacy: Least‑privilege file and network access; sensitive data is
  never logged. Third‑party licenses are tracked; only approved licenses
  allowed.
- Documentation: Public classes and critical algorithms include concise Javadoc
  explaining intent and constraints.

## Development Workflow, Reviews & Quality Gates

Pull requests MUST include links to the relevant spec/plan and a brief risk
assessment. The following gates are required in CI before merge:

1. Build & Static Checks: Project builds successfully; format and static
   analysis pass with no new warnings above baseline.
2. Tests: Unit tests pass; coverage ≥ 80% on core domain packages; property/
   boundary tests exist for critical logic.
3. UX Smoke: No UI thread blocking in manual smoke test or automated UI smoke;
   new user‑visible flows include screenshots or recordings.
4. Accuracy: For features affecting calculations or parsing, include examples
   and expected outputs; verify determinism under fixed seeds.
5. Release Notes: Update change log and migration notes when behavior changes.

Branch strategy, release cadence, and environment setup are documented in the
repository README or per‑feature quickstart.

## Governance

- Authority: This constitution governs design, development, and review.
- Amendments: Changes are proposed via PR that includes: change log entries,
  rationale, impact analysis, and migration plan (if applicable). Approval by
  code owners is required.
- Versioning: Semantic versioning (MAJOR.MINOR.PATCH) for this document.
  - MAJOR: Backward‑incompatible governance changes or removals.
  - MINOR: New sections/principles or substantial expansions.
  - PATCH: Clarifications and non‑semantic edits.
- Compliance: PR templates MUST reference this constitution; reviewers verify
  gates and principles. Non‑compliant changes are rejected or require waivers
  with explicit time‑boxed remediation tasks.

**Version**: 1.0.0 | **Ratified**: 2025-10-31 | **Last Amended**: 2025-10-31
