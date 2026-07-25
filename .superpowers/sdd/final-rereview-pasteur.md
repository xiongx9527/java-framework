# Final Re-review Feedback - Pasteur

## Verdict

- Ready to merge: no.
- Critical remaining: 2.
- Important remaining: none from Hilbert's original list.

## Strengths

- Lock syntax, multi-statements, `SELECT INTO`, known side-effect functions, `LIMIT ALL`, and over-large `LIMIT` are covered.
- JDBC execution has both `setMaxRows` and read-loop row limiting.
- Executing checkpoint state prevents uncertain re-execution.
- Model error code preservation, execute default semantics, main/sub-agent logging and checkpoint merge, multiple `ContentRetriever` composition, and `sql.enforceLimit` are implemented.

## Critical Findings

1. SQL read-only and metadata scope can still be bypassed.
   - Metadata containing only `orders` allows `other_schema.orders`.
   - `pg_notify(...)` and `lo_unlink(...)` remain valid SELECT calls.
   - Required fix: schema-qualified names must match exactly; simple table metadata must reject explicit foreign schema qualification. Add `pg_notify` and `lo_unlink` to side-effect function rejection, plus regression tests. Also record that DB read-only account/transaction remains a deployment defense.

2. Resume identity does not include `options`.
   - `AskDataRequest.options` can carry tenant/security parameters.
   - Checkpoint/request identity comparison currently ignores it.
   - Required fix: snapshot/copy/normalize request options into checkpoint and reject resume when options differ; fail closed if options cannot be compared safely. Add tenant/options drift regression test.
