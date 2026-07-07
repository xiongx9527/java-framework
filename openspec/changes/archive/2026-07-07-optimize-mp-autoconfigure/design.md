## Overview

This tweak focuses on the `mp` package inside `fw-plus-boot-autoconfigure`. The root configuration should only validate the base MyBatis-Plus runtime and data-source prerequisites. Optional extensions such as MP-Join, data-change, field encryption, and data-scope should remain guarded by their own classpath, property, and bean conditions.

## Implementation Notes

- Remove the optional MP-Join condition class from the root `MpAutoConfiguration` `@ConditionalOnClass` list.
- Keep each child auto configuration independently controlled by its existing property prefix.
- Add or tighten classpath and bean checks where they make bean creation safer.
- Clean up obvious comment noise and dead commented code while preserving bean names and configuration prefixes.
- Use `ApplicationContextRunner` tests to verify configuration behavior without starting a full application.
