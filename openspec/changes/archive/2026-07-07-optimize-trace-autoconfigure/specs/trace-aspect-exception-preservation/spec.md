## ADDED Requirements

### Requirement: Trace aspect preserves method outcomes

The Plumelog trace aspect MUST preserve the original return value and thrown exception from the advised method while adding trace start/end logging around successful trace starts.

#### Scenario: Traced method returns normally

- **GIVEN** a traced method returns a value
- **WHEN** `PlumelogAspect` invokes the method
- **THEN** the same value is returned to the caller

#### Scenario: Traced method throws a checked exception

- **GIVEN** a traced method throws a checked exception
- **WHEN** `PlumelogAspect` invokes the method
- **THEN** the exact original checked exception is propagated to the caller
