## Overview

This tweak is limited to the servlet web auto-configuration slow-request monitor. The fix should keep existing configuration keys and logging behavior, while making event processing safer and exception logs more accurate.

## Implementation Notes

- Guard threshold-level access with a null/empty check before streaming entries.
- Use `failureCause.getClass().getName()` for the exception-type placeholder and `failureCause.getMessage()` for the exception-message placeholder.
- Prefer small private helper methods where they improve readability.
- Add focused tests around `WebMvcAutoConfiguration.onApplicationEvent`.
