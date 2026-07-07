## Why

`PlumelogAspect` currently wraps checked exceptions thrown by traced methods into `RuntimeException`. This changes the caller-visible exception type and can break existing exception handling contracts for methods annotated with `@PlumelogTrace`.

## What Changes

- Preserve the original exception thrown by `ProceedingJoinPoint.proceed()` in `PlumelogAspect`.
- Add focused tests for traced method return values and exception propagation.
- Clean up trace auto-configuration classes where safe, without changing public configuration prefixes or adding new APIs.

## Capabilities

### New Capabilities

### Modified Capabilities

## Impact

- Affected code: `fw-plus-boot-autoconfigure` trace package, especially `PlumelogAspect`.
- Affected tests: new trace aspect tests.
- No public API, schema, or dependency version changes.
