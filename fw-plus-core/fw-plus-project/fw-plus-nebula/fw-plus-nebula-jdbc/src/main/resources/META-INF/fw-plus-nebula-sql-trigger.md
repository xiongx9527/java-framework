# Nebula MySQL Trigger Subset

This module exposes a gateway-side trigger subset for SQL write plans.

## Scope

- Applies only to writes that enter the MySQL gateway path and are converted to `NebulaMutationPlan`.
- Does not observe native nGQL execution through `NebulaConnection.execute(...)`.
- Does not change the existing JDBC direct nGQL statement and prepared statement behavior.
- Does not claim full MySQL Server trigger compatibility.

## Supported DDL

Supported CREATE form:

```sql
CREATE TRIGGER trigger_name
BEFORE INSERT ON table_name
FOR EACH ROW
INSERT INTO audit_table(id) VALUES ('audit-id')
```

Supported timing values are `BEFORE` and `AFTER`.
Supported events are `INSERT`, `UPDATE` and `DELETE`.
`FOR EACH ROW` is required, but the implementation remains limited to the gateway write plan boundary.

Supported DROP forms:

```sql
DROP TRIGGER trigger_name
DROP TRIGGER IF EXISTS trigger_name
```

`DROP TRIGGER IF EXISTS` is idempotent. `DROP TRIGGER` without `IF EXISTS` returns a metadata error when the trigger is missing.

## Unsupported DDL And Body Syntax

The first subset rejects:

- `BEGIN ... END` compound trigger bodies
- `CALL`
- transaction statements
- dynamic SQL through `PREPARE` or `EXECUTE`
- nested `CREATE TRIGGER` or `DROP TRIGGER`
- multiple body statements separated by semicolon
- `FOLLOWS` and `PRECEDES`
- cross-schema target tables

Body SQL is expected to be a single supported write statement handled by the gateway write execution port.

## Execution Order

For a matching write:

1. Enabled BEFORE triggers run in metadata order.
2. The primary write runs only if all BEFORE triggers succeed.
3. Enabled AFTER triggers run only if the primary write succeeds.

Metadata order is stable by creation order, with trigger name as a deterministic tie breaker.

## Failure Policy

- BEFORE trigger failure aborts the primary write and skips remaining triggers.
- Primary write failure skips AFTER triggers.
- AFTER trigger failure is returned to the client, but this subset does not promise rollback of an already successful primary write or earlier AFTER trigger body.

## Recursion

Trigger execution carries a `TriggerExecutionContext` through nested body writes.
The default production limit is `TriggerExecutionContext.DEFAULT_MAX_DEPTH` (8 levels).
It can be overridden with the `nebula.jdbc.sql.trigger.maxDepth` system property.
Exceeding the configured limit returns a `HY000` error with a trigger recursion depth message.
