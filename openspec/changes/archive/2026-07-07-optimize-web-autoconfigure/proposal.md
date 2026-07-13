## Why

The web auto-configuration package has a few maintainability and safety issues around slow request monitoring. Exception logging in `WebMvcAutoConfiguration` uses the exception message in the exception-type placeholder, and slow-request threshold processing assumes the threshold map is always present.

## What Changes

- Make slow-request threshold lookup null/empty safe.
- Correct exception detail logging so exception type and exception message are logged separately.
- Add focused tests for slow-request event handling.
- Clean up obvious comment noise and formatting in the touched web configuration classes.

## Capabilities

### New Capabilities
- `web-slow-request-monitoring`: Covers safe and accurate servlet slow-request monitoring behavior.

### Modified Capabilities

## Impact

- Affected code: `fw-plus-boot-autoconfigure` package `com.xiongx9527.fw.plus.boot.autoconfigure.web`.
- Affected tests: new focused tests for servlet slow-request monitoring.
- No public API, schema, or dependency version changes.
