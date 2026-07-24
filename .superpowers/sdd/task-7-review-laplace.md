# Task 7 Review Feedback - Laplace

## Verdict

- Spec compliance: needs fixes.
- Task quality: needs fixes.
- Critical: none.
- Important: 3.
- Minor: 1 test coverage note.

## Important Findings

1. `DefaultAskDataFlowExecutor.isSafeCheckpoint` does not validate `completedSteps` order, continuity, and consistency with `currentStep`. A forged or corrupted checkpoint with `["validate-sql"]` and `sqlValidated=true` can bypass safety checks and execute SQL directly. Only legal step prefixes should be accepted, and `currentStep`, SQL, validation, and execution flags must be validated together.

2. The metadata checkpoint is saved after metadata collection, but only dialect is persisted. Resuming from a `[metadata]` checkpoint currently returns `RESUME_FAILED` instead of continuing from the next stable step. Persist the required structured metadata, or safely reload it during resume.

3. `InMemoryAskDataConversationStore` synchronizes map access, but saves and returns the same mutable `AskDataCheckpoint` instance. Callers can mutate saved state after `save`, and readers can observe later mutation. `save` and `load` should use defensive deep snapshots, including checkpoint state, completed steps, and step logs.

## Minor Finding

- Add focused tests for: `sqlExecuted=true` checkpoint recovery does not re-execute SQL; non-continuous `completedSteps` returns `RESUME_FAILED`; metadata checkpoint resumes; mutating the object after store save does not mutate stored state.

## Controller Notes

- The reviewer could not confirm whether the sub Agent merge path saves checkpoints from the diff alone. The controller should verify this before marking Task 7 complete.
