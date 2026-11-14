# OpenHex compatibility ledger

Baseline: empty directory, initialized as an independent repository. Guide: `../skills/open-hex/SKILL.md` and its bundled inventory. This iteration establishes an API foundation, not complete product parity.

Target: **Hex API v1, OpenAPI info 1.0.0**, primary documentation accessed 2026-11-13: [reference](https://static.hex.site/openapi.json). Tests exercise our implementation; no licensed vendor conformance capture was available.

| ID | Capability | Implemented behavior | Boundary / next work |
|---|---|---|---|
| OH-001 | Projects | Create/list/detail; creator/owner filtering; admin status changes | No team sharing, templates, archive/delete or published apps |
| OH-002 | Notebook authoring | Create/read/update/delete CODE, SQL and MARKDOWN cells | Classic source only; no data connections or compute |
| OH-003 | Cell order | Insert after cell and cursor pagination | Local UUID cursors, no collaborative editing |
| OH-004 | Execution | Missing | No fake run endpoint or fabricated results |
| OH-005 | Remaining product | Missing | YAML, orchestration, apps, charts, AI, warehouse connectivity and UI |

Cross-cutting foundation: persistent SQLite transactions, local bearer identity, tenant isolation, role checks, strict input validation, Docker deployment and exact coverage gates. Missing across the product: vendor OAuth/SSO, fine-grained ACLs, production migrations/backup tooling, distributed workers and vendor-identical UI. Coverage measures implemented Java, not the missing inventory.

Acceptance: valid lifecycle persists through app restart; invalid inputs fail without partial writes; tenant boundaries and roles hold; exact executable-line coverage includes every production Java class; branch counts are reported separately. Docker smoke exercises a real HTTP server. See [validation](validation.md).

## OH-001 continuation — project identity filters, 13 November 2026

Starting HEAD `5f92da5`, branch main, clean tree. Selected the missing owner/creator filters (inventory A6, ListProjects) over execution (OH-004, missing, requires isolated workers) and YAML interchange (OH-005, missing, requires schema evidence). Using the open-hex skill. Target: classic Hex API v1, OpenAPI info 1.0.0, [official specification](https://static.hex.site/openapi.json) retrieved 2026-11-13; GET /v1/projects declares creatorEmail and ownerEmail string filters. Local exact-match semantics and identity values are explicitly documented in `api.md`; no email verification, full sharing or live vendor conformance is claimed.

Implemented in `ApiController.java` with five new acceptance cases in `src/test/java/ge/opensoft/openhex/ContractTest.java`. Tests cover each filter, conjunction, empty matches, tenant isolation, viewer reads, restart persistence, forwards/backwards pagination after filtering, invalid cursors and malformed/repeated parameters. The positive test failed on the baseline. Current Java measurements are in [validation](validation.md).

## Java 25 migration

The runtime, HTTP acceptance harness and build now use Java 25 / Spring Boot 4.1.2. Existing persistence and implemented API behavior are retained. Current coverage and test results supersede earlier stack-specific measurements: see [validation](validation.md). This migration does not expand vendor-compatibility claims.
