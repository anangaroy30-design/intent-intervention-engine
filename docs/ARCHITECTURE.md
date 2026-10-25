# Architecture

- Android: Jetpack Compose UI, simple screen state, local persistence, UsageStatsManager integration.
- Backend: Express REST API, SQLite repository layer, JWT bearer authentication.
- Intent engine: deterministic rules based on planned duration, goal completion, and user responses.
- Demo data is labeled simulated and is not represented as device-measured usage.

## Data labels
- **Measured:** Android UsageStatsManager data, when permission is granted.
- **User-reported:** purpose, goal completion, and choices.
- **Inferred:** heuristic drift score.
- **Simulated:** demo-clock advancement and seeded demo scenarios.
