# Final Review Feedback - Hilbert

## Verdict

- Ready to merge: no.
- Critical: 2.
- Important: 4.
- Minor: 2.

## Critical Findings

1. SQL safety boundary does not guarantee true read-only access or metadata scope.
   - `DefaultAskDataSqlValidator` only checks first keyword and dangerous tokens.
   - `JdbcAskDataSqlExecutor` executes through ordinary `JdbcTemplate`.
   - Risk examples: `SELECT setval(...)`, `SELECT ... FOR UPDATE`, queries against tables outside `AskDataMetadata`.
   - Required fix: strengthen Java 8 compatible SQL validation for side-effect functions, lock clauses, statement type, metadata table scope where metadata is available, and physical row limits in executor or validation.

2. Resume protocol can execute checkpoint SQL with mismatched request fields and can repeat execution after crash between SQL execution and checkpoint save.
   - Resume currently hands checkpoint SQL to executor with current request.
   - SQL execution checkpoint is saved only after execution completes.
   - Required fix: checkpoint enough request identity fields and reject mismatches; introduce an executing/in-flight state or equivalent safe failure behavior before execution so uncertain state resumes as `RESUME_FAILED` instead of re-executing.

## Important Findings

1. Default flow does not implement the promised Agent replacement and sub-agent merge behavior.
   - Auto-configuration does not inject `AskDataAgent` replacements.
   - Dispatch result is discarded and exceptions are swallowed.
   - Required fix: map custom main agents into steps where feasible, consume `AskDataDispatchResult`, add step logs/output attributes, and save a checkpoint after merge.

2. `maxRows` is not a real maximum row count.
   - Existing `LIMIT` is accepted unchanged.
   - Risk examples: `LIMIT ALL`, `LIMIT 1000000`.
   - Required fix: tighten existing limits or enforce physical max rows, with regression tests.

3. Default model missing error code is folded into `MODEL_NOT_FOUND`.
   - `LangChain4jAskDataModelRegistry` can throw `DEFAULT_MODEL_NOT_CONFIGURED`.
   - `DefaultAskDataFlowExecutor` catches all runtime exceptions and returns `MODEL_NOT_FOUND`.
   - Required fix: preserve `AskDataModelException` error codes.

4. Optional request `execute` does not use global config as default.
   - Current logic requires global enabled and request `execute=true`.
   - Required fix: global disabled always skips; global enabled executes unless request explicitly sets `execute=false`.

## Minor Findings

1. Multiple `ContentRetriever` beans are not composed; `getIfAvailable()` may fail with multiple candidates. Prefer `orderedStream()`.
2. `sql.enforceLimit` is defined but unused. Implement or remove to avoid misleading users.

## Reviewer Recommendations

- Add regression tests for `LIMIT ALL`, over-large limit, `FOR UPDATE`, side-effect functions, and metadata table scope.
- Add resume mismatch and in-flight execution recovery tests.
- Add custom main Agent replacement and sub-agent merge/checkpoint tests.
