# URA Workflow Engine

A reusable Quarkus backend that orchestrates how tasks and documents move
between staff across ranks and departments at Uganda Revenue Authority. It
does not replace departmental systems — it owns movement, authority
resolution, and traceability, and departmental systems plug into it over
REST (see the architecture doc / concept paper for the full picture).

**Stage 1 scope**: upward approval flows (prepare → review → approve...),
resolved against a seeded organisation model (ranks + `reports_to`
chains). Authentication, the Admin UI, HR sync, and downward/cross-department
flows are Stage 2/3 and out of scope here — see `CHANGES.md`.

## Running locally

Requires a PostgreSQL instance. Either point at your own:

```shell
export DB_URL=jdbc:postgresql://localhost:5432/ura_workflow_engine
export DB_USERNAME=workflow_user
export DB_PASSWORD=workflow123
./mvnw quarkus:dev
```

or omit the env vars and rely on the dev-mode defaults in
`application.properties` (same values as above, for local convenience only
— never rely on these defaults outside a developer's own machine).

Flyway runs the migrations in `src/main/resources/db/migration` on
startup, including seed data (ranks, departments, a sample Domestic Taxes
reporting chain, and the `internal-memo` workflow definition).

## Running tests

```shell
./mvnw test
```

Tests run against an ephemeral PostgreSQL container that Quarkus Dev
Services starts automatically (requires Docker/Podman locally or in CI —
no explicit datasource is configured under the `%test` profile, which is
what lets Dev Services take over).

## API

All endpoints are under `/api/v1` (see `quarkus.http.root-path`):

| Method | Path | Purpose |
|---|---|---|
| POST | `/v1/workflows/start` | Start a workflow instance |
| GET | `/v1/workflows/{id}` | Get instance status |
| POST | `/v1/workflows/{id}/approve` | Approve at the current step |
| POST | `/v1/workflows/{id}/return` | Return to an earlier step |
| POST | `/v1/workflows/{id}/cancel` | Cancel an in-progress instance |
| POST | `/v1/workflows/{id}/delegate` | Hand the current assignment to someone else |
| GET | `/v1/assignments/inbox/{personId}` | Pending work for a person |
| GET | `/v1/assignments/{id}` | Look up one assignment |
| GET | `/v1/history/workflow/{workflowInstanceId}` | Full audit history for an instance |

`actorId` in request bodies is trusted from the request for now (Stage 1
non-goal, see `CHANGES.md`) — before any real integration this needs to
come from an authenticated identity instead.
