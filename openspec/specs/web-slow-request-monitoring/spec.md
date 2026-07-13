# web-slow-request-monitoring Specification

## Purpose
TBD - created by archiving change optimize-web-autoconfigure. Update Purpose after archive.
## Requirements
### Requirement: Slow request monitoring handles missing thresholds safely

Servlet slow-request monitoring MUST ignore threshold logging when the threshold-level map is null or empty instead of throwing an exception.

#### Scenario: Threshold map is missing

- **GIVEN** slow-request monitoring is enabled
- **AND** the threshold-level map is null
- **WHEN** a servlet request handled event is received
- **THEN** event handling completes without throwing an exception

### Requirement: Exception detail logging reports the exception type

Servlet slow-request monitoring MUST log the exception class name separately from the exception message when exception details are enabled.

#### Scenario: Failed request contains an exception

- **GIVEN** slow-request monitoring is enabled and exception detail logging is enabled
- **WHEN** a servlet request handled event contains a failure cause
- **THEN** the exception detail log includes the failure class name and message in their own fields

