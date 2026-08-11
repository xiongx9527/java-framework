# mybatis-datachange-dialect Specification

## Purpose
TBD - created by archiving change fix-oracle-datachange-diff. Update Purpose after archive.
## Requirements
### Requirement: Snapshot field lookup is case-insensitive

The data-change diff engine MUST match monitored column names with snapshot column labels without requiring identical letter casing.

#### Scenario: Oracle uppercase labels

- **GIVEN** a monitored column is configured as `amount`
- **AND** the JDBC snapshot exposes the label as `AMOUNT`
- **WHEN** the diff engine compares the snapshot
- **THEN** it MUST read the value and emit the corresponding row/field change

### Requirement: Cross-database numeric assertions

The integration contract MUST compare numeric field values by numeric value rather than Java wrapper type.

#### Scenario: Oracle NUMBER value

- **GIVEN** an Oracle snapshot returns `BigDecimal("20")`
- **WHEN** the contract checks the previous amount
- **THEN** the assertion MUST accept it as numeric value `20`

