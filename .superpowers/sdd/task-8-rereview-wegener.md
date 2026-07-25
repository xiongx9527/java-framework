# Task 8 Re-review Feedback - Wegener

## Verdict

- Spec compliance: needs fixes.
- Task quality: needs fixes.
- Critical: none.
- Important: 1.
- Minor: none.

## Strengths

- `AskDataProperties` is now a replaceable conditional bean with the correct `fw.plus.ai.askdata` configuration prefix.
- User `AskDataSubAgent` beans are collected by the default dispatcher and injected into the default flow.
- Runtime code keeps the main SQL flow conservative: no sub-agents continue as an empty dispatch, and failed or throwing sub-agents do not interrupt the main flow.

## Important Finding

The new sub-agent wiring lacks default auto-configuration chain tests for failure/exception and no-sub-agent behavior. Existing tests only prove a successful custom sub-agent is called. Add `ApplicationContextRunner` coverage that:

- Registers a sub-agent returning failure and verifies default `AskDataService` still produces a successful SQL result.
- Registers a sub-agent throwing `RuntimeException` and verifies default `AskDataService` still produces a successful SQL result.
- Runs the same default service request with no `AskDataSubAgent` bean and verifies it succeeds.

## Controller Note

This is the re-review after one standard-mode review-fix round. Another automatic fix round requires explicit user continuation.
