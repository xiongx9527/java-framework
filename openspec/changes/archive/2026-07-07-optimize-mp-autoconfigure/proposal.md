## Why

The `mp` auto-configuration package currently couples the root MyBatis-Plus auto configuration to optional MP-Join classes, uses broad conditions in a few extension configurations, and contains noisy comments that make the configuration boundaries harder to review. This change keeps optional extensions independent and makes the auto-configuration code easier to maintain.

## What Changes

- Relax `MpAutoConfiguration` classpath checks so it only requires the MyBatis-Plus base runtime dependencies globally.
- Keep optional MP-Join, data-change, encrypt, and data-scope checks inside their own auto-configuration classes.
- Add focused auto-configuration tests for root conditions and extension bean registration.
- Remove redundant comments, empty constructors, and dead commented code without changing public configuration prefixes or bean names.

## Capabilities

### New Capabilities
- `mp-autoconfigure-optimization`: Covers safe activation of MyBatis-Plus auto configuration based on dependencies, properties, and required beans.

### Modified Capabilities

## Impact

- Affected code: `fw-plus-boot-autoconfigure` package `com.xiongx9527.fw.plus.boot.autoconfigure.mp`.
- Affected tests: new or updated auto-configuration tests for the same package.
- No new public API, schema migration, or dependency version change.
