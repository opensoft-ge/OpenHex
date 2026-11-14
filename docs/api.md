# OpenHex API

Base: `/api/v1`.
`GET|POST /projects` lists projects (after/before/limit) or creates `{title, description?}`.
`GET|PATCH /projects/{id}` reads or changes local `status` (admin).
`GET /cells?projectId={id}` supports `after`, `before`, `limit` (1–100; default 25); both cursor directions together fail.
`POST /cells` accepts `projectId`, `cellType`, `contents`, optional `label` and `location: {insertAfterCellId}`.
`contents` uses `codeCell: {source}`, `sqlCell: {source, outputDataframe?}`, or `markdownCell: {source}` according to type.
`GET|PATCH|DELETE /cells/{id}` reads, edits or removes a cell. All IDs are stable UUIDs. Source text is stored and returned; it is never executed. Tenant editors write, viewers read. Unsupported fields and malformed cursors fail.

Requests require `Authorization: Bearer <token>`. `/healthz` is public. Unknown/repeated query parameters and unknown body fields are rejected. Authentication uses local TOKENS configuration, not vendor OAuth. Transactions commit only successful requests.

Project lists accept `creatorEmail` and `ownerEmail`, combined with AND when both are supplied. Values match stored identity strings exactly (case-sensitive), are nonblank and at most 255 characters. Existing local principal IDs populate the email fields; this adds no email verification or ownership transfer. Filtering precedes newest-first pagination. A cursor outside the filtered result returns 400. Repeated parameters are rejected. Tenant access is unchanged.
