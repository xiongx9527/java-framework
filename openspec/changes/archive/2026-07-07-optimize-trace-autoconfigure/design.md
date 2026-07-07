## Root Cause

The aspect lambda catches `Throwable` from `ProceedingJoinPoint.proceed()` and rethrows checked exceptions as `RuntimeException` because the surrounding `beforeExecute` method does not declare `throws Throwable`.

## Fix

Let `PlumelogAspect.beforeExecute` declare `throws Throwable`, call `plumelogTrace.startTraceLog` and `endTraceLog` directly, and always close the trace when start succeeds. This preserves the original return value and exception object while keeping the same trace lifecycle behavior.

## Notes

- Keep annotation pointcut and bean wiring unchanged.
- Keep trace cleanup behavior in `PlumelogFilterService` unchanged.
- Add tests that fail on the current wrapping behavior and pass after the fix.
